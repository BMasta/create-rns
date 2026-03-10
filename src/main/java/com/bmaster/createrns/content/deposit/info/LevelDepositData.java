package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.content.deposit.mining.recipe.DepositDurability;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipeLookup;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerLocateContext;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerLocateContext.DepositCandidateFilter;
import com.bmaster.createrns.data.gen.depositworldgen.DepositSetConfigBuilder;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.util.Utils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LevelDepositData implements INBTSerializable<CompoundTag> {
    public static final int MIN_COMPUTE_INTERVAL = 90;

    private final ServerLevel level;

    // Deposits added via a console command and not tied to a structure
    private final Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<BlockPos>> customDeposits =
            new Object2ObjectOpenHashMap<>();

    // Generated found deposits are represented as bounding box centers of deposit structures (not bound to res. loc.)
    private final Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<ChunkPos>> foundDeposits =
            new Object2ObjectOpenHashMap<>();

    private final Cache<UUID, CachedData> perPlayerCache = CacheBuilder.newBuilder()
            .initialCapacity(1)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    private final Object2LongOpenHashMap<BlockPos> depositDurabilities = new Object2LongOpenHashMap<>();

    public LevelDepositData(ServerLevel level) {
        this.level = level;
    }

    public @Nullable ResourceKey<Structure> getType(BlockPos pos) {
        for (var e : foundDeposits.object2ObjectEntrySet()) {
            for (var chunk : e.getValue()) {
                if (Utils.isPosInChunk(pos, chunk)) {
                    return ResourceKey.create(Registries.STRUCTURE, e.getKey());
                }
            }
        }
        return null;
    }

    public DepPosInfo getNearest(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks) {
        return getNearest(depositKey, sp, sp.serverLevel(), sp.blockPosition(), searchRadiusChunks, false);
    }

    public DepPosInfo getNearest(ResourceKey<Structure> depositKey, ServerLevel level, BlockPos pos,
                                 int searchRadiusChunks, boolean allow_discovered) {
        return getNearest(depositKey, null, level, pos, searchRadiusChunks, allow_discovered);
    }

    public DepPosInfo getNearestCached(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks) {
        var uuid = sp.getUUID();
        var hit = perPlayerCache.getIfPresent(uuid);

        if (hit == null) {
            CreateRNS.LOGGER.trace("[Cache miss] Deposit position is not cached for player");
            return new DepPosInfo(null, null, null);
        } else if (hit.i.bestEffortPos == null) {
            perPlayerCache.invalidate(uuid);
            CreateRNS.LOGGER.trace("[Cache hit] Did not find any deposits nearby");
            return new DepPosInfo(null, null, null);
        } else if (!hit.depKey().equals(depositKey)) {
            perPlayerCache.invalidate(uuid);
            CreateRNS.LOGGER.trace("[Cache hit] Cached deposit type does not match the requested type");
            return new DepPosInfo(null, null, null);
        }

        // Try to resolve accurate position of deposit
        if (hit.i.accuratePos == null && hit.i.startChunk != null) {
            var accuratePos = getDepositCenter(hit.i.startChunk);
            // If successful, update the cache
            if (accuratePos != null) {
                var newInfo = new DepPosInfo(hit.i.startChunk, accuratePos, accuratePos);
                perPlayerCache.put(uuid, new CachedData(hit.depKey, newInfo, hit.creationTimestamp));
            }
        }

        // Log result
        var qualifier = (hit.i.startChunk == null) ? "custom" : ((hit.i.accuratePos == null) ? "ungenerated" : "generated");
        var yCoord = (hit.i.accuratePos != null) ? hit.i.accuratePos.getY() : "~";
        CreateRNS.LOGGER.trace("[Cache hit] Found {} deposit at {},{},{}", qualifier,
                hit.i.bestEffortPos.getX(), yCoord, hit.i.bestEffortPos.getZ());

        return hit.i;
    }

    public void addCustomDeposit(ResourceKey<Structure> depositKey, BlockPos customPos) {
        customDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>()).add(customPos);
    }

    public boolean removeCustomDeposit(BlockPos customPos) {
        boolean isRemoved = false;
        for (var s : customDeposits.values()) {
            if (s.remove(customPos)) isRemoved = true;
        }
        return isRemoved;
    }

    public boolean isFound(ResourceKey<Structure> depKey, ChunkPos depStartChunk) {
        var foundSet = foundDeposits.get(depKey.location());
        if (foundSet == null) return false;
        return foundSet.contains(depStartChunk);
    }

    public boolean isCustomFound(BlockPos customPos) {
        for (var s : foundDeposits.values()) {
            if (s.contains(new ChunkPos(customPos))) return true;
        }
        return false;
    }

    public boolean setFound(ResourceKey<Structure> depositKey, ChunkPos depStartChunk, boolean val) {
        var centerPos = getDepositCenter(depStartChunk);
        if (centerPos == null) {
            CreateRNS.LOGGER.error("Attempted to mark ungenerated deposit at ~{},{} as {}",
                    depStartChunk.getMiddleBlockX(), depStartChunk.getMiddleBlockZ(), val ? "found" : "not found");
            return false;
        }

        foundDeposits.computeIfAbsent(depositKey.location(), l -> new ObjectOpenHashSet<>()).add(depStartChunk);

        // Remove all mentions of it from cache
        perPlayerCache.asMap().entrySet().removeIf(e -> {
            var cachedData = e.getValue();
            return cachedData != null && cachedData.i.startChunk == depStartChunk;
        });

        CreateRNS.LOGGER.debug("Marking {},{},{} as {}", centerPos.getX(), centerPos.getY(), centerPos.getZ(),
                val ? "found" : "not found");
        return true;
    }

    public boolean setCustomFound(ResourceKey<Structure> depositKey, BlockPos customPos, boolean val) {
        return setFound(depositKey, new ChunkPos(customPos), val);
    }

    public int initDepositVeinDurability(BlockPos start) {
        if (ServerConfig.infiniteDeposits) return 0;
        if (depositDurabilities.containsKey(start)) return 0;
        var startRecipe = MiningRecipeLookup.find(level, level.getBlockState(start).getBlock());
        if (startRecipe == null) return 0;
        var startDur = startRecipe.getDurability();
        // Infinite starts never initialize vein durabilities (but can be initialized from finite starts)
        if (startDur.edge() <= 0 || startDur.core() <= 0) return 0;

        var blockToDepth = DepositBlock.getVein(level, start);
        if (blockToDepth.isEmpty()) return 0;
        var maxDepth = blockToDepth.values().intStream().max().orElseThrow();

        int initCount = 0;
        for (var e : blockToDepth.object2IntEntrySet()) {
            var bp = e.getKey();
            if (depositDurabilities.containsKey(bp)) continue;
            var b = level.getBlockState(bp).getBlock();
            var r = MiningRecipeLookup.find(level, b);
            if (r == null) continue;
            float depthRatio = (maxDepth != 0) ? ((float) e.getIntValue() / maxDepth) : 0.5f;
            depositDurabilities.put(bp, rollDurability(r.getDurability(), depthRatio));
            initCount++;
        }

        return initCount;
    }

    /// Returns -1 if not initialized, 0 if infinite, actual durability otherwise.
    public long getDepositBlockDurability(BlockPos dbPos, boolean initIfNeeded) {
        if (ServerConfig.infiniteDeposits) return 0;
        if (initIfNeeded) initDepositVeinDurability(dbPos);
        if (!depositDurabilities.containsKey(dbPos)) return -1;
        return depositDurabilities.getLong(dbPos);
    }

    /// Returns 0 if infinite, actual durability otherwise.
    public long getDepositBlockDurability(BlockPos dbPos) {
        return getDepositBlockDurability(dbPos, true);
    }

    public boolean setDepositBlockDurability(BlockPos dbPos, long durability) {
        if (ServerConfig.infiniteDeposits) return false;
        depositDurabilities.put(dbPos, durability);
        return true;
    }

    public void removeDepositBlockDurability(BlockPos dbPos) {
        depositDurabilities.removeLong(dbPos);
    }

    public void useDepositBlock(BlockPos dbPos, BlockState replacementBlock) {
        if (ServerConfig.infiniteDeposits) return;
        initDepositVeinDurability(dbPos); // No-op if already initialized
        var dur = depositDurabilities.getLong(dbPos);
        if (dur == 1) {
            removeDepositBlockDurability(dbPos);
            level.setBlockAndUpdate(dbPos, replacementBlock);
            CreateRNS.LOGGER.trace("Depleted deposit at {},{},{}", dbPos.getX(), dbPos.getY(), dbPos.getZ());
        } else if (dur > 1) {
            depositDurabilities.addTo(dbPos, -1);
            CreateRNS.LOGGER.trace("Used deposit at {},{},{}: {} -> {}", dbPos.getX(), dbPos.getY(), dbPos.getZ(), dur, dur - 1);
        }
        // <= 0 means deposit is infinite
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var root = new CompoundTag();
        var found = new CompoundTag();
        var custom = new CompoundTag();
        var durabilities = new ListTag();

        CreateRNS.LOGGER.trace("Serializing found deposits {}", foundDeposits);
        CreateRNS.LOGGER.trace("Serializing custom deposits {}", customDeposits);
        CreateRNS.LOGGER.trace("Serializing durabilities ({} entries)", depositDurabilities.size());

        for (var e : foundDeposits.object2ObjectEntrySet()) {
            ResourceLocation rl = e.getKey();
            long[] packed = e.getValue().stream().mapToLong(ChunkPos::toLong).toArray();
            found.putLongArray(rl.toString(), packed);
        }

        for (var e : customDeposits.object2ObjectEntrySet()) {
            ResourceLocation rl = e.getKey();
            long[] packed = e.getValue().stream().mapToLong(BlockPos::asLong).toArray();
            custom.putLongArray(rl.toString(), packed);
        }

        for (var e : depositDurabilities.object2LongEntrySet()) {
            var d = new CompoundTag();
            d.putLong("pos", e.getKey().asLong());
            d.putLong("durability", e.getLongValue());
            durabilities.add(d);
        }

        root.put("found", found);
        root.put("custom", custom);
        root.put("durabilities", durabilities);

        CreateRNS.LOGGER.trace("Serialized level deposit data with {}", root);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        foundDeposits.clear();
        if (nbt.get("found") instanceof CompoundTag foundTag) {
            for (String key : foundTag.getAllKeys()) {
                var rl = ResourceLocation.parse(key);
                long[] packed = foundTag.getLongArray(key);
                var set = new ObjectOpenHashSet<ChunkPos>(packed.length);
                for (long l : packed) set.add(new ChunkPos(l));
                foundDeposits.put(rl, set);
            }
            CreateRNS.LOGGER.trace("Deserialized found deposits to {}", foundDeposits);
        } else {
            CreateRNS.LOGGER.error("Failed to deserialize found deposits from nbt");
        }

        customDeposits.clear();
        if (nbt.get("custom") instanceof CompoundTag customTag) {
            for (String key : customTag.getAllKeys()) {
                var rl = ResourceLocation.parse(key);
                long[] packed = customTag.getLongArray(key);
                var set = new ObjectOpenHashSet<BlockPos>(packed.length);
                for (long l : packed) set.add(BlockPos.of(l));
                customDeposits.put(rl, set);
            }
            CreateRNS.LOGGER.trace("Deserialized custom deposits to {}", customDeposits);
        } else {
            CreateRNS.LOGGER.error("Failed to deserialize custom deposits from nbt");
        }

        depositDurabilities.clear();
        if (!(nbt.get("durabilities") instanceof ListTag durabilities)) {
            CreateRNS.LOGGER.error("Failed to deserialize deposit durabilities from nbt");
            return;
        }
        for (var d : durabilities) {
            if (!(d instanceof CompoundTag dc)) continue;
            depositDurabilities.put(BlockPos.of(dc.getLong("pos")), dc.getLong("durability"));
        }
        CreateRNS.LOGGER.trace("Deserialized durabilities ({} entries)", depositDurabilities.size());
    }

    protected @Nullable BlockPos getDepositCenter(ChunkPos depStartChunk) {
        var isDeposit = DepositSpecLookup.isDeposit(level.registryAccess());

        var starts = level.getChunk(depStartChunk.x, depStartChunk.z, ChunkStatus.STRUCTURE_STARTS).getAllStarts();
        for (var ss : starts.values()) {
            if (ss != null && ss.isValid() && isDeposit.test(ss.getStructure())) {
                return ss.getBoundingBox().getCenter();
            }
        }
        return null;
    }

    protected @Nullable BlockPos getDepositCenter(BlockPos depStartPos) {
        return getDepositCenter(new ChunkPos(depStartPos));
    }

    /// Returns the center position of the nearest deposit.
    /// May ot be fully accurate until the deposit is generated in the world.
    /// Server player is only used for caching.
    protected DepPosInfo getNearest(ResourceKey<Structure> depKey, @Nullable ServerPlayer sp,
                                    ServerLevel sl, BlockPos pos, int searchRadiusChunks,
                                    boolean allow_discovered) {
        if (sp != null) {
            var hit = perPlayerCache.getIfPresent(sp.getUUID());

            // Okay, chill out buddy
            if (hit != null && sl.getGameTime() - hit.creationTimestamp < MIN_COMPUTE_INTERVAL) {
                return new DepPosInfo(null, null, null);
            }
        }

        var gen = sl.getChunkSource().getGenerator();
        var targetStructure = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(depKey);
        var searchRadiusRegions = searchRadiusChunks / DepositSetConfigBuilder.DEFAULT_SPACING;
        DepositCandidateFilter ignoreFoundFilter = (level, structure, chunkPos) ->
                structure == targetStructure.value() && isFound(depKey, chunkPos);

        // Get nearest custom deposit position and distance
        var depSet = customDeposits.get(depKey.location());
        var nearestCustom = (depSet == null) ? null : depSet.stream()
                .filter(dp -> !isFound(depKey, new ChunkPos(dp)))
                .min(Comparator.comparing(dp -> dp.distSqr(pos)))
                .orElse(null);
        double customDist = (nearestCustom != null) ? nearestCustom.distSqr(pos) : Double.MAX_VALUE;

        // Get nearest structure deposit position and distance
        Pair<BlockPos, Holder<Structure>> hit = null;
        // Thread-local context within this scope is used by a mixin to filter out found deposits
        try (var ignored = DepositScannerLocateContext.push(ignoreFoundFilter)) {
            hit = gen.findNearestMapStructure(sl, HolderSet.direct(targetStructure), pos, searchRadiusRegions, false);
        }
        var nearestNaturalStart = (hit != null) ? hit.getFirst() : null;
        var nearestNaturalCenter = (nearestNaturalStart != null) ? getDepositCenter(nearestNaturalStart) : null;
        var nearestNatural = (nearestNaturalCenter != null) ? nearestNaturalCenter : nearestNaturalStart;
        var naturalDist = (nearestNatural != null) ? nearestNatural.distSqr(pos) : Double.MAX_VALUE;

        // Calculate relevant positions based on proximity to the target position.
        DepPosInfo result = null;
        if (nearestCustom != null && customDist < naturalDist) {
            // Custom is nearest
            if (!isOutsideSearchRadius(pos, nearestCustom, searchRadiusChunks)) {
                result = new DepPosInfo(null, nearestCustom, nearestCustom);
            }
        } else if (nearestNaturalStart != null && nearestNaturalCenter == null) {
            // Natural ungenerated is nearest
            if (!isOutsideSearchRadius(pos, nearestNaturalStart, searchRadiusChunks)) {
                result = new DepPosInfo(new ChunkPos(nearestNaturalStart), null, nearestNaturalStart);
            }
        } else if (nearestNaturalCenter != null) {
            // Natural generated is nearest
            if (!isOutsideSearchRadius(pos, nearestNaturalCenter, searchRadiusChunks)) {
                result = new DepPosInfo(new ChunkPos(nearestNaturalStart), nearestNaturalCenter, nearestNaturalCenter);
            }
        }
        if (result == null) result = new DepPosInfo(null, null, null);

        // Cache the result if possible
        if (sp != null) perPlayerCache.put(sp.getUUID(), new CachedData(depKey, result, sl.getGameTime()));

        // Print log with result
        if (result.bestEffortPos == null) {
            CreateRNS.LOGGER.debug("Could not find deposits nearby");
        } else {
            var qualifier = (result.startChunk == null) ? "custom"
                    : ((result.accuratePos == null) ? "ungenerated" : "generated");
            var yCoord = (result.accuratePos != null) ? result.bestEffortPos.getY() : "~";
            CreateRNS.LOGGER.debug("Found {} deposit at {},{},{}", qualifier,
                    result.bestEffortPos.getX(), yCoord, result.bestEffortPos.getZ());
        }

        return result;
    }

    protected boolean isOutsideSearchRadius(BlockPos pos, ChunkPos depChunk, int searchRadiusChunks) {
        // Use Chebyshev distance for parity with findNearestMapStructure
        var distX = Math.abs(SectionPos.blockToSectionCoord(pos.getX()) - depChunk.x);
        var distZ = Math.abs(SectionPos.blockToSectionCoord(pos.getZ()) - depChunk.z);
        return Math.max(distX, distZ) > searchRadiusChunks;
    }

    protected boolean isOutsideSearchRadius(BlockPos pos, BlockPos depPos, int searchRadiusChunks) {
        return isOutsideSearchRadius(pos, new ChunkPos(depPos), searchRadiusChunks);
    }

    /// Durabilities for all deposits fall within that range based on their depth.
    /// For each depth, there is a yet another range which confines the possible random durability values.
    ///
    /// E.g. assume depth ratio is 0.3, then:
    /// \[--(--)--------] where
    /// Square brackets are minimum and maximum durabilities across all deposits in the vein.
    /// Parentheses are minimum and maximum durabilities for the given depth.
    ///
    /// If vein is infinite, 0 is returned. Otherwise, the return value is random, but guaranteed to lie within both ranges.
    protected long rollDurability(DepositDurability dur, float depthRatio) {
        assert 0f <= depthRatio && depthRatio <= 1f;

        long minDur = dur.edge();
        long maxDur = dur.core();
        long range = maxDur - minDur;
        if (minDur <= 0 || maxDur <= 0) {
            CreateRNS.LOGGER.trace("Skipped roll for infinite deposit");
            return 0;
        }

        // Average durability at given depth and its maximum spread (deviation)
        long curDur = (long) ((maxDur - minDur) * depthRatio + minDur);
        long spread = (long) (dur.randomSpread() * curDur);

        // Range of depth durabilities (aka the parentheses) are clamped to the absolute range (aka the square brackets)
        long minDepthDur = Mth.clamp(curDur - spread, minDur, maxDur - spread);
        long maxDepthDur = Mth.clamp(curDur + spread, minDur + spread, maxDur);
        long depthRange = maxDepthDur - minDepthDur;

        long roll = (depthRange != 0) ? ((Math.abs(level.random.nextLong()) % depthRange) + minDepthDur) : minDepthDur;

        long numBarsBefore = (range != 0) ? (Math.round(30 * ((double) (roll - minDur) / range))) : 15;
        CreateRNS.LOGGER.trace("Rolled deposit durability: [{}]{}x{}[{}] {}",
                minDur, "-".repeat((int) numBarsBefore), "-".repeat(30 - (int) numBarsBefore), maxDur, roll);

        return roll;
    }

    public record DepPosInfo(
            @Nullable ChunkPos startChunk,
            @Nullable BlockPos accuratePos,
            @Nullable BlockPos bestEffortPos) {}

    private record CachedData(
            ResourceKey<Structure> depKey,
            DepPosInfo i,
            long creationTimestamp
    ) {}
}

package com.bmaster.createrns.deposit;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.gen.depositworldgen.DepositSetConfigBuilder;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.mining.MiningRecipeLookup;
import com.bmaster.createrns.mining.recipe.MiningRecipe;
import com.bmaster.createrns.util.Utils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.TimeUnit;

@ParametersAreNonnullByDefault
public class LevelDepositData implements INBTSerializable<CompoundTag> {
    public static final int MIN_COMPUTE_INTERVAL = 90;

    private final Level level;

    // Generated found deposits are represented as bounding box centers of deposit structures (not bound to res. loc.)
    private final Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<BlockPos>> generatedFoundDeposits =
            new Object2ObjectOpenHashMap<>();

    // Generated deposits are represented as bounding box centers of deposit structures
    private final Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<BlockPos>> generatedDeposits =
            new Object2ObjectOpenHashMap<>();

    // Ungenerated deposits are represented as positions of deposit structure starts since ungenerated deposits
    // do not yet have a bounding box.
    private final Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<BlockPos>> ungeneratedDeposits =
            new Object2ObjectOpenHashMap<>();

    private final Cache<UUID, CachedData> perPlayerCache = CacheBuilder.newBuilder()
            .initialCapacity(1)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    private final Object2LongOpenHashMap<BlockPos> depositDurabilities = new Object2LongOpenHashMap<>();

    public LevelDepositData(Level level) {
        this.level = level;
    }

    public @Nullable ResourceKey<Structure> getType(BlockPos pos) {
        for (var e : generatedFoundDeposits.object2ObjectEntrySet()) {
            if (e.getValue().contains(pos)) {
                return ResourceKey.create(Registries.STRUCTURE, e.getKey());
            }
        }
        for (var e : generatedDeposits.object2ObjectEntrySet()) {
            if (e.getValue().contains(pos)) {
                return ResourceKey.create(Registries.STRUCTURE, e.getKey());
            }
        }
        for (var e : ungeneratedDeposits.object2ObjectEntrySet()) {
            if (e.getValue().contains(pos)) {
                return ResourceKey.create(Registries.STRUCTURE, e.getKey());
            }
        }
        return null;
    }

    public @Nullable BlockPos getNearest(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks) {
        return getNearest(depositKey, sp, sp.serverLevel(), sp.blockPosition(), searchRadiusChunks, false, false);
    }

    public @Nullable BlockPos getNearest(ResourceKey<Structure> depositKey, ServerLevel level, BlockPos pos,
                                         int searchRadiusChunks, boolean allow_discovered, boolean generatedOnly) {
        return getNearest(depositKey, null, level, pos, searchRadiusChunks, allow_discovered, generatedOnly);
    }

    public @Nullable BlockPos getNearestCached(ResourceKey<Structure> depositKey, ServerPlayer sp,
                                               int searchRadiusChunks) {
        var hit = perPlayerCache.getIfPresent(sp.getUUID());
        if (hit == null) {
            CreateRNS.LOGGER.trace("[Cache miss] Deposit position is not cached for player");
            return null;
        } else {
            if (hit.dPosPacked == null) {
                CreateRNS.LOGGER.trace("[Cache hit] Deposit position in cache is null");
                return null;
            }
            var cachedBP = BlockPos.of(hit.dPosPacked);
            if (!hit.depositKey.equals(depositKey)) {
                CreateRNS.LOGGER.trace("[Cache hit] Found deposit at {},{}, but its type does not match the " +
                        "requested type", cachedBP.getX(), cachedBP.getZ());
            }
            if (isOutsideSearchRadius(sp.blockPosition(), cachedBP, searchRadiusChunks)) {
                CreateRNS.LOGGER.trace("[Cache hit] Found deposit at {},{}, but it's too far away",
                        cachedBP.getX(), cachedBP.getZ());
                return null;
            }
            CreateRNS.LOGGER.trace("[Cache hit] Found deposit at {},{}", cachedBP.getX(), cachedBP.getZ());
            return cachedBP;
        }
    }

    public void addDeposit(ResourceKey<Structure> depositKey, StructureStart ss) {
        var startChunk = ss.getChunkPos();
        if (!ss.isValid()) {
            CreateRNS.LOGGER.error("Attempted to add an invalid deposit start to level data");
            return;
        }
        var center = ss.getBoundingBox().getCenter();

        // Remove newly-generated deposit start from the list of ungenerated deposits
        for (var t : ungeneratedDeposits.object2ObjectEntrySet()) {
            var dSet = t.getValue();
            boolean removed = dSet.removeIf(d -> Utils.isPosInChunk(d, startChunk));
            if (removed) {
                // Also modify cached positions to point to structure center
                perPlayerCache.asMap().replaceAll((u, d) -> {
                    if (d == null) return null;
                    if (d.dPosPacked == null || !Utils.isPosInChunk(BlockPos.of(d.dPosPacked), startChunk)) return d;
                    return new CachedData(center.asLong(), d.depositKey, d.creationTimestamp);
                });
                CreateRNS.LOGGER.debug("Generated deposit start at {},{} removed from ungenerated",
                        startChunk.getBlockX(8), startChunk.getBlockZ(8));
            }
        }

        // Do not add to generated if it already exists in generated+found
        var genFoundSet = generatedFoundDeposits.get(depositKey.location());
        if (genFoundSet != null && genFoundSet.contains(center)) {
            return;
        }

        // Add to the set of generated deposits
        generatedDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>()).add(center);
    }

    public void addDeposit(ResourceKey<Structure> depositKey, BlockPos pos) {
        generatedDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>()).add(pos);
    }

    public boolean removeDeposit(BlockPos pos) {
        boolean isRemoved = false;
        for (var s : generatedFoundDeposits.values()) {
            if (s.remove(pos)) isRemoved = true;
        }
        for (var s : generatedDeposits.values()) {
            if (s.remove(pos)) isRemoved = true;
        }
        for (var s : ungeneratedDeposits.values()) {
            if (s.remove(pos)) isRemoved = true;
        }
        return isRemoved;
    }

    public boolean isFound(BlockPos pos) {
        for (var s : generatedFoundDeposits.values()) {
            if (s.contains(pos)) return true;
        }
        return false;
    }

    public boolean setFound(ResourceKey<Structure> depositKey, BlockPos centerPos, boolean val) {
        Set<BlockPos> genSet = generatedDeposits.values().stream()
                .filter(s -> s.contains(centerPos))
                .findFirst().orElse(null);
        Set<BlockPos> genFoundSet = generatedFoundDeposits.values().stream()
                .filter(s -> s.contains(centerPos))
                .findFirst().orElse(null);

        if (genSet == null && genFoundSet == null) {
            CreateRNS.LOGGER.error("Attempted to mark a non-existent deposit at {},{},{} as {}",
                    centerPos.getX(), centerPos.getY(), centerPos.getZ(), val ? "found" : "not found");
            return false;
        }

        // Remove all mentions of it from cache
        perPlayerCache.asMap().entrySet().removeIf(e -> {
            var d = e.getValue();
            return d != null && d.dPosPacked != null && d.dPosPacked == centerPos.asLong();
        });

        // Transfer to a new set
        if (val) {
            if (genSet != null) genSet.remove(centerPos);
            generatedFoundDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>()).add(centerPos);
        } else {
            if (genFoundSet != null) genFoundSet.remove(centerPos);
            generatedDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>()).add(centerPos);
        }
        CreateRNS.LOGGER.debug("Marking {},{},{} as {}", centerPos.getX(), centerPos.getY(), centerPos.getZ(),
                val ? "found" : "not found");
        return true;
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
        var generatedFound = new CompoundTag();
        var generated = new CompoundTag();
        var ungenerated = new CompoundTag();
        var durabilities = new ListTag();

        CreateRNS.LOGGER.trace("Serializing generated_found {}", generatedFoundDeposits);
        CreateRNS.LOGGER.trace("Serializing generated {}", generatedDeposits);
        CreateRNS.LOGGER.trace("Serializing ungenerated {}", ungeneratedDeposits);
        CreateRNS.LOGGER.trace("Serializing durabilities ({} entries)", depositDurabilities.size());

        for (var e : generatedFoundDeposits.object2ObjectEntrySet()) {
            ResourceLocation rl = e.getKey();
            long[] packed = e.getValue().stream().mapToLong(BlockPos::asLong).toArray();
            generatedFound.putLongArray(rl.toString(), packed);
        }

        for (var e : generatedDeposits.object2ObjectEntrySet()) {
            ResourceLocation rl = e.getKey();
            long[] packed = e.getValue().stream().mapToLong(BlockPos::asLong).toArray();
            generated.putLongArray(rl.toString(), packed);
        }

        for (var e : ungeneratedDeposits.object2ObjectEntrySet()) {
            ResourceLocation rl = e.getKey();
            long[] packed = e.getValue().stream().mapToLong(BlockPos::asLong).toArray();
            ungenerated.putLongArray(rl.toString(), packed);
        }

        for (var e : depositDurabilities.object2LongEntrySet()) {
            var d = new CompoundTag();
            d.putLong("pos", e.getKey().asLong());
            d.putLong("durability", e.getLongValue());
            durabilities.add(d);
        }

        root.put("generated_found", generatedFound);
        root.put("generated", generated);
        root.put("ungenerated", ungenerated);
        root.put("durabilities", durabilities);

        CreateRNS.LOGGER.trace("Serialized level deposit data with {}", root);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (!(nbt.get("generated_found") instanceof CompoundTag generatedFound) ||
                !(nbt.get("generated") instanceof CompoundTag generated) ||
                !(nbt.get("ungenerated") instanceof CompoundTag ungenerated) ||
                !(nbt.get("durabilities") instanceof ListTag durabilities)) {
            CreateRNS.LOGGER.error("Failed to deserialize level deposit data from nbt");
            return;
        }
        CreateRNS.LOGGER.trace("Deserializing level deposit data with tag {}", nbt);
        generatedFoundDeposits.clear();
        generatedDeposits.clear();
        ungeneratedDeposits.clear();
        depositDurabilities.clear();

        for (String key : generatedFound.getAllKeys()) {
            var rl = ResourceLocation.parse(key);
            long[] packed = generatedFound.getLongArray(key);
            var set = new ObjectOpenHashSet<BlockPos>(packed.length);
            for (long l : packed) set.add(BlockPos.of(l));
            generatedFoundDeposits.put(rl, set);
        }
        for (String key : generated.getAllKeys()) {
            var rl = ResourceLocation.parse(key);
            long[] packed = generated.getLongArray(key);
            var set = new ObjectOpenHashSet<BlockPos>(packed.length);
            for (long l : packed) set.add(BlockPos.of(l));
            generatedDeposits.put(rl, set);
        }
        for (String key : ungenerated.getAllKeys()) {
            var rl = ResourceLocation.parse(key);
            long[] packed = ungenerated.getLongArray(key);
            var set = new ObjectOpenHashSet<BlockPos>(packed.length);
            for (long l : packed) set.add(BlockPos.of(l));
            ungeneratedDeposits.put(rl, set);
        }
        for (var d : durabilities) {
            if (!(d instanceof CompoundTag dc)) continue;
            depositDurabilities.put(BlockPos.of(dc.getLong("pos")), dc.getLong("durability"));
        }
        CreateRNS.LOGGER.trace("Deserialized generated_found to {}", generatedFoundDeposits);
        CreateRNS.LOGGER.trace("Deserialized generated to {}", generatedDeposits);
        CreateRNS.LOGGER.trace("Deserialized ungenerated to {}", ungeneratedDeposits);
        CreateRNS.LOGGER.trace("Deserialized durabilities ({} entries)", depositDurabilities.size());
    }

    private @Nullable BlockPos getNearest(ResourceKey<Structure> depositKey, @Nullable ServerPlayer sp,
                                            ServerLevel sl, BlockPos pos, int searchRadiusChunks,
                                          boolean allow_discovered, boolean generated_only) {
        if (sp != null) {
            var hit = perPlayerCache.getIfPresent(sp.getUUID());

            // Okay, chill out buddy
            if (hit != null && sl.getGameTime() - hit.creationTimestamp < MIN_COMPUTE_INTERVAL) {
                return null;
            }
        }

        BlockPos closestBP = null;
        double closestDist = Double.MAX_VALUE;
        double dist;

        for (var d : generatedDeposits.getOrDefault(depositKey.location(), new ObjectOpenHashSet<>())) {
            dist = pos.distSqr(d);
            if (dist < closestDist) {
                closestDist = dist;
                closestBP = new BlockPos(d);
            }
        }

        for (var d : ungeneratedDeposits.getOrDefault(depositKey.location(), new ObjectOpenHashSet<>())) {
            dist = pos.distSqr(d);
            if (dist < closestDist) {
                closestDist = dist;
                closestBP = new BlockPos(d);
            }
        }

        if (allow_discovered) {
            for (var d : generatedFoundDeposits.getOrDefault(depositKey.location(), new ObjectOpenHashSet<>())) {
                dist = pos.distSqr(d);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestBP = new BlockPos(d);
                }
            }
        }

        if (!generated_only) {
            // Discover the closest unknown deposit and use it if it's closer than any known deposit (square distance)
            var closestUnknownBP = discoverNearest(depositKey, sl, pos, searchRadiusChunks);
            if (closestUnknownBP != null && pos.distSqr(closestUnknownBP) < closestDist) {
                closestBP = closestUnknownBP;
            }
        }

        // Cache and return result
        if (closestBP == null) {
            if (sp != null) perPlayerCache.put(sp.getUUID(), new CachedData(null, depositKey, sl.getGameTime()));
            CreateRNS.LOGGER.debug("No deposits of target type are recorded");
            return null;
        }
        if (isOutsideSearchRadius(pos, closestBP, searchRadiusChunks)) {
            if (sp != null) perPlayerCache.put(sp.getUUID(), new CachedData(closestBP.asLong(), depositKey, sl.getGameTime()));
            CreateRNS.LOGGER.debug("No deposits in scanned area. Closest is at {},{} ({} blocks away)",
                    closestBP.getX(), closestBP.getZ(), (int) Math.sqrt(closestDist));
            return null;
        }
        if (sp != null) perPlayerCache.put(sp.getUUID(), new CachedData(closestBP.asLong(), depositKey, sl.getGameTime()));
        CreateRNS.LOGGER.debug("Found deposit at {},{}", closestBP.getX(), closestBP.getZ());
        return closestBP;
    }

    private @Nullable BlockPos discoverNearest(ResourceKey<Structure> depositKey, ServerLevel sl, BlockPos pos,
                                               int searchRadiusChunks) {
        var gen = sl.getChunkSource().getGenerator();
        var target = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(depositKey);
        var searchRadiusRegions = searchRadiusChunks / DepositSetConfigBuilder.DEFAULT_SPACING;

        var newDeposit = gen.findNearestMapStructure(sl, HolderSet.direct(target), pos,
                searchRadiusRegions, true);

        // If found invalid (not yet generated) deposit, save it to the ungenerated list
        var ss = (newDeposit == null) ? null : sl.structureManager().getStructureWithPieceAt(newDeposit.getFirst(),
                newDeposit.getSecond().value());
        if (newDeposit != null && !ss.isValid()) {
            var startPos = newDeposit.getFirst();
            CreateRNS.LOGGER.trace("Found undiscovered deposit at {}, {}, {}", startPos.getX(), startPos.getY(),
                    startPos.getZ());
            ungeneratedDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>()).add(startPos);
            return startPos;
        }
        // If found valid (generated) deposit, save it to the generated list
        else if (newDeposit != null) {
            var centerPos = ss.getBoundingBox().getCenter();
            CreateRNS.LOGGER.trace("Found undiscovered generated deposit at {}, {}, {}", centerPos.getX(), centerPos.getY(),
                    centerPos.getZ());
            generatedDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>()).add(centerPos);
            return centerPos;
        }
        return null;
    }

    private boolean isOutsideSearchRadius(BlockPos playerPos, BlockPos dPos, int searchRadiusChunks) {
        // Use Chebyshev distance for parity with findNearestMapStructure
        var distX = Math.abs(playerPos.getX() - dPos.getX());
        var distZ = Math.abs(playerPos.getZ() - dPos.getZ());
        return Math.max(distX, distZ) > searchRadiusChunks << 4;
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
    private long rollDurability(MiningRecipe.Durability dur, float depthRatio) {
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

    private record CachedData(@Nullable Long dPosPacked, ResourceKey<Structure> depositKey, long creationTimestamp) {}
}

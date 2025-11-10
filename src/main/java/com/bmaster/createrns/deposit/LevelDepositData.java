package com.bmaster.createrns.deposit;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.data.gen.depositworldgen.DepositSetConfigBuilder;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.mining.MiningRecipeLookup;
import com.bmaster.createrns.mining.recipe.MiningRecipe;
import com.bmaster.createrns.util.Utils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
public class LevelDepositData implements INBTSerializable<CompoundTag> {
    public static final int MIN_COMPUTE_INTERVAL = 90;
    public static final int MAX_DEPOSIT_VEIN_SIZE = 128;
    public static final float RANDOM_SPREAD_BOUNDARY = 0.2f;
    private static final Set<Direction> xzDirections = Set.of(
            Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.NORTH);

    private final Level level;

    // Generated found deposits are represented as bounding box centers of deposit structures (not bound to res. loc.)
    private final ObjectOpenHashSet<BlockPos> generatedFoundDeposits = new ObjectOpenHashSet<>();

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

    public @Nullable BlockPos getNearest(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks) {
        var sl = sp.level();
        var hit = perPlayerCache.getIfPresent(sp.getUUID());

        // Okay, chill out buddy
        if (hit != null && sl.getGameTime() - hit.creationTimestamp < MIN_COMPUTE_INTERVAL) {
            return null;
        }

        var playerPos = sp.blockPosition();
        BlockPos closestBP = null;
        double closestDist = Double.MAX_VALUE;
        double dist;

        // Get closest generated deposit. Use square distance for best precision.
        for (var d : generatedDeposits.getOrDefault(depositKey.location(), new ObjectOpenHashSet<>())) {
            dist = playerPos.distSqr(d);
            if (dist < closestDist) {
                closestDist = dist;
                closestBP = new BlockPos(d);
            }
        }

        // Get closest ungenerated, but known deposit. Use square distance for best precision.
        for (var d : ungeneratedDeposits.getOrDefault(depositKey.location(), new ObjectOpenHashSet<>())) {
            dist = playerPos.distSqr(d);
            if (dist < closestDist) {
                closestDist = dist;
                closestBP = new BlockPos(d);
            }
        }

        // Discover the closest unknown deposit and use it if it's closer than any known deposit (square distance)
        var closestUnknownBP = discoverNearest(depositKey, sp, searchRadiusChunks);
        if (closestUnknownBP != null && playerPos.distSqr(closestUnknownBP) < closestDist) {
            closestBP = closestUnknownBP;
        }

        // Cache and return result
        if (closestBP == null) {
            perPlayerCache.put(sp.getUUID(), new CachedData(null, depositKey, sl.getGameTime()));
            CreateRNS.LOGGER.debug("No deposits of target type are recorded");
            return null;
        }
        if (isOutsideSearchRadius(playerPos, closestBP, searchRadiusChunks)) {
            perPlayerCache.put(sp.getUUID(), new CachedData(closestBP.asLong(), depositKey, sl.getGameTime()));
            CreateRNS.LOGGER.debug("No deposits in scanned area. Closest is at {},{} ({} blocks away)",
                    closestBP.getX(), closestBP.getZ(), (int) Math.sqrt(closestDist));
            return null;
        }
        perPlayerCache.put(sp.getUUID(), new CachedData(closestBP.asLong(), depositKey, sl.getGameTime()));
        CreateRNS.LOGGER.debug("Found deposit at {},{}", closestBP.getX(), closestBP.getZ());
        return closestBP;
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

    public void add(ResourceKey<Structure> depositKey, StructureStart ss, ServerLevel sl) {
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
        if (generatedFoundDeposits.contains(center)) {
            return;
        }

        // Add to the set of generated deposits
        generatedDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>()).add(center);
    }

    public void markAsFound(BlockPos centerPos) {
        Set<BlockPos> foundGenSet = null;

        // Find generated deposit with provided center pos
        for (var e : generatedDeposits.object2ObjectEntrySet()) {
            var genSet = e.getValue();
            if (genSet.contains(centerPos)) foundGenSet = genSet;

        }
        if (foundGenSet == null) {
            CreateRNS.LOGGER.error("Attempted to mark a non-existent deposit");
            return;
        }

        // Remove all mentions of it from cache
        perPlayerCache.asMap().entrySet().removeIf(e -> {
            var d = e.getValue();
            return d != null && d.dPosPacked != null && d.dPosPacked == centerPos.asLong();
        });

        // Remove it from generated, add to generated+found
        foundGenSet.remove(centerPos);
        generatedFoundDeposits.add(centerPos);
        CreateRNS.LOGGER.debug("Marking {},{},{} as found", centerPos.getX(), centerPos.getY(), centerPos.getZ());
    }

    public void initDepositVeinDurability(BlockPos start) {
        if (ServerConfig.infiniteDeposits) return;
        if (depositDurabilities.containsKey(start)) return;
        var startRecipe = MiningRecipeLookup.find(level, level.getBlockState(start).getBlock());
        if (startRecipe == null) return;
        var startDur = startRecipe.getDurability();
        // Infinite starts never initialize vein durabilities (but can be initialized from finite starts)
        if (startDur.edge() <= 0 || startDur.core() <= 0) return;

        var blockToDepth = getDepositVein(start);
        if (blockToDepth.isEmpty()) return;
        var maxDepth = blockToDepth.values().intStream().max().orElseThrow();

        for (var e : blockToDepth.object2IntEntrySet()) {
            var bp = e.getKey();
            if (depositDurabilities.containsKey(bp)) continue;
            var b = level.getBlockState(bp).getBlock();
            var r = MiningRecipeLookup.find(level, b);
            if (r == null) continue;
            float depthRatio = (maxDepth != 0) ? ((float) e.getIntValue() / maxDepth) : 0.5f;
            depositDurabilities.put(bp, rollDurability(r.getDurability(), depthRatio));
        }
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
        long[] generatedFound;
        var generated = new CompoundTag();
        var ungenerated = new CompoundTag();
        var durabilities = new ListTag();

        CreateRNS.LOGGER.trace("Serializing generated_found {}", generatedFoundDeposits);
        CreateRNS.LOGGER.trace("Serializing generated {}", generatedDeposits);
        CreateRNS.LOGGER.trace("Serializing ungenerated {}", ungeneratedDeposits);
        CreateRNS.LOGGER.trace("Serializing durabilities ({} entries)", depositDurabilities.size());

        generatedFound = generatedFoundDeposits.stream().mapToLong(BlockPos::asLong).toArray();

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

        root.putLongArray("generated_found", generatedFound);
        root.put("generated", generated);
        root.put("ungenerated", ungenerated);
        root.put("durabilities", durabilities);

        CreateRNS.LOGGER.trace("Serialized level deposit data with {}", root);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (!(nbt.get("generated_found") instanceof LongArrayTag generatedFound) ||
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

        for (long l : generatedFound.getAsLongArray()) {
            generatedFoundDeposits.add(BlockPos.of(l));
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

    private @Nullable BlockPos discoverNearest(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks) {
        var sl = (ServerLevel) sp.level();
        var gen = sl.getChunkSource().getGenerator();
        var target = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(depositKey);
        var searchRadiusRegions = searchRadiusChunks / DepositSetConfigBuilder.DEFAULT_SPACING;

        var newDeposit = gen.findNearestMapStructure(sl, HolderSet.direct(target), sp.blockPosition(),
                searchRadiusRegions, true);

        // If found invalid (not yet generated) deposit, save it to the ungenerated list
        var ss = (newDeposit == null) ? null : sl.structureManager().getStructureWithPieceAt(newDeposit.getFirst(),
                newDeposit.getSecond().value());
        if (newDeposit != null && !ss.isValid()) {
            var pos = newDeposit.getFirst();
            CreateRNS.LOGGER.trace("Found undiscovered deposit at {}, {}, {}", pos.getX(), pos.getY(),
                    pos.getZ());
            ungeneratedDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>()).add(pos);
            return pos;
        }
        // If found valid (generated) deposit, save it to the generated list
        else if (newDeposit != null) {
            var pos = ss.getBoundingBox().getCenter();
            CreateRNS.LOGGER.trace("Found undiscovered generated deposit at {}, {}, {}", pos.getX(), pos.getY(),
                    pos.getZ());
            generatedDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>()).add(pos);
            return pos;
        }
        return null;
    }

    private boolean isOutsideSearchRadius(BlockPos playerPos, BlockPos dPos, int searchRadiusChunks) {
        // Use Chebyshev distance for parity with findNearestMapStructure
        var distX = Math.abs(playerPos.getX() - dPos.getX());
        var distZ = Math.abs(playerPos.getZ() - dPos.getZ());
        return Math.max(distX, distZ) > searchRadiusChunks << 4;
    }

    private Object2IntOpenHashMap<BlockPos> getDepositVein(BlockPos start) {
        Queue<BlockPos> q = new ArrayDeque<>();
        Object2IntOpenHashMap<BlockPos> visited = new Object2IntOpenHashMap<>();
        if (!level.getBlockState(start).is(RNSTags.Block.DEPOSIT_BLOCKS)) return visited;
        q.add(start);

        // Collect all blocks in the deposit vein. Assign depth of outer blocks to 0, all other to MAX_VALUE.
        int depth = 0;
        while (!q.isEmpty() && depth < LevelDepositData.MAX_DEPOSIT_VEIN_SIZE) {
            var bp = q.poll();
            if (visited.containsKey(bp)) continue;

            AtomicBoolean external = new AtomicBoolean(false);
            Direction.stream().forEach(d -> {
                var nb = bp.relative(d);
                if (level.getBlockState(nb).is(RNSTags.Block.DEPOSIT_BLOCKS)) {
                    q.add(bp.relative(d));
                } else {
                    if (xzDirections.contains(d)) external.set(true);
                }
            });
            visited.put(bp, external.get() ? 0 : Integer.MAX_VALUE);
            ++depth;
        }

        // Start with outer blocks whose depth is 0. Compute depth of their neighbors until all blocks are processed.
        for (depth = 0; depth < MAX_DEPOSIT_VEIN_SIZE; ++depth) {
            int finalDepth = depth;
            var curDepthBlocks = visited.object2IntEntrySet().stream()
                    .filter(e -> e.getIntValue() == finalDepth)
                    .collect(Collectors.toSet());
            if (curDepthBlocks.isEmpty()) break;

            for (var e : curDepthBlocks) {
                xzDirections.forEach(d -> {
                    var neighbor = e.getKey().relative(d);
                    if (!visited.containsKey(neighbor)) return;
                    visited.computeInt(neighbor, (k, v) -> Math.min(v, finalDepth + 1));
                });
            }
        }

        if (visited.containsValue(Integer.MAX_VALUE)) {
            throw new IllegalStateException("Could not process deposit vein starting at %s,%s,%s"
                    .formatted(start.getX(), start.getY(), start.getZ()));
        }

        return visited;
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
    private int rollDurability(MiningRecipe.Durability dur, float depthRatio) {
        assert 0f <= depthRatio && depthRatio <= 1f;

        int minDur = dur.edge();
        int maxDur = dur.core();
        if (minDur <= 0 || maxDur <= 0) {
            CreateRNS.LOGGER.trace("Skipped roll for infinite deposit");
            return 0;
        }

        // Average durability at given depth and its maximum spread (deviation)
        int curDur = (int) ((maxDur - minDur) * depthRatio + minDur);
        int spread = (int) (dur.randomSpread() * curDur);

        // Range of depth durabilities (aka the parentheses) are clamped to the absolute range (aka the square brackets)
        int minDepthDur = Mth.clamp(curDur - spread, minDur, maxDur - spread);
        int maxDepthDur = Mth.clamp(curDur + spread, minDur + spread, maxDur);

        int roll = level.random.nextIntBetweenInclusive(minDepthDur, maxDepthDur);

        int numBarsBefore = Math.round(30 * (float) (roll - minDur) / (maxDur - minDur));
        CreateRNS.LOGGER.trace("Rolled deposit durability: [{}]{}x{}[{}] {}",
                minDur, "-".repeat(numBarsBefore), "-".repeat(30 - numBarsBefore), maxDur, roll);

        return roll;
    }

    private record CachedData(@Nullable Long dPosPacked, ResourceKey<Structure> depositKey, long creationTimestamp) {}
}

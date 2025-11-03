package com.bmaster.createrns.deposit.capability;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.gen.depositworldgen.DepositSetConfigBuilder;
import com.bmaster.createrns.util.Utils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.common.util.INBTSerializable;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DepositIndex implements IDepositIndex, INBTSerializable<CompoundTag> {
    public static final int MIN_COMPUTE_INTERVAL = 90;

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

    @Override
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

    @Override
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

    @Override
    public void add(ResourceKey<Structure> depositKey, StructureStart ss, ServerLevel sl) {
        var startChunk = ss.getChunkPos();
        if (!ss.isValid()) {
            CreateRNS.LOGGER.error("Attempted to add an invalid deposit start to deposit index");
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

    @Override
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

    @Override
    public CompoundTag serializeNBT() {
        var root = new CompoundTag();
        long[] generatedFound;
        var generated = new CompoundTag();
        var ungenerated = new CompoundTag();

        CreateRNS.LOGGER.trace("Serializing GeneratedFound {}", generatedFoundDeposits);
        CreateRNS.LOGGER.trace("Serializing Generated {}", generatedDeposits);
        CreateRNS.LOGGER.trace("Serializing Ungenerated {}", ungeneratedDeposits);

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

        root.putLongArray("GeneratedFound", generatedFound);
        root.put("Generated", generated);
        root.put("Ungenerated", ungenerated);

        CreateRNS.LOGGER.trace("Serialized deposit index with {}", root);
        return root;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (!(nbt.get("GeneratedFound") instanceof LongArrayTag generatedFound) ||
                !(nbt.get("Generated") instanceof CompoundTag generated) ||
                !(nbt.get("Ungenerated") instanceof CompoundTag ungenerated)) {
            CreateRNS.LOGGER.error("Failed to deserialize deposit index from nbt");
            return;
        }
        CreateRNS.LOGGER.trace("Deserializing deposit index with tag {}", nbt);
        generatedFoundDeposits.clear();
        generatedDeposits.clear();
        ungeneratedDeposits.clear();

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
        CreateRNS.LOGGER.trace("Deserialized GeneratedFound to {}", generatedFoundDeposits);
        CreateRNS.LOGGER.trace("Deserialized Generated to {}", generatedDeposits);
        CreateRNS.LOGGER.trace("Deserialized Ungenerated to {}", ungeneratedDeposits);
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
                newDeposit.getSecond().get());
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

    private record CachedData(@Nullable Long dPosPacked, ResourceKey<Structure> depositKey, long creationTimestamp) {}
}

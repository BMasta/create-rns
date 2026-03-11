package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSMisc;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class DepositLocation {
    public static final int MIN_COMPUTE_INTERVAL = 90;
    protected static final Object2ObjectOpenHashMap<ServerLevel, Cache<UUID, CachedData>> perLevelPerPlayerCache =
            new Object2ObjectOpenHashMap<>();

    /// Used when requested by a non-player entity
    public static @Nullable DepositLocation getNearest(
            ServerLevel sl, ResourceKey<Structure> depKey, BlockPos pos,
            boolean allowFound, int searchRadiusChunks
    ) {
        return getNearest(sl, depKey, pos, allowFound, searchRadiusChunks, null);
    }

    /// Used when requested by a player
    public static @Nullable DepositLocation getNearest(
            ServerPlayer sp, ResourceKey<Structure> depKey,
            boolean allowFound, int searchRadiusChunks, boolean cached
    ) {
        if (cached) {
            return getNearest(sp.serverLevel(), depKey, sp.blockPosition(), allowFound, searchRadiusChunks, sp);
        } else {
            return getNearestCached(sp, depKey, searchRadiusChunks);
        }
    }

    protected static @Nullable DepositLocation getNearest(
            ServerLevel sl, ResourceKey<Structure> depKey, BlockPos pos,
            boolean allowFound, int searchRadiusChunks, @Nullable ServerPlayer sp
    ) {
        Cache<UUID, CachedData> perPlayerCache = null;
        if (sp != null) {
            perPlayerCache = perLevelPerPlayerCache.computeIfAbsent(sl, ignored -> CacheBuilder.newBuilder()
                    .initialCapacity(1)
                    .expireAfterAccess(10, TimeUnit.MINUTES)
                    .build()
            );
            var hit = perPlayerCache.getIfPresent(sp.getUUID());

            // Okay, chill out buddy
            if (hit != null && sl.getGameTime() - hit.creationTimestamp < MIN_COMPUTE_INTERVAL) return null;
        }

        var custom = CustomDepositLocation.getNearestCustom(sl, depKey, pos, allowFound, searchRadiusChunks);
        var structure = StructureDepositLocation.getNearestStructure(sl, depKey, pos, allowFound, searchRadiusChunks);

        // Select between custom and structure deposit
        DepositLocation selected;
        if (custom == null) {
            selected = structure;
        } else if (structure == null) {
            selected = custom;
        } else if (structure.getLocation().distSqr(pos) < custom.getLocation().distSqr(pos)) {
            selected = structure;
        } else {
            selected = custom;
        }

        // Cache the result if possible
        if (sp != null) perPlayerCache.put(sp.getUUID(), new CachedData(selected, sl.getGameTime()));

        if (selected == null) {
            CreateRNS.LOGGER.debug("Could not find deposits nearby");
        } else {
            CreateRNS.LOGGER.debug("Found {} {} deposit at {}", selected.getTypeStr(), selected.getKey().location(),
                    selected.getLocationStr());
        }

        return selected;
    }

    protected static @Nullable DepositLocation getNearestCached(
            ServerPlayer sp, ResourceKey<Structure> depKey, int searchRadiusChunks
    ) {
        var sl = sp.serverLevel();
        var pos = sp.blockPosition();
        var uuid = sp.getUUID();
        var perPlayerCache = perLevelPerPlayerCache.get(sl);
        if (perPlayerCache == null) return null;
        var hit = perPlayerCache.getIfPresent(uuid);

        if (hit == null) {
            CreateRNS.LOGGER.trace("[Cache miss] Deposit position is not cached for player");
            return null;
        } else if (hit.loc == null) {
            perPlayerCache.invalidate(uuid);
            CreateRNS.LOGGER.trace("[Cache hit] Did not find any deposits nearby");
            return null;
        } else if (!hit.loc.getKey().equals(depKey)) {
            perPlayerCache.invalidate(uuid);
            CreateRNS.LOGGER.trace("[Cache hit] Cached deposit type does not match the requested type");
            return null;
        }
        var depLoc = hit.loc.getLocation();
        var chDist = new ChunkPos(depLoc).getChessboardDistance(new ChunkPos(pos));
        if (chDist > searchRadiusChunks) {
            perPlayerCache.invalidate(uuid);
            CreateRNS.LOGGER.trace("[Cache hit] Cached deposit is too far away ({} blocks)", (int) depLoc.distSqr(pos));
            return null;
        }

        // Log result
        CreateRNS.LOGGER.trace("[Cache hit] Found {} {} deposit at {}", hit.loc.getTypeStr(), hit.loc.getKey().location(),
                hit.loc.getLocationStr());

        return hit.loc;
    }

    public static DepositLocation of(ServerLevel sl, CompoundTag nbt) {
        if (nbt.contains("location")) return CustomDepositLocation.of(sl, nbt);
        else return StructureDepositLocation.of(sl, nbt);
    }

    protected ResourceKey<Structure> key;
    protected ChunkPos origin;

    public DepositLocation(ResourceKey<Structure> key, ChunkPos origin) {
        this.key = key;
        this.origin = origin;
    }

    public ResourceKey<Structure> getKey() {
        return key;
    }

    public ChunkPos getOrigin() {
        return origin;
    }

    public abstract BlockPos getLocation();

    public abstract @Nullable BlockPos getPreciseLocation(boolean computeIfUnknown);

    public abstract String getTypeStr();

    public abstract String getLocationStr();

    public boolean isFound(ServerLevel sl) {
        var depData = sl.getData(RNSMisc.LEVEL_DEPOSIT_DATA.get());
        return depData.foundDeposits.contains(this);
    }

    public boolean setFound(ServerLevel sl, boolean val) {
        var depData = sl.getData(RNSMisc.LEVEL_DEPOSIT_DATA.get());

        // Already done
        if (depData.foundDeposits.contains(this) == val) return false;

        if (val) depData.foundDeposits.add(this);
        else depData.foundDeposits.remove(this);

        return true;
    }

    public abstract CompoundTag serialize();

    protected record CachedData(@Nullable DepositLocation loc, long creationTimestamp) {}
}

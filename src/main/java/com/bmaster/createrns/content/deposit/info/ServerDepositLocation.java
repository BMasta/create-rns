package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSMisc;
import com.mojang.datafixers.util.Either;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class ServerDepositLocation extends DepositLocation {
    public static final int MIN_COMPUTE_INTERVAL = 90;

    /// Does not cache the result. Does not have to be called by a player.
    public static @Nullable ServerDepositLocation getNearest(
            ServerLevel sl, ResourceKey<Structure> depKey, BlockPos pos,
            boolean allowFound, int searchRadiusChunks
    ) {
        return getNearest(sl, Either.left(depKey), pos, allowFound, searchRadiusChunks);
    }

    public static @Nullable ServerDepositLocation getNearest(
            ServerLevel sl, TagKey<Structure> depTag, BlockPos pos,
            boolean allowFound, int searchRadiusChunks
    ) {
        return getNearest(sl, Either.right(depTag), pos, allowFound, searchRadiusChunks);
    }

    public static @Nullable ServerDepositLocation getNearest(
            ServerLevel sl, Either<ResourceKey<Structure>, TagKey<Structure>> depResOrTag, BlockPos pos,
            boolean allowFound, int searchRadiusChunks
    ) {
        return getNearest(sl, depResOrTag, pos, allowFound, searchRadiusChunks, null);
    }

    /// Caches the result per player
    public static @Nullable ServerDepositLocation getNearest(
            ServerPlayer sp, ResourceKey<Structure> depKey,
            boolean allowFound, int searchRadiusChunks, boolean cached
    ) {
        if (cached) {
            return getNearestCached(sp, depKey, searchRadiusChunks);
        } else {
            return getNearest(sp.serverLevel(), Either.left(depKey), sp.blockPosition(), allowFound, searchRadiusChunks, sp);
        }
    }

    public static @Nullable ServerDepositLocation getNearest(
            ServerPlayer sp, TagKey<Structure> depTag,
            boolean allowFound, int searchRadiusChunks
    ) {
        return getNearest(sp.serverLevel(), Either.right(depTag), sp.blockPosition(), allowFound, searchRadiusChunks, sp);
    }

    public static @Nullable ServerDepositLocation getNearest(
            ServerPlayer sp, Either<ResourceKey<Structure>, TagKey<Structure>> depResOrTag,
            boolean allowFound, int searchRadiusChunks
    ) {
        return getNearest(sp.serverLevel(), depResOrTag, sp.blockPosition(), allowFound, searchRadiusChunks, sp);
    }

    private static @Nullable ServerDepositLocation getNearest(
            ServerLevel sl, Either<ResourceKey<Structure>, TagKey<Structure>> depResOrTag, BlockPos pos,
            boolean allowFound, int searchRadiusChunks, @Nullable ServerPlayer sp
    ) {
        var depData = sl.getData(RNSMisc.LEVEL_DEPOSIT_DATA.get());
        if (sp != null) {
            var hit = depData.perPlayerCache.getIfPresent(sp.getUUID());

            // Okay, chill out buddy
            if (hit != null && sl.getGameTime() - hit.creationTimestamp < MIN_COMPUTE_INTERVAL) return null;
        }

        var custom = CustomServerDepositLocation.getNearestCustom(sl, depResOrTag, pos, allowFound, searchRadiusChunks);
        var structure = StructureServerDepositLocation.getNearestStructure(sl, depResOrTag, pos, allowFound, searchRadiusChunks);

        // Select between custom and structure deposit
        ServerDepositLocation selected;
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
        if (sp != null) depData.perPlayerCache.put(sp.getUUID(), new CachedData(selected, sl.getGameTime()));

        if (selected == null) {
            CreateRNS.LOGGER.debug("Could not find deposits nearby");
        } else {
            int dist = (int) Math.sqrt(selected.getLocation().distSqr(pos));
            CreateRNS.LOGGER.debug("Found {} {} deposit at {} ({} blocks away)", selected.getTypeStr(),
                    selected.getKey().location(), selected.getLocationStr(), dist);
        }

        if (selected != null) selected.computePreciseLocation();
        return selected;
    }

    private static @Nullable ServerDepositLocation getNearestCached(
            ServerPlayer sp, ResourceKey<Structure> depKey, int searchRadiusChunks
    ) {
        var sl = sp.serverLevel();
        var pos = sp.blockPosition();
        var uuid = sp.getUUID();
        var depData = sl.getData(RNSMisc.LEVEL_DEPOSIT_DATA.get());
        var hit = depData.perPlayerCache.getIfPresent(uuid);

        if (hit == null) {
            CreateRNS.LOGGER.trace("[Cache miss] Deposit position is not cached for player");
            return null;
        } else if (hit.loc == null) {
            depData.perPlayerCache.invalidate(uuid);
            CreateRNS.LOGGER.trace("[Cache hit] Did not find any deposits nearby");
            return null;
        } else if (!hit.loc.getKey().equals(depKey)) {
            depData.perPlayerCache.invalidate(uuid);
            CreateRNS.LOGGER.trace("[Cache hit] Cached deposit type does not match the requested type");
            return null;
        }
        var depLoc = hit.loc.getLocation();
        var chDist = new ChunkPos(depLoc).getChessboardDistance(new ChunkPos(pos));
        if (chDist > searchRadiusChunks) {
            depData.perPlayerCache.invalidate(uuid);
            CreateRNS.LOGGER.trace("[Cache hit] Cached deposit is too far away ({} blocks)", (int) depLoc.distSqr(pos));
            return null;
        }

        // Log result
        CreateRNS.LOGGER.trace("[Cache hit] Found {} {} deposit at {}", hit.loc.getTypeStr(), hit.loc.getKey().location(),
                hit.loc.getLocationStr());

        hit.loc.computePreciseLocation();
        return hit.loc;
    }

    public static ServerDepositLocation of(ServerLevel sl, CompoundTag nbt) {
        if (nbt.contains("location")) return CustomServerDepositLocation.of(sl, nbt);
        else return StructureServerDepositLocation.of(sl, nbt);
    }

    public ServerDepositLocation(ResourceKey<Structure> key, ChunkPos origin) {
        super(key, origin);
    }

    public abstract boolean computePreciseLocation();

    public abstract String getTypeStr();

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

    protected record CachedData(@Nullable ServerDepositLocation loc, long creationTimestamp) {}
}

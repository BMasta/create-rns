package com.bmaster.createrns.content.deposit.info;

import com.mojang.datafixers.util.Either;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CustomDepositLocation extends DepositLocation {
    public static @Nullable CustomDepositLocation getNearestCustom(
            ServerLevel sl, BlockPos pos,
            boolean allowFound, int searchRadiusChunks
    ) {
        var depIdx = IDepositIndex.get(sl);
        return getNearestCustom(sl, depIdx.customDeposits.values().stream().flatMap(Collection::stream),
                pos, allowFound, searchRadiusChunks);
    }

    public static @Nullable DepositLocation getNearestCustom(
            ServerLevel sl, Either<ResourceKey<Structure>, TagKey<Structure>> depResOrTag, BlockPos pos,
            boolean allowFound, int searchRadiusChunks
    ) {
        var res = depResOrTag.left().orElse(null);
        var tag = depResOrTag.right().orElse(null);
        assert res != null || tag != null;
        if (res != null) return getNearestCustom(sl, res, pos, allowFound, searchRadiusChunks);
        return getNearestCustom(sl, tag, pos, allowFound, searchRadiusChunks);
    }

    public static @Nullable CustomDepositLocation getNearestCustom(
            ServerLevel sl, ResourceKey<Structure> key, BlockPos pos,
            boolean allowFound, int searchRadiusChunks
    ) {
        var depIdx = IDepositIndex.get(sl);
        var depSet = depIdx.customDeposits.get(key.location());
        if (depSet == null) return null;

        return getNearestCustom(sl, depSet.stream(), pos, allowFound, searchRadiusChunks);
    }

    public static @Nullable DepositLocation getNearestCustom(
            ServerLevel sl, TagKey<Structure> depTag, BlockPos pos,
            boolean allowFound, int searchRadiusChunks
    ) {
        var named = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(depTag).orElse(null);
        if (named == null) return null;
        var depKeys = named.stream()
                .map(h -> h.unwrapKey().orElse(null))
                .filter(Objects::nonNull)
                .toList();

        double shortestDist = Float.MAX_VALUE;
        DepositLocation nearest = null;
        for (var k : depKeys) {
            var nearestOfType = getNearestCustom(sl, k, pos, allowFound, searchRadiusChunks);
            if (nearestOfType == null) continue;
            var dist = nearestOfType.getLocation().distSqr(pos);
            if (dist < shortestDist) {
                nearest = nearestOfType;
                shortestDist = dist;
            }
        }

        return nearest;
    }

    private static @Nullable CustomDepositLocation getNearestCustom(
            ServerLevel sl, Stream<CustomDepositLocation> locStream, BlockPos pos,
            boolean allowFound, int searchRadiusChunks
    ) {
        var result = locStream
                .filter(dp -> allowFound || !dp.isFound(sl))
                .min(Comparator.comparing(dp -> dp.getLocation().distSqr(pos)))
                .orElse(null);

        if (result == null) return null;
        var chDist = new ChunkPos(result.getLocation()).getChessboardDistance(new ChunkPos(pos));
        if (chDist > searchRadiusChunks) return null;

        return result;
    }

    public static CustomDepositLocation of(ServerLevel sl, CompoundTag nbt) {
        var key = ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse(nbt.getString("id")));
        var location = BlockPos.of(nbt.getLong("location"));
        return new CustomDepositLocation(key, location);
    }

    protected BlockPos location;

    public CustomDepositLocation(ResourceKey<Structure> key, BlockPos pos) {
        super(key, new ChunkPos(pos));
        location = pos;
    }

    @Override
    public BlockPos getLocation() {
        return location;
    }

    @Override
    public boolean computePreciseLocation() {
        return true;
    }

    @Override
    public String getTypeStr() {
        return "custom";
    }

    @Override
    public String getLocationStr() {
        return location.getX() + "," + location.getY() + "," + location.getZ();
    }

    @Override
    public CompoundTag serialize() {
        var root = new CompoundTag();
        root.putString("id", key.location().toString());
        root.putLong("location", location.asLong());
        return root;
    }
}

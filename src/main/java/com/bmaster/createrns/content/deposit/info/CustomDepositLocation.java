package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.RNSMisc;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CustomDepositLocation extends DepositLocation {
    public static @Nullable CustomDepositLocation getNearestCustom(
            ServerLevel sl, BlockPos pos,
            boolean allowFound, int searchRadiusChunks
    ) {
        var depData = sl.getData(RNSMisc.LEVEL_DEPOSIT_DATA.get());
        return getNearestCustom(sl, depData.customDeposits.values().stream().flatMap(Collection::stream),
                pos, allowFound, searchRadiusChunks);
    }

    public static @Nullable CustomDepositLocation getNearestCustom(
            ServerLevel sl, ResourceKey<Structure> key, BlockPos pos,
            boolean allowFound, int searchRadiusChunks
    ) {
        var depData = sl.getData(RNSMisc.LEVEL_DEPOSIT_DATA.get());
        var depSet = depData.customDeposits.get(key.location());
        if (depSet == null) return null;

        return getNearestCustom(sl, depSet.stream(), pos, allowFound, searchRadiusChunks);
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
    public @Nullable BlockPos getPreciseLocation(boolean computeIfUnknown) {
        return location;
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

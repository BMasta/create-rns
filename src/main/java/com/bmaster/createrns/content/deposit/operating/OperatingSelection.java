package com.bmaster.createrns.content.deposit.operating;

import com.bmaster.createrns.content.deposit.operating.space.OperatingSpaceAdapter.OperatingSpace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class OperatingSelection {
    public final boolean remote;
    public final OperatingSpace space;
    public final Set<BlockPos> positions;

    public static CompoundTag toNBT(OperatingSelection selection) {
        var result = new CompoundTag(4);
        result.putBoolean("remote", selection.remote);
        result.putString("space_dim", selection.space.dimension().location().toString());
        result.putString("space_id", selection.space.identity());
        result.putLongArray("positions", selection.positions.stream().mapToLong(BlockPos::asLong).toArray());

        return result;
    }

    public static @Nullable OperatingSelection fromNBT(CompoundTag tag) {
        if (!tag.contains("remote") || !tag.contains("space_dim") || !tag.contains("space_id") ||
                !tag.contains("positions")) {
            return null;
        }
        var dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("space_dim")));
        var positions = Arrays.stream(tag.getLongArray("positions"))
                .mapToObj(BlockPos::of)
                .collect(Collectors.toSet());
        return new OperatingSelection(tag.getBoolean("remote"),
                new OperatingSpace(dimKey, tag.getString("space_id")), positions);
    }

    public OperatingSelection(boolean remote, OperatingSpace space, Set<BlockPos> positions) {
        this.remote = remote;
        this.space = space;
        this.positions = Set.copyOf(positions);
    }
}

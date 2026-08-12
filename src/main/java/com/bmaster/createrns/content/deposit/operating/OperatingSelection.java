package com.bmaster.createrns.content.deposit.operating;

import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapter.OperatingSublevel;
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
    public final boolean crossSublevel;
    public final OperatingSublevel sublevel;
    public final Set<BlockPos> positions;

    public static CompoundTag toNBT(OperatingSelection selection) {
        var result = new CompoundTag(4);
        result.putString("dimension", selection.sublevel.dimension().location().toString());
        result.putBoolean("cross_sublevel", selection.crossSublevel);
        result.putString("sublevel_id", selection.sublevel.identity());
        result.putLongArray("positions", selection.positions.stream().mapToLong(BlockPos::asLong).toArray());

        return result;
    }

    public static @Nullable OperatingSelection fromNBT(CompoundTag tag) {
        if (!tag.contains("dimension") || !tag.contains("cross_sublevel") || !tag.contains("sublevel_id") ||
                !tag.contains("positions")) {
            return null;
        }
        var dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("dimension")));
        var positions = Arrays.stream(tag.getLongArray("positions"))
                .mapToObj(BlockPos::of)
                .collect(Collectors.toSet());
        return new OperatingSelection(tag.getBoolean("cross_sublevel"),
                new OperatingSublevel(dimKey, tag.getString("sublevel_id")), positions);
    }

    public OperatingSelection(boolean crossSublevel, OperatingSublevel sublevel, Set<BlockPos> positions) {
        this.crossSublevel = crossSublevel;
        this.sublevel = sublevel;
        this.positions = Set.copyOf(positions);
    }
}

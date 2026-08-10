package com.bmaster.createrns.content.deposit.operating;

import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.util.Utils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IDepositBlockOperator {
    BlockPos getBlockPos();

    @Nullable Level getLevel();

    @Nullable BlockPos getAnchor();

    @Nullable OperatingDimensions getOperatingDimensions();

    Direction getOperatingDirection();

    default @Nullable BoundingBox getOperatingBoundingBox() {
        var spec = getOperatingDimensions();
        if (spec == null) return null;
        var anchor = getAnchor();
        if (anchor == null) return null;
        var dir = getOperatingDirection();
        Vec3i pos = new Vec3i(anchor.getX(), anchor.getY(), anchor.getZ());

        var minOffset = dir.getNormal().multiply(
                dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : spec.length);
        var maxOffset = dir.getNormal().multiply(
                dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 1 : spec.length);

        var minRadiusDelta = Utils.normalVecFlip(dir, false).multiply(spec.radius);
        var maxRadiusDelta = Utils.normalVecFlip(dir, true).multiply(spec.radius);

        var minPos = pos.offset(minOffset).offset(minRadiusDelta);
        var maxPos = pos.offset(maxOffset).offset(maxRadiusDelta);

        return new BoundingBox(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX(), maxPos.getY(), maxPos.getZ());
    }

    default Set<BlockPos> getConfinedDepositVein() {
        var level = getLevel();
        if (level == null) return Set.of();
        var spec = getOperatingDimensions();
        if (spec == null) return Set.of();
        var anchor = getAnchor();
        if (anchor == null) return Set.of();
        var ma = getOperatingBoundingBox();
        if (ma == null) return Set.of();
        var dir = getOperatingDirection();

        Queue<BlockPos> q = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet(ma.getXSpan() * ma.getYSpan() * ma.getZSpan());

        q.offer(anchor.relative(dir));
        while (!q.isEmpty()) {
            var bp = q.poll();
            if (visited.contains(bp.asLong()) || !ma.isInside(bp) ||
                    !level.getBlockState(bp).is(RNSTags.RNSBlockTags.DEPOSIT_BLOCKS)) {
                continue;
            }
            visited.add(bp.asLong());

            Direction.stream().forEach(d -> q.add(bp.relative(d)));
        }
        return visited.longStream().mapToObj(BlockPos::of).collect(Collectors.toSet());
    }

    record OperatingDimensions(int radius, int length) {}
}

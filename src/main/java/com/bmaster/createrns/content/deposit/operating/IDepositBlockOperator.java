package com.bmaster.createrns.content.deposit.operating;

import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.util.Utils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Queue;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IDepositBlockOperator {
    int MAX_VEIN_SIZE = 4096;

    static LinkedHashSet<BlockPos> getConfinedDepositVein(Level level, BlockPos start, BoundingBox area) {
        Queue<BlockPos> q = new ArrayDeque<>();
        var visited = new LinkedHashSet<BlockPos>();

        q.offer(start);
        while (!q.isEmpty() && visited.size() < MAX_VEIN_SIZE) {
            var bp = q.poll();
            if (visited.contains(bp) || (area != null && !area.isInside(bp)) ||
                    !level.getBlockState(bp).is(RNSTags.RNSBlockTags.DEPOSIT_BLOCKS)) {
                continue;
            }
            visited.add(bp);

            Direction.stream().forEach(d -> q.add(bp.relative(d)));
        }
        return visited;
    }

    BlockPos getBlockPos();

    @Nullable Level getLevel();

    @Nullable BlockPos getOperatingStart();

    @Nullable OperatingDimensions getOperatingDimensions();

    Direction getOperatingDirection();

    default @Nullable BoundingBox getOperatingBoundingBox() {
        var start = getOperatingStart();
        var dims = getOperatingDimensions();
        if (start == null || dims == null) return null;

        var dir = getOperatingDirection();
        Vec3i pos = new Vec3i(start.getX(), start.getY(), start.getZ());

        var minOffset = dir.getNormal().multiply(
                dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0 : dims.length - 1);
        var maxOffset = dir.getNormal().multiply(
                dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 0 : dims.length - 1);

        var minRadiusDelta = Utils.normalVecFlip(dir, false).multiply(dims.radius);
        var maxRadiusDelta = Utils.normalVecFlip(dir, true).multiply(dims.radius);

        var minPos = pos.offset(minOffset).offset(minRadiusDelta);
        var maxPos = pos.offset(maxOffset).offset(maxRadiusDelta);

        return new BoundingBox(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX(), maxPos.getY(), maxPos.getZ());
    }

    record OperatingDimensions(int radius, int length) {}
}

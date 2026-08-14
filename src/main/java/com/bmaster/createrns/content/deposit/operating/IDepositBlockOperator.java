package com.bmaster.createrns.content.deposit.operating;

import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapter.OperatingSublevel;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapterHolder;
import com.bmaster.createrns.util.Utils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
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
        while (!q.isEmpty() && visited.size() <= MAX_VEIN_SIZE) {
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

    static AABB createCrossSublevelDetectionArea(
            BlockPos contact, Direction operatingDirection, DetectionDimensions detectionDimensions
    ) {
        var center = contact.getCenter().relative(operatingDirection, detectionDimensions.detectionOffset());
        var halfLength = detectionDimensions.detectionLength() / 2;
        var xRadius = operatingDirection.getAxis() == Direction.Axis.X ? halfLength : detectionDimensions.detectionRadius();
        var yRadius = operatingDirection.getAxis() == Direction.Axis.Y ? halfLength : detectionDimensions.detectionRadius();
        var zRadius = operatingDirection.getAxis() == Direction.Axis.Z ? halfLength : detectionDimensions.detectionRadius();
        return new AABB(
                center.x - xRadius,
                center.y - yRadius,
                center.z - zRadius,
                center.x + xRadius,
                center.y + yRadius,
                center.z + zRadius
        );
    }

    BlockPos getBlockPos();

    @Nullable Level getLevel();

    @Nullable OperatingSublevel getSublevel();

    boolean isOperatingCrossSublevel();

    /// The position from which the operator projects operating area and detection area
    @Nullable BlockPos getOperatingContact();

    /// The root position in the operating area that touches the contact.
    /// For cross-sublevel operations, contact and start will be in different sublevels.
    @Nullable BlockPos getOperatingStart();

    @Nullable OperatingDimensions getOperatingDimensions();

    default @Nullable DetectionDimensions getDetectionDimensions() {
        return null;
    }

    Direction getOperatingDirection();

    default Direction getLogicalOperatingDirection() {
        var level = getLevel();
        var contact = getOperatingContact();
        var direction = getOperatingDirection();
        if (level == null || contact == null) return direction;
        return OperatingSublevelAdapterHolder.getAdapter().getLogicalDirection(level, contact, direction);
    }

    default @Nullable BoundingBox getOperatingBoundingBox() {
        var start = getOperatingStart();
        var dims = getOperatingDimensions();
        if (start == null || dims == null) return null;

        var dir = getLogicalOperatingDirection();
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

    record DetectionDimensions( double detectionRadius, double detectionLength, double detectionOffset) {}
}

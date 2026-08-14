package com.bmaster.createrns.compat.sable;

import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator.DetectionDimensions;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapter;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class SableOperatingSublevelAdapter implements OperatingSublevelAdapter {
    public static OperatingSublevelAdapter create() {
        return new SableOperatingSublevelAdapter();
    }

    private SableOperatingSublevelAdapter() {}

    @Override
    public OperatingSublevel getOperatingSublevel(Level level, BlockPos pos) {
        var sublevel = SableCompanion.INSTANCE.getContaining(level, pos);
        var identity = sublevel == null ? OperatingSublevel.MAIN_ID : sublevel.getUniqueId().toString();
        return new OperatingSublevel(level.dimension(), identity);
    }

    @Override
    public SidedOperatingSublevel getSidedOperatingSublevel(Level level, BlockPos pos) {
        var sublevel = SableCompanion.INSTANCE.getContaining(level, pos);
        var identity = sublevel == null ? OperatingSublevel.MAIN_ID : sublevel.getUniqueId().toString();
        return new SidedOperatingSublevel(level.dimension(), identity, level.isClientSide);
    }

    @Override
    public Direction getLogicalDirection(Level level, BlockPos pos, BlockPos relativeTo, Direction direction) {
        var relativeNormal = Vec3.atLowerCornerOf(direction.getNormal());

        var sourceSublevel = SableCompanion.INSTANCE.getContaining(level, pos);
        if (sourceSublevel != null) relativeNormal = sourceSublevel.logicalPose().transformNormal(relativeNormal);

        var targetSublevel = SableCompanion.INSTANCE.getContaining(level, relativeTo);
        if (targetSublevel != null) relativeNormal = targetSublevel.logicalPose().transformNormalInverse(relativeNormal);

        return Direction.getNearest(relativeNormal);
    }

    @Override
    public double distManhattan(Level level, BlockPos firstPos, BlockPos secondPos) {
        return SableCompanion.INSTANCE.rectilinearDistanceWithSubLevels(
                level, firstPos.getCenter(), secondPos.getCenter());
    }

    @Override
    public double distSqr(Level level, BlockPos firstPos, BlockPos secondPos) {
        return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, firstPos.getCenter(), secondPos.getCenter());
    }

    @Override
    public CrossSublevelDepositBlocks getCrossSublevelDepositBlocks(
            Level level, OperatingSublevel operatorSublevel, BlockPos contact,
            Direction operatingDirection, DetectionDimensions operatingDimensions
    ) {
        var blocks = new ObjectOpenHashSet<BlockPos>();
        @Nullable BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;

        var operatorSableSublevel = SableCompanion.INSTANCE.getContaining(level, contact);
        var logicalContact = contact.getCenter();
        if (operatorSableSublevel != null) {
            logicalContact = operatorSableSublevel.logicalPose().transformPosition(logicalContact);
        }

        var operatingBounds = new BoundingBox3d(IDepositBlockOperator.createCrossSublevelDetectionArea(
                contact, operatingDirection, operatingDimensions));
        if (operatorSableSublevel != null) operatingBounds.transform(operatorSableSublevel.logicalPose());

        if (!operatorSublevel.identity().equals(OperatingSublevel.MAIN_ID)) {
            var detectedBlocks = findDepositBlocks(level,
                    new OperatingSublevel(level.dimension(), OperatingSublevel.MAIN_ID),
                    new BoundingBox3i(operatingBounds), logicalContact);
            blocks.addAll(detectedBlocks.blocks());
            closest = detectedBlocks.closest();
            if (closest != null) {
                closestDistance = SableCompanion.INSTANCE.distanceSquaredWithSubLevels(
                        level, contact.getCenter(), closest.getCenter());
            }
        }

        for (var targetSubLevel : SableCompanion.INSTANCE.getAllIntersecting(level, operatingBounds)) {
            var targetSublevel = new OperatingSublevel(level.dimension(), targetSubLevel.getUniqueId().toString());
            if (targetSublevel.equals(operatorSublevel)) continue;

            var targetBounds = new BoundingBox3d(operatingBounds).transformInverse(targetSubLevel.logicalPose());
            var targetContact = targetSubLevel.logicalPose().transformPositionInverse(logicalContact);
            var detectedBlocks = findDepositBlocks(
                    level, targetSublevel, new BoundingBox3i(targetBounds), targetContact);
            blocks.addAll(detectedBlocks.blocks());

            var candidate = detectedBlocks.closest();
            if (candidate == null) continue;
            double candidateDistance = SableCompanion.INSTANCE.distanceSquaredWithSubLevels(
                    level, contact.getCenter(), candidate.getCenter());
            if (candidateDistance >= closestDistance) continue;

            closest = candidate;
            closestDistance = candidateDistance;
        }

        return new CrossSublevelDepositBlocks(closest, blocks);
    }

    private static CrossSublevelDepositBlocks findDepositBlocks(
            Level level, OperatingSublevel sublevel, BoundingBox3i bounds, Vec3 contact
    ) {
        var positions = new ObjectOpenHashSet<BlockPos>();
        @Nullable BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;
        var mutablePos = new BlockPos.MutableBlockPos();

        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    mutablePos.set(x, y, z);
                    if (!level.isLoaded(mutablePos)) continue;

                    var containingSublevel = SableCompanion.INSTANCE.getContaining(level, mutablePos);
                    var containingIdentity = containingSublevel == null ? OperatingSublevel.MAIN_ID :
                            containingSublevel.getUniqueId().toString();
                    if (!sublevel.identity().equals(containingIdentity) ||
                            !level.getBlockState(mutablePos).is(RNSTags.RNSBlockTags.DEPOSIT_BLOCKS)) continue;

                    var position = mutablePos.immutable();
                    positions.add(position);

                    double xDistance = position.getX() + 0.5 - contact.x;
                    double yDistance = position.getY() + 0.5 - contact.y;
                    double zDistance = position.getZ() + 0.5 - contact.z;
                    double distance = xDistance * xDistance + yDistance * yDistance + zDistance * zDistance;
                    if (distance >= closestDistance) continue;

                    closest = position;
                    closestDistance = distance;
                }
            }
        }

        return new CrossSublevelDepositBlocks(closest, positions);
    }

}

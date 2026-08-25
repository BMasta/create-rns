package com.bmaster.createrns.compat.sable;

import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator.DetectionDimensions;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapter;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalDouble;
import java.util.function.Predicate;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class SableOperatingSublevelAdapter implements OperatingSublevelAdapter {
    private static final double INTERSECTION_EPSILON = 1e-7;
    private static final Vec3[] DETECTION_AXES = {
            new Vec3(1, 0, 0),
            new Vec3(0, 1, 0),
            new Vec3(0, 0, 1)
    };

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
    public OptionalDouble getCrossSublevelBlockHitDistance(
            Level level, BlockPos contact, Direction operatingDirection, BlockPos target, double rayLength
    ) {
        var sourceSublevel = SableCompanion.INSTANCE.getContaining(level, contact);
        var logicalContact = contact.getCenter();
        var logicalDirection = Vec3.atLowerCornerOf(operatingDirection.getNormal());
        if (sourceSublevel != null) {
            logicalContact = sourceSublevel.logicalPose().transformPosition(logicalContact);
            logicalDirection = sourceSublevel.logicalPose().transformNormal(logicalDirection);
        }
        logicalDirection = logicalDirection.normalize();

        var targetSublevel = SableCompanion.INSTANCE.getContaining(level, target);
        var targetContact = logicalContact;
        var targetDirection = logicalDirection;
        if (targetSublevel != null) {
            targetContact = targetSublevel.logicalPose().transformPositionInverse(targetContact);
            targetDirection = targetSublevel.logicalPose().transformNormalInverse(targetDirection).normalize();
        }

        var clipContext = new ClipContext(
                targetContact,
                targetContact.add(targetDirection.scale(rayLength)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        );
        ((ClipContextExtension) clipContext).sable$setDoNotProject(true);

        var hit = level.clip(clipContext);
        if (hit.getType() == HitResult.Type.MISS) return OptionalDouble.empty();

        var logicalHit = hit.getLocation();
        if (targetSublevel != null) logicalHit = targetSublevel.logicalPose().transformPosition(logicalHit);
        return OptionalDouble.of(logicalContact.distanceTo(logicalHit));
    }

    @Override
    public CrossSublevelDepositBlocks getCrossSublevelDepositBlocks(
            Level level, OperatingSublevel operatorSublevel, BlockPos contact,
            Direction operatingDirection, DetectionDimensions operatingDimensions
    ) {
        var blocks = new ObjectOpenHashSet<BlockPos>();
        @Nullable BlockPos closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;

        var operatorSableSublevel = SableCompanion.INSTANCE.getContaining(level, contact);
        var operatorPose = operatorSableSublevel == null ? null : operatorSableSublevel.logicalPose();
        var logicalContact = contact.getCenter();
        if (operatorPose != null) logicalContact = operatorPose.transformPosition(logicalContact);

        var detectionArea = IDepositBlockOperator.createCrossSublevelDetectionArea(
                contact, operatingDirection, operatingDimensions);
        var operatingBounds = new BoundingBox3d(detectionArea);
        if (operatorPose != null) operatingBounds.transform(operatorPose);

        if (!operatorSublevel.identity().equals(OperatingSublevel.MAIN_ID)) {
            var detectedBlocks = findDepositBlocks(
                    level, null, new BoundingBox3i(operatingBounds), logicalContact,
                    position -> intersectsDetectionArea(detectionArea, position, operatorPose, null));
            blocks.addAll(detectedBlocks.blocks());
            closest = detectedBlocks.closest();
            if (closest != null) {
                closestDistance = SableCompanion.INSTANCE.distanceSquaredWithSubLevels(
                        level, contact.getCenter(), closest.getCenter());
            }
        }

        for (var targetSableSublevel : SableCompanion.INSTANCE.getAllIntersecting(level, operatingBounds)) {
            if (targetSableSublevel == operatorSableSublevel) continue;

            var targetPose = targetSableSublevel.logicalPose();
            var targetBounds = new BoundingBox3d(operatingBounds).transformInverse(targetPose);
            var targetContact = targetPose.transformPositionInverse(logicalContact);
            var detectedBlocks = findDepositBlocks(
                    level, targetSableSublevel, new BoundingBox3i(targetBounds), targetContact,
                    position -> intersectsDetectionArea(detectionArea, position, operatorPose, targetPose));
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

    static boolean intersectsDetectionArea(
            AABB detectionArea, BlockPos block, @Nullable Pose3dc operatorPose, @Nullable Pose3dc targetPose
    ) {
        // Composed sublevel poses can turn the block cube into a general parallelepiped in detection space.
        var blockOrigin = Vec3.atLowerCornerOf(block);
        var transformedOrigin = transformToDetectionSpace(blockOrigin, operatorPose, targetPose);
        var halfEdges = new Vec3[] {
                transformToDetectionSpace(blockOrigin.add(1, 0, 0), operatorPose, targetPose)
                        .subtract(transformedOrigin).scale(0.5),
                transformToDetectionSpace(blockOrigin.add(0, 1, 0), operatorPose, targetPose)
                        .subtract(transformedOrigin).scale(0.5),
                transformToDetectionSpace(blockOrigin.add(0, 0, 1), operatorPose, targetPose)
                        .subtract(transformedOrigin).scale(0.5)
        };

        var blockCenter = transformedOrigin.add(halfEdges[0]).add(halfEdges[1]).add(halfEdges[2]);
        var centerOffset = blockCenter.subtract(detectionArea.getCenter());
        var detectionHalfExtents = new Vec3(
                detectionArea.getXsize() / 2,
                detectionArea.getYsize() / 2,
                detectionArea.getZsize() / 2
        );

        for (var detectionAxis : DETECTION_AXES) {
            if (isSeparatingAxis(detectionAxis, centerOffset, detectionHalfExtents, halfEdges)) return false;
        }

        var blockFaceNormals = new Vec3[] {
                halfEdges[1].cross(halfEdges[2]),
                halfEdges[2].cross(halfEdges[0]),
                halfEdges[0].cross(halfEdges[1])
        };
        for (var blockFaceNormal : blockFaceNormals) {
            if (isSeparatingAxis(blockFaceNormal, centerOffset, detectionHalfExtents, halfEdges)) return false;
        }

        for (var detectionAxis : DETECTION_AXES) {
            for (var halfEdge : halfEdges) {
                var edgeCrossAxis = detectionAxis.cross(halfEdge);
                if (isSeparatingAxis(edgeCrossAxis, centerOffset, detectionHalfExtents, halfEdges)) return false;
            }
        }

        return true;
    }

    private static Vec3 transformToDetectionSpace(
            Vec3 position, @Nullable Pose3dc operatorPose, @Nullable Pose3dc targetPose
    ) {
        if (targetPose != null) position = targetPose.transformPosition(position);
        if (operatorPose != null) position = operatorPose.transformPositionInverse(position);
        return position;
    }

    private static boolean isSeparatingAxis(
            Vec3 axis, Vec3 centerOffset, Vec3 detectionHalfExtents, Vec3[] blockHalfEdges
    ) {
        if (axis.lengthSqr() <= INTERSECTION_EPSILON * INTERSECTION_EPSILON) return false;

        double detectionRadius = Math.abs(axis.x) * detectionHalfExtents.x +
                Math.abs(axis.y) * detectionHalfExtents.y + Math.abs(axis.z) * detectionHalfExtents.z;
        double blockRadius = Math.abs(axis.dot(blockHalfEdges[0])) + Math.abs(axis.dot(blockHalfEdges[1])) +
                Math.abs(axis.dot(blockHalfEdges[2]));
        double tolerance = INTERSECTION_EPSILON * (Math.abs(axis.x) + Math.abs(axis.y) + Math.abs(axis.z));
        return Math.abs(axis.dot(centerOffset)) > detectionRadius + blockRadius + tolerance;
    }

    private static CrossSublevelDepositBlocks findDepositBlocks(
            Level level, @Nullable SubLevelAccess sublevel, BoundingBox3i bounds, Vec3 contact,
            Predicate<BlockPos> detectionAreaFilter
    ) {
        int minY = Math.max(bounds.minY(), level.getMinBuildHeight());
        int maxY = Math.min(bounds.maxY(), level.getMaxBuildHeight() - 1);
        if (minY > maxY) return CrossSublevelDepositBlocks.EMPTY;

        var positions = new ObjectOpenHashSet<BlockPos>();
        @Nullable BlockPos closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        var mutablePos = new BlockPos.MutableBlockPos();
        var pose = sublevel == null ? null : sublevel.logicalPose();
        var scale = pose == null ? null : pose.scale();
        double xScaleSqr = scale == null ? 1 : scale.x() * scale.x();
        double yScaleSqr = scale == null ? 1 : scale.y() * scale.y();
        double zScaleSqr = scale == null ? 1 : scale.z() * scale.z();

        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                mutablePos.set(x, minY, z);
                if (!level.isLoaded(mutablePos)) continue;

                if (SableCompanion.INSTANCE.getContaining(level, mutablePos) != sublevel) continue;

                for (int y = minY; y <= maxY; y++) {
                    mutablePos.set(x, y, z);
                    if (!level.getBlockState(mutablePos).is(RNSTags.RNSBlockTags.DEPOSIT_BLOCKS)) continue;

                    var position = mutablePos.immutable();
                    if (!detectionAreaFilter.test(position)) continue;
                    positions.add(position);

                    double xDistance = position.getX() + 0.5 - contact.x;
                    double yDistance = position.getY() + 0.5 - contact.y;
                    double zDistance = position.getZ() + 0.5 - contact.z;
                    double distance = xDistance * xDistance * xScaleSqr + yDistance * yDistance * yScaleSqr +
                            zDistance * zDistance * zScaleSqr;
                    if (distance >= closestDistance) continue;

                    closest = position;
                    closestDistance = distance;
                }
            }
        }

        return new CrossSublevelDepositBlocks(closest, positions);
    }

}

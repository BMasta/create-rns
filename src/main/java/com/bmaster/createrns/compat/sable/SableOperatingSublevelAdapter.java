package com.bmaster.createrns.compat.sable;

import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator.CrossSublevelOperatingDimensions;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapter;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

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
    public double distManhattan(Level level, BlockPos firstPos, BlockPos secondPos) {
        return SableCompanion.INSTANCE.rectilinearDistanceWithSubLevels(
                level, firstPos.getCenter(), secondPos.getCenter());
    }

    @Override
    public double distSqr(Level level, BlockPos firstPos, BlockPos secondPos) {
        return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, firstPos.getCenter(), secondPos.getCenter());
    }

    @Override
    public Set<BlockPos> getCrossSublevelDepositBlocks(
            Level level, OperatingSublevel operatorSublevel, BlockPos anchor,
            Direction operatingDirection, CrossSublevelOperatingDimensions operatingDimensions
    ) {
        var blocks = new ObjectOpenHashSet<BlockPos>();

        var operatorSableSublevel = SableCompanion.INSTANCE.getContaining(level, anchor);
        var operatingBounds = new BoundingBox3d(IDepositBlockOperator.createCrossSublevelOperatingArea(
                anchor, operatingDirection, operatingDimensions));
        if (operatorSableSublevel != null) operatingBounds.transform(operatorSableSublevel.logicalPose());

        if (!operatorSublevel.identity().equals(OperatingSublevel.MAIN_ID)) {
            blocks.addAll(findDepositBlocks(level, new OperatingSublevel(level.dimension(), OperatingSublevel.MAIN_ID),
                    new BoundingBox3i(operatingBounds)));
        }

        for (var targetSubLevel : SableCompanion.INSTANCE.getAllIntersecting(level, operatingBounds)) {
            var targetSublevel = new OperatingSublevel(level.dimension(), targetSubLevel.getUniqueId().toString());
            if (targetSublevel.equals(operatorSublevel)) continue;

            var targetBounds = new BoundingBox3d(operatingBounds).transformInverse(targetSubLevel.logicalPose());
            blocks.addAll(findDepositBlocks(level, targetSublevel, new BoundingBox3i(targetBounds)));
        }

        return blocks;
    }

    private static Set<BlockPos> findDepositBlocks(Level level, OperatingSublevel sublevel, BoundingBox3i bounds) {
        var positions = new ObjectOpenHashSet<BlockPos>();
        var mutablePos = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    mutablePos.set(x, y, z);
                    var containingSubLevel = SableCompanion.INSTANCE.getContaining(level, mutablePos);
                    var containingIdentity = containingSubLevel == null ? OperatingSublevel.MAIN_ID :
                            containingSubLevel.getUniqueId().toString();
                    if (!sublevel.identity().equals(containingIdentity) || !level.isLoaded(mutablePos) ||
                            !level.getBlockState(mutablePos).is(RNSTags.RNSBlockTags.DEPOSIT_BLOCKS)) {
                        continue;
                    }
                    positions.add(mutablePos.immutable());
                }
            }
        }
        return positions;
    }
}

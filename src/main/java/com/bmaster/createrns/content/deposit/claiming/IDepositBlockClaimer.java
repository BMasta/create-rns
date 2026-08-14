package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IDepositBlockClaimer extends IDepositBlockOperator {
    /// All claimers whose area intersects the provided area will reclaim their blocks

    static void reclaimArea(BoundingBox area, IDepositBlockClaimer except) {
        var claimers = DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(area, except);
        for (var c : claimers) {
            c.claimDepositBlocks();
            c.claimCrossSublevelDepositBlocks();
        }
        CreateRNS.LOGGER.trace("Deposit block states changed. Updated {} nearby claimers.", claimers.size());
    }

    static void reclaimBlock(Level level, BlockPos pos) {
        var claimers = DepositClaimerInstanceHolder.getInstancesThatCanClaim(level, pos);
        for (var c : claimers) {
            c.claimDepositBlocks();
            c.claimCrossSublevelDepositBlocks();
        }
        CreateRNS.LOGGER.trace("Deposit block state changed. Updated {} nearby claimers.", claimers.size());
    }

    @Nullable Set<BlockPos> getClaimedDepositBlocks();

    void setClaimedDepositBlocks(@Nullable Set<BlockPos> positions, boolean crossSublevel);

    void claimDepositBlocks();

    void claimCrossSublevelDepositBlocks();

    default Set<BlockPos> getClaimableDepositVein() {
        var level = getLevel();
        var area = getOperatingBoundingBox();
        var start = getOperatingStart();
        if (level == null || area == null || start == null) return Set.of();
        var vein = IDepositBlockOperator.getConfinedDepositVein(level, start, area);
        if(vein.isEmpty()) return vein;

        // Remove blocks claimed by other claimers of the same type
        for (var c : DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(area, this)) {
            var claimedBlocks = c.getClaimedDepositBlocks();
            if (claimedBlocks != null) vein.removeAll(claimedBlocks);
        }
        return vein;
    }
}

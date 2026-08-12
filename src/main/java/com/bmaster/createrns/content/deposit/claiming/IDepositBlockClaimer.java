package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IDepositBlockClaimer extends IDepositBlockOperator {
    /// All claimers whose area intersects the provided area will reclaim their blocks

    static void reclaimArea(Level level, BoundingBox area, @Nullable IDepositBlockClaimer except) {
        var claimers = DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(level, area);
        for (var c : claimers) {
            if (except != null && except.equals(c)) continue;
            c.claimDepositBlocks();
        }
    }

    static void reclaimArea(Level level, BoundingBox area) {
        reclaimArea(level, area, null);
    }

    @Nullable ClaimedDepositBlocks getClaimedDepositBlocks();

    void setClaimedDepositBlocks(@Nullable ClaimedDepositBlocks claimedBlocks);

    void claimDepositBlocks();

    default Set<BlockPos> getClaimableDepositVein(Level level) {
        var vein = getConfinedDepositVein();
        // Remove blocks claimed by other claimers of the same type
        for (var c : DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(this, level)) {
            var claimedBlocks = c.getClaimedDepositBlocks();
            if (claimedBlocks != null) vein.removeAll(claimedBlocks.positions());
        }
        return vein;
    }

    record ClaimedDepositBlocks(Set<BlockPos> positions, boolean crossSublevel) {
        public static final ClaimedDepositBlocks NONE = new  ClaimedDepositBlocks(Set.of(), false);

        public ClaimedDepositBlocks {
            positions = Set.copyOf(positions);
        }
    }
}

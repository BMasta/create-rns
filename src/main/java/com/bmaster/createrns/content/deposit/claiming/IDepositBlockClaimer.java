package com.bmaster.createrns.content.deposit.claiming;

import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IDepositBlockClaimer extends IDepositBlockOperator {
    /// All claimers whose area intersects the provided area will reclaim their blocks
    static void reclaimArea(Level level, BoundingBox area) {
        var claimers = DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(level, area);
        for (var c : claimers) {
            c.claimDepositBlocks();
        }
    }

    @Nullable Set<BlockPos> getClaimedDepositBlocks();

    void setClaimedDepositBlocks(@Nullable Set<BlockPos> claimedBlocks);

    void claimDepositBlocks();

    default Set<BlockPos> getClaimableDepositVein(Level level) {
        var vein = getConfinedDepositVein();
        // Remove blocks claimed by other claimers of the same type
        for (var c : DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(this, level)) {
            var claimedBlocks = c.getClaimedDepositBlocks();
            if (claimedBlocks != null) vein.removeAll(claimedBlocks);
        }
        return vein;
    }

    default CompoundTag serializeDepositBlockClaimer(Provider provider) {
        var root = new CompoundTag();

        var list = new ListTag();
        var claimedBlocks = getClaimedDepositBlocks();
        if (claimedBlocks != null) {
            for (var bp : claimedBlocks) {
                list.add(LongTag.valueOf(bp.asLong()));
            }
            root.put("claimed_blocks", list);
        }
        return root;
    }

    default void deserializeDepositBlockClaimer(Provider provider, CompoundTag nbt) {
        var alreadyClaimedBlocks = getClaimedDepositBlocks();
        Set<BlockPos> newlyClaimedBlocks = null;

        // If list exists (even if empty), the claimer has finished claiming
        if (nbt.get("claimed_blocks") instanceof ListTag list) {
            newlyClaimedBlocks = new HashSet<BlockPos>(nbt.size());
            for (var t : list) {
                if (!(t instanceof LongTag lt)) continue;
                newlyClaimedBlocks.add(BlockPos.of(lt.getAsLong()));
            }
            if (alreadyClaimedBlocks != null && alreadyClaimedBlocks.equals(newlyClaimedBlocks)) return;
        } else if (alreadyClaimedBlocks == null) {
            return;
        }

        var level = getLevel();
        boolean updateOutline = level != null && level.isClientSide;

        if (updateOutline) DepositClaimerOutlineRenderer.removeClaimer(this);
        setClaimedDepositBlocks(newlyClaimedBlocks);
        if (updateOutline) DepositClaimerOutlineRenderer.addClaimer(this);
    }
}

package com.bmaster.createrns.mining;

import com.bmaster.createrns.deposit.DepositClaimerInstanceHolder;
import com.bmaster.createrns.deposit.IDepositBlockClaimer;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

public abstract class MiningBlock extends KineticBlock {
    public MiningBlock(Properties props) {
        super(props);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || oldState.is(state.getBlock()) ||
                !(level.getBlockEntity(pos) instanceof MiningBlockEntity be)) return;
        be.getBehaviour(MiningBehaviour.TYPE).claimDepositBlocks();
    }

    @ParametersAreNonnullByDefault
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        Set<IDepositBlockClaimer> nearbyClaimers = null;
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MiningBlockEntity minerBE) {
                // Pop inventory contents on the ground
                be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
                    for (int i = 0; i < iItemHandler.getSlots(); ++i) {
                        ItemStack stack = iItemHandler.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            Block.popResource(level, pos, stack);
                        }
                    }
                });

                // Collect all nearby deposit claimers
                if (!level.isClientSide) {
                    nearbyClaimers = DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(
                            minerBE.getBehaviour(MiningBehaviour.TYPE), level);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);

        if (nearbyClaimers != null) {
            // Now that our own claimer is removed, let others re-reserve their deposit blocks
            for (var c : nearbyClaimers) {
                c.claimDepositBlocks();
            }
        }
    }
}

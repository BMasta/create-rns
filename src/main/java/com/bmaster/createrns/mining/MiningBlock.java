//package com.bmaster.createrns.mining;
//
//import com.bmaster.createrns.RNSContent;
//import com.bmaster.createrns.mining.MiningBlockEntity;
//import com.simibubi.create.content.kinetics.base.KineticBlock;
//import com.simibubi.create.foundation.block.IBE;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.LevelReader;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.entity.BlockEntityType;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraftforge.common.capabilities.ForgeCapabilities;
//import org.jetbrains.annotations.Nullable;
//
//import javax.annotation.ParametersAreNonnullByDefault;
//import java.util.Set;
//
//public abstract class MiningBlock extends KineticBlock {
//    public MiningBlock(Properties props) {
//        super(props);
//    }
//
//    @Override
//    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
//        super.onPlace(state, level, pos, oldState, isMoving);
//        if (level.isClientSide || oldState.is(state.getBlock()) ||
//                !(level.getBlockEntity(pos) instanceof MiningBlockEntity be)) return;
//
//        be.reserveDepositBlocks();
//        be.notifyUpdate();
//    }
//
//    @ParametersAreNonnullByDefault
//    @Override
//    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
//        Set<com.bmaster.createrns.mining.MiningBlockEntity> nearbyMiningBEs = null;
//        if (!state.is(newState.getBlock())) {
//            BlockEntity be = level.getBlockEntity(pos);
//            if (be instanceof MiningBlockEntity minerBE) {
//                // Pop inventory contents on the ground
//                be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
//                    for (int i = 0; i < iItemHandler.getSlots(); ++i) {
//                        ItemStack stack = iItemHandler.getStackInSlot(i);
//                        if (!stack.isEmpty()) {
//                            Block.popResource(level, pos, stack);
//                        }
//                    }
//                });
//
//                // Collect all nearby miner BEs
//                if (!level.isClientSide) {
//                    nearbyMiningBEs = MiningBlockEntityInstanceHolder.getInstancesWithIntersectingMiningArea(minerBE);
//                }
//            }
//        }
//        super.onRemove(state, level, pos, newState, movedByPiston);
//
//        if (nearbyMiningBEs != null) {
//            // Now that our own BE is removed, let others re-reserve their deposit blocks
//            for (var m : nearbyMiningBEs) {
//                m.reserveDepositBlocks();
//                m.notifyUpdate();
//            }
//        }
//    }
//}

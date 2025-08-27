package com.bmaster.createrns.block.miner;

import com.bmaster.createrns.RNSContent;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.Set;

public class MinerBlock extends KineticBlock implements IBE<MinerBlockEntity> {
    public MinerBlock(Properties props) {
        super(props);
    }

    public static Direction.Axis getRotationAxis() {
        return Direction.Axis.Y;
    }

    @ParametersAreNonnullByDefault
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MinerBlockEntity(pPos, pState);
    }

    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    @Override
    public @NotNull InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer,
                                          InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide() && pPlayer instanceof ServerPlayer serverPlayer) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof MinerBlockEntity minerBE) {
                NetworkHooks.openScreen(serverPlayer, minerBE, buf -> {
                    buf.writeBlockPos(minerBE.getBlockPos());
                });
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || oldState.is(state.getBlock()) ||
                !(level.getBlockEntity(pos) instanceof MinerBlockEntity be)) return;

        be.reserveDepositBlocks();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    @ParametersAreNonnullByDefault
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        Set<MinerBlockEntity> nearbyMiners = null;
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MinerBlockEntity minerBE) {
                // Pop inventory contents on the ground
                be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
                    for (int i = 0; i < iItemHandler.getSlots(); ++i) {
                        ItemStack stack = iItemHandler.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            Block.popResource(level, pos, stack);
                        }
                    }
                });

                // Collect all nearby miner BEs
                if (!level.isClientSide) {
                    nearbyMiners = MinerBlockEntityInstanceHolder.getInstancesWithIntersectingMiningArea(minerBE);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);

        if (nearbyMiners != null) {
            // Now that our own BE is removed, let other miners re-reserve their deposit blocks
            for (var m : nearbyMiners) {
                m.reserveDepositBlocks();
                var mPos = m.getBlockPos();
                var mState = level.getBlockState(mPos);
                // onRemove is not called for client levels, so a sync is necessary
                level.sendBlockUpdated(mPos, mState, mState, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public Class<MinerBlockEntity> getBlockEntityClass() {
        return MinerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MinerBlockEntity> getBlockEntityType() {
        return RNSContent.MINER_BE.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return getRotationAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.UP;
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.FAST;
    }
}

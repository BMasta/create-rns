package com.bmaster.createrns.block.miner;

import com.bmaster.createrns.AllContent;
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
                    // Send an item to the client, so it can render it as a placeholder in the yield slot
                    ItemStack ghostItemStack = ItemStack.EMPTY;
                    if (minerBE.process != null) {
                        ghostItemStack = new ItemStack(minerBE.process.minedItemStack.getItem());
                    }
                    buf.writeItem(ghostItemStack);
                });
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @ParametersAreNonnullByDefault
    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        if (!pState.is(pNewState.getBlock())) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof MinerBlockEntity) {
                be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
                    for (int i = 0; i < iItemHandler.getSlots(); ++i) {
                        ItemStack stack = iItemHandler.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            Block.popResource(pLevel, pPos, stack);
                        }
                    }
                });
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }

    @Override
    public Class<MinerBlockEntity> getBlockEntityClass() {
        return MinerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MinerBlockEntity> getBlockEntityType() {
        return AllContent.MINER_BE.get();
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
        return SpeedLevel.MEDIUM;
    }
}

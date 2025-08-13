package com.github.bmasta.createrns.excavator;

import com.github.bmasta.createrns.Content;
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

public class ExcavatorBlock extends KineticBlock implements IBE<ExcavatorBlockEntity> {
    public ExcavatorBlock(Properties props) {
        super(props);
    }

    @ParametersAreNonnullByDefault
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ExcavatorBlockEntity(pPos, pState);
    }

    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    @Override
    public @NotNull InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer,
                                          InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide() && pPlayer instanceof ServerPlayer serverPlayer) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof ExcavatorBlockEntity excavatorBE) {
                NetworkHooks.openScreen(serverPlayer, excavatorBE, buf -> {
                    buf.writeBlockPos(excavatorBE.getBlockPos());
                    // Send an item to the client, so it can render it as a placeholder in the yield slot
                    ItemStack ghostItemStack = ItemStack.EMPTY;
                    if (excavatorBE.process != null) {
                        ghostItemStack = new ItemStack(excavatorBE.process.excavatedItemStack.getItem());
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
            if (be instanceof ExcavatorBlockEntity) {
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
    public Class<ExcavatorBlockEntity> getBlockEntityClass() {
        return ExcavatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ExcavatorBlockEntity> getBlockEntityType() {
        return Content.EXCAVATOR_BE.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
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

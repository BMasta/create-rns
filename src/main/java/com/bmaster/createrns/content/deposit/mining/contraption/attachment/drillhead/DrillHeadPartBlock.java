package com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DrillHeadPartBlock extends Block {
    public DrillHeadPartBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        var ownerPos = DrillHeadMultiblock.findOwnerController(level, pos);
        if (ownerPos == null) return ItemInteractionResult.FAIL;

        var ownerState = level.getBlockState(ownerPos);
        if (!(ownerState.getBlock() instanceof DrillHeadBlock owner)) return ItemInteractionResult.FAIL;

        var ownerHit = new BlockHitResult(hitResult.getLocation(), hitResult.getDirection(), ownerPos, hitResult.isInside());
        return owner.useItemOn(stack, ownerState, level, ownerPos, player, hand, ownerHit);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || state.is(oldState.getBlock())) return;
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) return;
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        if (DrillHeadMultiblock.findOwnerController(level, pos) != null) return;
        level.removeBlock(pos, false);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            var ownerPos = DrillHeadMultiblock.findOwnerController(level, pos);
            if (ownerPos != null) {
                level.destroyBlock(ownerPos, true);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

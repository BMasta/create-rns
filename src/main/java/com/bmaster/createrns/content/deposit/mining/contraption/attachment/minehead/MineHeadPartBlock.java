package com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead;

import com.bmaster.createrns.RNSBlocks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MineHeadPartBlock extends Block {
    public MineHeadPartBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
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
        if (MineHeadMultiblock.findOwnerController(level, pos) != null) return;
        level.removeBlock(pos, false);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !movedByPiston && !state.is(newState.getBlock()) &&
                !newState.is(Blocks.IRON_BLOCK) && !newState.is(RNSBlocks.MINE_HEAD.get())) {
            var ownerPos = MineHeadMultiblock.findOwnerController(level, pos);
            if (ownerPos != null) {
                var ownerState = level.getBlockState(ownerPos);
                if (ownerState.getValue(MineHeadBlock.SIZE) != MineHeadSize.SMALL) {
                    // Trigger multiblock removal
                    MineHeadMultiblock.breakMultiblock(level, ownerPos, ownerState);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

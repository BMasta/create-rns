package com.bmaster.createrns.mining;

import com.bmaster.createrns.mining.miner.MinerBlockEntityInstanceHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

public class DepositBlock extends Block {
    public DepositBlock(Properties pProperties) {
        super(pProperties);
    }

    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide) return;
        for (var m : MinerBlockEntityInstanceHolder.getInstancesThatMine(level, pos)) {
            m.reserveDepositBlocks();
            var mPos = m.getBlockPos();
            var mState = level.getBlockState(mPos);
            // onRemove is not called for client levels, so a sync is necessary
            level.sendBlockUpdated(mPos, mState, mState, Block.UPDATE_CLIENTS);
        }
    }

    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (level.isClientSide) return;
        for (var m : MinerBlockEntityInstanceHolder.getInstancesThatMine(level, pos)) {
            m.reserveDepositBlocks();
            var mPos = m.getBlockPos();
            var mState = level.getBlockState(mPos);
            // onRemove is not called for client levels, so a sync is necessary
            level.sendBlockUpdated(mPos, mState, mState, Block.UPDATE_CLIENTS);
        }
    }
}

package com.bmaster.createrns.mining.miner.impl;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.mining.miner.MinerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

public class MinerMk2Block extends MinerBlock<MinerMk2BlockEntity> {
    public MinerMk2Block(Properties props) {
        super(props);
    }

    @ParametersAreNonnullByDefault
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MinerMk2BlockEntity(pPos, pState);
    }

    @Override
    public Class<MinerMk2BlockEntity> getBlockEntityClass() {
        return MinerMk2BlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MinerMk2BlockEntity> getBlockEntityType() {
        return RNSContent.MINER_MK2_BE.get();
    }
}

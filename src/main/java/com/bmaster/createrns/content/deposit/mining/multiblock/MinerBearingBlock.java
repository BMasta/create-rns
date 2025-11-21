package com.bmaster.createrns.content.deposit.mining.multiblock;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer.ClaimerType;
import com.bmaster.createrns.content.deposit.claiming.IDepositClaimerOutlineTarget;
import com.bmaster.createrns.content.deposit.mining.block.MiningBehaviour;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MinerBearingBlock extends BearingBlock implements IBE<MinerBearingBlockEntity>, IDepositClaimerOutlineTarget {
    public MinerBearingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Area must be collected from this BE before it's removed
        ClaimerType type = null;
        BoundingBox area = null;
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MinerBearingBlockEntity be) {
            var mb = be.getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE);
            type = mb.getClaimerType();
            area = mb.getClaimingBoundingBox();
        }

        super.onRemove(state, level, pos, newState, movedByPiston);

        // Area must be reclaimed after this BE is removed
        if (type != null && area != null) IDepositBlockClaimer.reclaimArea(level, area, type);
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn,
                                 BlockHitResult hit) {
        if (!player.mayBuild())
            return InteractionResult.FAIL;
        if (player.isShiftKeyDown())
            return InteractionResult.FAIL;
        if (player.getItemInHand(handIn)
                .isEmpty()) {
            if (worldIn.isClientSide)
                return InteractionResult.SUCCESS;
            withBlockEntityDo(worldIn, pos, be -> {
                if (be.isRunning()) {
                    be.disassemble();
                    return;
                }
                be.assembleNextTick();
            });
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public ClaimerType getClaimerType() {
        return MiningBehaviour.CLAIMER_TYPE;
    }

    @Override
    public Class<MinerBearingBlockEntity> getBlockEntityClass() {
        return MinerBearingBlockEntity.class;
    }

    @Override
    public BlockEntityType<MinerBearingBlockEntity> getBlockEntityType() {
        return RNSContent.MINER_BEARING_BE.get();
    }
}

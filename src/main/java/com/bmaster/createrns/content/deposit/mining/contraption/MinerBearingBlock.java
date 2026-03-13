package com.bmaster.createrns.content.deposit.mining.contraption;

import com.bmaster.createrns.RNSBlockEntities;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer.ClaimerType;
import com.bmaster.createrns.content.deposit.claiming.IDepositClaimerOutlineTarget;
import com.bmaster.createrns.content.deposit.mining.MiningBehaviour;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MinerBearingBlock extends BearingBlock implements IBE<MinerBearingBlockEntity>, IDepositClaimerOutlineTarget {
    public MinerBearingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Area must be collected from this BE before it's removed
        ClaimerType type = null;
        BoundingBox area = null;
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MinerBearingBlockEntity be) {
            var mb = be.getBehaviour(ContraptionMiningBehaviour.BEHAVIOUR_TYPE);
            type = mb.getClaimerType();
            area = mb.getClaimingBoundingBox();
        }

        super.onRemove(state, level, pos, newState, movedByPiston);

        // Area must be reclaimed after this BE is removed
        if (type != null && area != null) IDepositBlockClaimer.reclaimArea(level, area, type);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.mayBuild())
            return ItemInteractionResult.FAIL;
        if (player.isShiftKeyDown())
            return ItemInteractionResult.FAIL;
        if (stack.isEmpty()) {
            if (level.isClientSide)
                return ItemInteractionResult.SUCCESS;
            withBlockEntityDo(level, pos, be -> {
                if (be.isRunning()) {
                    be.disassemble();
                    return;
                }
                be.assembleNextTick();
            });
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
        return RNSBlockEntities.MINER_BEARING.get();
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.FAST;
    }
}

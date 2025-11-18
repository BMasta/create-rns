package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.claiming.IDepositClaimerOutlineTarget;
import com.simibubi.create.AllShapes;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

public class MinerBlock extends KineticBlock implements IBE<MinerBlockEntity>, IDepositClaimerOutlineTarget, ICogWheel {
    public MinerBlock(Properties props) {
        super(props);
    }

    @Override
    public IDepositBlockClaimer.ClaimerType getClaimerType() {
        return MiningBehaviour.CLAIMER_TYPE;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || oldState.is(state.getBlock()) ||
                !(level.getBlockEntity(pos) instanceof MinerBlockEntity be)) return;
        be.getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE).claimDepositBlocks();
    }

    @ParametersAreNonnullByDefault
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Area must be collected from this BE before it's removed
        IDepositBlockClaimer.ClaimerType type = null;
        BoundingBox area = null;
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MinerBlockEntity be) {
            var mb = be.getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE);
            type = mb.getClaimerType();
            area = mb.getClaimingBoundingBox();
        }

        super.onRemove(state, level, pos, newState, movedByPiston);

        // Area must be reclaimed after this BE is removed
        if (type != null && area != null) IDepositBlockClaimer.reclaimArea(level, area, type);
    }

    public static Direction.Axis getRotationAxis() {
        return Direction.Axis.Y;
    }

    @ParametersAreNonnullByDefault
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MinerBlockEntity(pPos, pState);
    }

    @Override
    public Class<MinerBlockEntity> getBlockEntityClass() {
        return MinerBlockEntity.class;
    }

    @Override
    public BlockEntityType<MinerBlockEntity> getBlockEntityType() {
        return RNSContent.MINER_BE.get();
    }

    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    @Override
    public @NotNull InteractionResult use(BlockState state, Level l, BlockPos pos, Player p, InteractionHand hand,
                                          BlockHitResult hit) {
        return onBlockEntityUse(l, pos, be -> {
            var minerInv = be.getItemHandler(null);
            if (minerInv.isEmpty()) return InteractionResult.PASS;
            var playerInv = p.getInventory();
            boolean pickedUp = false;
            for (int i = 0; i < minerInv.getSlots(); ++i) {
                var stack = minerInv.extractItem(i, false);
                if (stack.isEmpty()) continue;
                playerInv.placeItemBackInInventory(stack);
                pickedUp = true;
            }
            if (pickedUp) {
                l.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .2f,
                        1f + Create.RANDOM.nextFloat());
            }
            return InteractionResult.SUCCESS;
        });
    }

    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    @Override
    public @NotNull VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return AllShapes.CASING_12PX.get(Direction.DOWN);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return getRotationAxis();
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.FAST;
    }
}

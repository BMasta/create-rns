package com.bmaster.createrns.mining.miner;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.mining.MiningBlock;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

public class MinerBlock extends MiningBlock implements IBE<MinerBlockEntity> {
    public static Direction.Axis getRotationAxis() {
        return Direction.Axis.Y;
    }

    public MinerBlock(Properties props) {
        super(props);
    }

    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    @Override
    public @NotNull InteractionResult use(BlockState state, Level l, BlockPos pos, Player p, InteractionHand hand,
                                          BlockHitResult hit) {
        return onBlockEntityUse(l, pos, be -> {
            var minerInv = be.getInventory();
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

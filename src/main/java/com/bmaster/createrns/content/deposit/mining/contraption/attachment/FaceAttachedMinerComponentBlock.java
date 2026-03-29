package com.bmaster.createrns.content.deposit.mining.contraption.attachment;

import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer.ClaimerType;
import com.bmaster.createrns.content.deposit.claiming.IDepositClaimerOutlineTarget;
import com.bmaster.createrns.content.deposit.mining.MiningBehaviour;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FaceAttachedMinerComponentBlock extends FaceAttachedHorizontalDirectionalBlock
        implements IDepositClaimerOutlineTarget, ProperWaterloggedBlock {
    public static final MapCodec<FaceAttachedMinerComponentBlock> CODEC = simpleCodec(FaceAttachedMinerComponentBlock::new);

    public static Direction getConnectedDirection(BlockState state) {
        switch (state.getValue(FACE)) {
            case CEILING -> {
                return Direction.DOWN;
            }
            case FLOOR -> {
                return Direction.UP;
            }
            default -> {
                return state.getValue(FACING);
            }
        }
    }

    public static BlockState withConnectedDirection(BlockState state, Direction direction) {
        return switch (direction) {
            case UP -> state.setValue(FACE, AttachFace.FLOOR);
            case DOWN -> state.setValue(FACE, AttachFace.CEILING);
            default -> state.setValue(FACE, AttachFace.WALL).setValue(FACING, direction);
        };
    }

    public FaceAttachedMinerComponentBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, WATERLOGGED);
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var bs = super.getStateForPlacement(context);
        if (bs == null) return null;

        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            if (bs.getValue(FACE) == AttachFace.WALL) {
                bs = bs.setValue(FACING, bs.getValue(FACING).getOpposite());
            } else if (bs.getValue(FACE) == AttachFace.CEILING) {
                bs = bs.setValue(FACE, AttachFace.FLOOR);
            } else {
                bs = bs.setValue(FACE, AttachFace.CEILING);
            }
        }

        return withWater(bs, context);
    }

    @Override
    protected BlockState updateShape(
            BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos
    ) {
        updateWater(level, state, pos);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return fluidState(state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    public ClaimerType getClaimerType() {
        return MiningBehaviour.CLAIMER_TYPE;
    }
}

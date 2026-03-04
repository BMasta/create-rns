package com.bmaster.createrns.content.deposit.mining.contraption.attachment;

import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer.ClaimerType;
import com.bmaster.createrns.content.deposit.claiming.IDepositClaimerOutlineTarget;
import com.bmaster.createrns.content.deposit.mining.MiningBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningEquipmentBlock extends FaceAttachedHorizontalDirectionalBlock implements IDepositClaimerOutlineTarget {
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

    public MiningEquipmentBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
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

        return bs;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

//    @Override
//    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
//        var face = state.getValue(FACE);
//        var rot = state.getValue(ROTATION);
//        var facing = state.getValue(FACING);
//        if (face != AttachFace.WALL) {
//            rot = switch (facing) {
//                case DOWN, UP, SOUTH -> Rotation.NONE;
//                case WEST -> Rotation.CLOCKWISE_90;
//                case NORTH -> Rotation.CLOCKWISE_180;
//                case EAST -> Rotation.COUNTERCLOCKWISE_90;
//            };
//        } else {
//            rot = rot.getRotated(Rotation.CLOCKWISE_90);
//        }
//        var s = getAttachedRotatedShape(getConnectedDirection(state), rot);
//        // Don't even ask me why. It works, okay?
//        boolean invert = (face == AttachFace.WALL && facing.getAxis() == Direction.Axis.Z);
//        return s.get(invert ? Direction.DOWN : Direction.UP);
//    }

    @Override
    public ClaimerType getClaimerType() {
        return MiningBehaviour.CLAIMER_TYPE;
    }
}

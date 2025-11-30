package com.bmaster.createrns.content.deposit.mining.multiblock.attachment;

import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer.ClaimerType;
import com.bmaster.createrns.content.deposit.claiming.IDepositClaimerOutlineTarget;
import com.bmaster.createrns.content.deposit.mining.block.MiningBehaviour;
import com.simibubi.create.AllShapes;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningEquipmentBlock extends FaceAttachedHorizontalDirectionalBlock implements IDepositClaimerOutlineTarget {
    public static final EnumProperty<Rotation> ROTATION = EnumProperty.create("rotation", Rotation.class);
    public static final VoxelShaper BLOCK_SHAPE = new AllShapes.Builder(Block.box(0, 0, 0, 16, 16, 16)).forDirectional();

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
        builder.add(FACING, FACE, ROTATION); // Rotation describes the Z-rotation of a block when attached to a wall
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var bs = super.getStateForPlacement(context);
        if (bs == null) return null;

        boolean isShifting = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        if (isShifting) {
            if (bs.getValue(FACE) == AttachFace.WALL) {
                bs = bs.setValue(FACING, bs.getValue(FACING).getOpposite());
            } else if (bs.getValue(FACE) == AttachFace.CEILING) {
                bs = bs.setValue(FACE, AttachFace.FLOOR);
            } else {
                bs = bs.setValue(FACE, AttachFace.CEILING);
            }
        }

        Rotation rot = Rotation.NONE;
        if (bs.getValue(FACE) == AttachFace.WALL) {
            var lookingDir = context.getNearestLookingDirection();
            if (lookingDir.getAxis() == Direction.Axis.Y) {
                rot = Rotation.CLOCKWISE_90;
            }
        }
        return bs.setValue(ROTATION, rot);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var face = state.getValue(FACE);
        var rot = state.getValue(ROTATION);
        var facing = state.getValue(FACING);
        if (face != AttachFace.WALL) {
            rot = switch (facing) {
                case DOWN, UP, SOUTH -> Rotation.NONE;
                case WEST -> Rotation.CLOCKWISE_90;
                case NORTH -> Rotation.CLOCKWISE_180;
                case EAST -> Rotation.COUNTERCLOCKWISE_90;
            };
        } else {
            rot = rot.getRotated(Rotation.CLOCKWISE_90);
        }
        var s = getAttachedRotatedShape(getConnectedDirection(state), rot);
        // Don't even ask me why. It works, okay?
        boolean invert = (face == AttachFace.WALL && facing.getAxis() == Direction.Axis.Z);
        return s.get(invert ? Direction.DOWN : Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        return state;
    }

    public VoxelShaper getAttachedRotatedShape(Direction attachedFace, Rotation rot) {
        return BLOCK_SHAPE;
    }

    @Override
    public ClaimerType getClaimerType() {
        return MiningBehaviour.CLAIMER_TYPE;
    }
}

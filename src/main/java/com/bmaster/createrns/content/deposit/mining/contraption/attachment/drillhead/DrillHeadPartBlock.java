package com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DrillHeadPartBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<DrillHeadPartPosition> POSITION =
            EnumProperty.create("position", DrillHeadPartPosition.class);

    public DrillHeadPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(POSITION, DrillHeadPartPosition.CORE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POSITION);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(POSITION) == DrillHeadPartPosition.CORE) return Shapes.block();

        var dirs = getOutwardDirections(state.getValue(FACING), state.getValue(POSITION));
        int minX = 0, minY = 0, minZ = 0;
        int maxX = 16, maxY = 16, maxZ = 16;

        for (var dir : dirs) {
            switch (dir) {
                case EAST -> minX = 8;
                case WEST -> maxX = 8;
                case UP -> minY = 8;
                case DOWN -> maxY = 8;
                case SOUTH -> minZ = 8;
                case NORTH -> maxZ = 8;
            }
        }

        return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    protected static Direction[] getOutwardDirections(Direction facing, DrillHeadPartPosition position) {
        var u = DrillHeadMultiblock.getUDirection(facing);
        var v = DrillHeadMultiblock.getVDirection(facing);
        return switch (position) {
            case CORE -> new Direction[0];
            case BOTTOM -> new Direction[]{v};
            case TOP -> new Direction[]{v.getOpposite()};
            case LEFT -> new Direction[]{u};
            case RIGHT -> new Direction[]{u.getOpposite()};
            case BOTTOM_LEFT -> new Direction[]{v, u};
            case BOTTOM_RIGHT -> new Direction[]{v, u.getOpposite()};
            case TOP_LEFT -> new Direction[]{v.getOpposite(), u};
            case TOP_RIGHT -> new Direction[]{v.getOpposite(), u.getOpposite()};
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        var ownerPos = DrillHeadMultiblock.findOwnerController(level, pos);
        if (ownerPos == null) return ItemInteractionResult.FAIL;

        var ownerState = level.getBlockState(ownerPos);
        if (!(ownerState.getBlock() instanceof DrillHeadBlock owner)) return ItemInteractionResult.FAIL;

        var ownerHit = new BlockHitResult(hitResult.getLocation(), hitResult.getDirection(), ownerPos, hitResult.isInside());
        return owner.useItemOn(stack, ownerState, level, ownerPos, player, hand, ownerHit);
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
        if (DrillHeadMultiblock.findOwnerController(level, pos) != null) return;
        level.removeBlock(pos, false);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !movedByPiston && !state.is(newState.getBlock())) {
            var ownerPos = DrillHeadMultiblock.findOwnerController(level, pos);
            if (ownerPos != null) {
                level.destroyBlock(ownerPos, true);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

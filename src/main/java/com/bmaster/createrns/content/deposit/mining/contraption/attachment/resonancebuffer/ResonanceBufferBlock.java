package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonancebuffer;

import com.simibubi.create.AllShapes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResonanceBufferBlock extends Block {
    public static final VoxelShape SHAPE = new AllShapes.Builder(
            // Top frame
            Block.box(0, 13, 0, 16, 16, 3))
            .add(Block.box(0, 13, 13, 16, 16, 16))
            .add(Block.box(0, 13, 3, 3, 16, 13))
            .add(Block.box(13, 13, 3, 16, 16, 13))
            // Bottom frame
            .add(Block.box(0, 0, 0, 16, 3, 3))
            .add(Block.box(0, 0, 13, 16, 3, 16))
            .add(Block.box(0, 0, 3, 3, 3, 13))
            .add(Block.box(13, 0, 3, 16, 3, 13))
            // Vertical bars
            .add(Block.box(0, 3, 0, 3, 13, 3))
            .add(Block.box(0, 3, 13, 3, 13, 16))
            .add(Block.box(13, 3, 0, 16, 13, 3))
            .add(Block.box(13, 3, 13, 16, 13, 16))
            // Core
            .add(Block.box(3, 3, 3, 13, 13, 13))
            .build();

    public ResonanceBufferBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}

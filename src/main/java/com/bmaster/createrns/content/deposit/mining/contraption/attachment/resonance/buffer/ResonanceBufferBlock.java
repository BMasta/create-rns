package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.buffer;

import com.bmaster.createrns.RNSBlockEntities;
import com.bmaster.createrns.RNSTags;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResonanceBufferBlock extends Block implements IBE<ResonanceBufferBlockEntity> {
    public static int countInContraption(BearingContraption contraption) {
        var count = 0;
        for (var info : contraption.getBlocks().values()) {
            if (info.state().is(RNSTags.RNSBlockTags.RES_BUFFER_ATTACHMENTS)) {
                count++;
            }
        }
        return count;
    }

    public static final VoxelShape SHAPE = new AllShapes.Builder(
            // Top frame
            Block.box(0, 13, 0, 16, 16, 16))
            // Bottom frame
            .add(Block.box(0, 0, 0, 16, 3, 16))
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
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public Class<ResonanceBufferBlockEntity> getBlockEntityClass() {
        return ResonanceBufferBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ResonanceBufferBlockEntity> getBlockEntityType() {
        return RNSBlockEntities.RESONANCE_BUFFER.get();
    }
}

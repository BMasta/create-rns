package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator;

import com.bmaster.createrns.RNSBlockEntities;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractResonatorBlock extends Block implements IBE<ResonatorBlockEntity> {
    public static final VoxelShape SHAPE = new AllShapes.Builder(
            Block.box(0, 0, 0, 16, 4, 16))
            .add(Block.box(0, 12, 0, 16, 16, 16))
            .add(Block.box(0, 4, 0, 4, 12, 4))
            .add(Block.box(0, 4, 12, 4, 12, 16))
            .add(Block.box(12, 4, 0, 16, 12, 4))
            .add(Block.box(12, 4, 12, 16, 12, 16))
            .add(Block.box(5, 4, 5, 11, 12, 11))
            .build();

    public abstract PartialModel getShard(boolean active);

    public abstract ParticleOptions getParticle();

    public AbstractResonatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<ResonatorBlockEntity> getBlockEntityClass() {
        return ResonatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ResonatorBlockEntity> getBlockEntityType() {
        return RNSBlockEntities.RESONATOR_BE.get();
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
}

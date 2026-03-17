package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator;

import com.bmaster.createrns.RNSBlockEntities;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.FaceAttachedMinerComponentBlock;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.VoxelShaper;
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
public abstract class AbstractResonatorBlock extends FaceAttachedMinerComponentBlock implements IBE<ResonatorBlockEntity> {
    public static final VoxelShaper SHAPE = new AllShapes.Builder(Block.box(6, 0, 6, 10, 14, 10)).forDirectional();

    public abstract PartialModel getShard(boolean active);

    public abstract ParticleOptions getParticle();

    public AbstractResonatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.get(getConnectedDirection(state));
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public Class<ResonatorBlockEntity> getBlockEntityClass() {
        return ResonatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ResonatorBlockEntity> getBlockEntityType() {
        return RNSBlockEntities.RESONATOR.get();
    }
}

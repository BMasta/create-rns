package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.propagator;

import com.bmaster.createrns.RNSBlockEntities;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.FaceAttachedMinerComponentBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.buffer.ResonanceBufferBlock;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
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
public class ResonancePropagatorBlock extends FaceAttachedMinerComponentBlock implements IBE<ResonancePropagatorBlockEntity> {
    public static final VoxelShaper SHAPE = new AllShapes.Builder(Block.box(6, 0, 6, 10, 14, 10)).forDirectional();

    public static int countInContraption(BearingContraption contraption) {
        var count = 0;
        for (var info : contraption.getBlocks().values()) {
            if (info.state().getBlock() instanceof ResonancePropagatorBlock) {
                count++;
            }
        }
        return count;
    }

    public ResonancePropagatorBlock(Properties properties) {
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
    public Class<ResonancePropagatorBlockEntity> getBlockEntityClass() {
        return ResonancePropagatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ResonancePropagatorBlockEntity> getBlockEntityType() {
        return RNSBlockEntities.RESONANCE_PROPAGATOR_BE.get();
    }
}

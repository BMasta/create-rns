package com.bmaster.createrns.content.deposit.mining.multiblock.attachment.resonator;

import com.bmaster.createrns.RNSBlockEntities;
import com.bmaster.createrns.content.deposit.mining.multiblock.attachment.MiningEquipmentBlock;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractResonatorBlock extends MiningEquipmentBlock implements IBE<ResonatorBlockEntity> {
    public static final AllShapes.Builder SHAPE = new AllShapes.Builder(Block.box(6, 0, 6, 10, 14, 10));

    public abstract PartialModel getShard(boolean active);

    public abstract ParticleOptions getParticle();

    public AbstractResonatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShaper getAttachedRotatedShape(Direction attachedFace, Rotation rot) {
        return SHAPE.forDirectional(attachedFace);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public Class<ResonatorBlockEntity> getBlockEntityClass() {
        return ResonatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ResonatorBlockEntity> getBlockEntityType() {
        return RNSBlockEntities.RESONATOR_BE.get();
    }
}

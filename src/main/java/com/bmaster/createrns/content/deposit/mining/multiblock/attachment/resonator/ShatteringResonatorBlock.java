package com.bmaster.createrns.content.deposit.mining.multiblock.attachment.resonator;

import com.bmaster.createrns.RNSPartialModels;
import com.bmaster.createrns.RNSParticleTypes;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleOptions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ShatteringResonatorBlock extends AbstractResonatorBlock {
    public ShatteringResonatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public PartialModel getShard(boolean active) {
        if (active) return RNSPartialModels.SHATTERING_RESONATOR_SHARD_ACTIVE;
        else return RNSPartialModels.SHATTERING_RESONATOR_SHARD;
    }

    @Override
    public ParticleOptions getParticle() {
        return RNSParticleTypes.SHATTERING_RESONANCE;
    }
}

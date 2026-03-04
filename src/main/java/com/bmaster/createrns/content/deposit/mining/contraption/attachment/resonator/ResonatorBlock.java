package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonator;

import com.bmaster.createrns.RNSPartialModels;
import com.bmaster.createrns.RNSParticleTypes;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleOptions;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ResonatorBlock extends AbstractResonatorBlock {
    public ResonatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public PartialModel getShard(boolean active) {
        if (active) return RNSPartialModels.RESONATOR_SHARD_ACTIVE;
        else return RNSPartialModels.RESONATOR_SHARD;
    }

    @Override
    public ParticleOptions getParticle() {
        return RNSParticleTypes.RESONANCE;
    }
}

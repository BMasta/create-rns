package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator;

import com.bmaster.createrns.RNSPartialModels;
import com.bmaster.createrns.RNSParticleTypes;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleOptions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class StabilizingResonatorBlock extends AbstractResonatorBlock {
    public StabilizingResonatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public PartialModel getShard(boolean active) {
        if (active) return RNSPartialModels.STABILIZING_RESONATOR_SHARD_ACTIVE;
        else return RNSPartialModels.STABILIZING_RESONATOR_SHARD;
    }

    @Override
    public ParticleOptions getParticle() {
        return RNSParticleTypes.STABILIZING_RESONANCE;
    }
}

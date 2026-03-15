package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ShatteringResonanceCatalystRequirement extends AbstractResonanceCatalystRequirement {
    public static final Codec<ShatteringResonanceCatalystRequirement> CODEC =
            codec(ShatteringResonanceCatalystRequirement::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, ShatteringResonanceCatalystRequirement> STREAM_CODEC =
            streamCodec(ShatteringResonanceCatalystRequirement::new);

    public ShatteringResonanceCatalystRequirement(int minResonators) {
        super(minResonators);
    }

    @Override
    public boolean isSatisfiedBy(Catalyst catalyst) {
        if (!(catalyst instanceof ShatteringResonanceCatalyst rc)) return false;
        return rc.resonatorCount >= minResonators;
    }

}

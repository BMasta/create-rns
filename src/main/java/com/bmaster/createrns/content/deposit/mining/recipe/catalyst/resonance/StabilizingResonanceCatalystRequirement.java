package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StabilizingResonanceCatalystRequirement extends AbstractResonanceCatalystRequirement {
    public static final Codec<StabilizingResonanceCatalystRequirement> CODEC =
            codec(StabilizingResonanceCatalystRequirement::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, StabilizingResonanceCatalystRequirement> STREAM_CODEC =
            streamCodec(StabilizingResonanceCatalystRequirement::new);

    public StabilizingResonanceCatalystRequirement(int minResonators) {
        super(minResonators);
    }

    @Override
    public boolean isSatisfiedBy(Catalyst catalyst) {
        if (!(catalyst instanceof StabilizingResonanceCatalyst rc)) return false;
        return rc.resonatorCount >= minResonators;
    }
}

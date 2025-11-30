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

    public StabilizingResonanceCatalystRequirement(boolean optional, float baseChance, float chancePerResonator, int minResonators) {
        super(optional, baseChance, chancePerResonator, minResonators);
    }

    @Override
    public float getChance(Catalyst catalyst) {
        if (!(catalyst instanceof StabilizingResonanceCatalyst rc)) return 0f;
        if (rc.resonatorCount < minResonators) return 0f;
        return baseChance + rc.resonatorCount * chancePerResonator;
    }
}

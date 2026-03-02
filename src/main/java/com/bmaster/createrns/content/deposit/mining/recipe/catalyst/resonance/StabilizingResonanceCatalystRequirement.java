package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StabilizingResonanceCatalystRequirement extends AbstractResonanceCatalystRequirement {
    public static final Codec<StabilizingResonanceCatalystRequirement> CODEC =
            codec(StabilizingResonanceCatalystRequirement::new);

    public static final Codec<StabilizingResonanceCatalystRequirement> STREAM_CODEC =
            streamCodec(StabilizingResonanceCatalystRequirement::new);

    public StabilizingResonanceCatalystRequirement(float baseChance, float chancePerResonator, int minResonators) {
        super(baseChance, chancePerResonator, minResonators);
    }

    @Override
    public boolean isSatisfiedBy(Catalyst catalyst) {
        if (!(catalyst instanceof StabilizingResonanceCatalyst rc)) return false;
        return rc.resonatorCount >= minResonators;
    }

    @Override
    public float getChanceMult(Catalyst catalyst) {
        if (!isSatisfiedBy(catalyst)) return 0f;
        return baseChance + ((StabilizingResonanceCatalyst) catalyst).resonatorCount * chancePerResonator;
    }

    @Override
    protected String langKey() {
        return "stabilizing";
    }

    @Override
    protected ChatFormatting style() {
        return ChatFormatting.AQUA;
    }
}

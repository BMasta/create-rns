package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ShatteringResonanceCatalystRequirement extends AbstractResonanceCatalystRequirement {
    public static final Codec<ShatteringResonanceCatalystRequirement> CODEC =
            codec(ShatteringResonanceCatalystRequirement::new);

    public static final Codec<ShatteringResonanceCatalystRequirement> STREAM_CODEC =
            streamCodec(ShatteringResonanceCatalystRequirement::new);

    public ShatteringResonanceCatalystRequirement(float baseChance, float chancePerResonator, int minResonators) {
        super(baseChance, chancePerResonator, minResonators);
    }

    @Override
    public boolean isSatisfiedBy(Catalyst catalyst) {
        if (!(catalyst instanceof ShatteringResonanceCatalyst rc)) return false;
        return rc.resonatorCount >= minResonators;
    }

    @Override
    public float getChanceMult(Catalyst catalyst) {
        if (!isSatisfiedBy(catalyst)) return 0f;
        return baseChance + ((ShatteringResonanceCatalyst) catalyst).resonatorCount * chancePerResonator;
    }

    @Override
    protected String langKey() {
        return "shattering";
    }

    @Override
    protected ChatFormatting style() {
        return ChatFormatting.RED;
    }
}

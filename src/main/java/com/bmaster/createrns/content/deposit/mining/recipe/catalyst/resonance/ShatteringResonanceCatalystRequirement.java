package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ShatteringResonanceCatalystRequirement extends AbstractResonanceCatalystRequirement {
    public static final Codec<ShatteringResonanceCatalystRequirement> CODEC =
            codec(ShatteringResonanceCatalystRequirement::new);

    public ShatteringResonanceCatalystRequirement(boolean optional, float baseChance, float chancePerResonator, int minResonators) {
        super(optional, baseChance, chancePerResonator, minResonators);
    }

    @Override
    public boolean isSatisfiedBy(Catalyst catalyst) {
        if (!(catalyst instanceof ShatteringResonanceCatalyst rc)) return false;
        return rc.resonatorCount >= minResonators;
    }

    @Override
    public float getChance(Catalyst catalyst) {
        if (!isSatisfiedBy(catalyst)) return 0f;
        return baseChance + ((ShatteringResonanceCatalyst) catalyst).resonatorCount * chancePerResonator;
    }
}

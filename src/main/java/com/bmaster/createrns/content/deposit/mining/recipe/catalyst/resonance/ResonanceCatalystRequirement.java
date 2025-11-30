package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResonanceCatalystRequirement extends AbstractResonanceCatalystRequirement {
    public static final Codec<ResonanceCatalystRequirement> CODEC =
            AbstractResonanceCatalystRequirement.codec(ResonanceCatalystRequirement::new);

    public ResonanceCatalystRequirement(boolean optional, float baseChance, float chancePerResonator, int minResonators) {
        super(optional, baseChance, chancePerResonator, minResonators);
    }

    @Override
    public float getChance(Catalyst catalyst) {
        if (!(catalyst instanceof AbstractResonanceCatalyst rc)) return 0f;
        if (rc.resonatorCount < minResonators) return 0f;
        return baseChance + rc.resonatorCount * chancePerResonator;
    }
}

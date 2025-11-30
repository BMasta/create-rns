package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirement;
import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public abstract class AbstractResonanceCatalystRequirement extends CatalystRequirement {
    public static <T extends AbstractResonanceCatalystRequirement> Codec<T> codec(
            Function4<Boolean, Float, Float, Integer, T> factory) {
        return RecordCodecBuilder.create(i -> i.group(
                        Codec.BOOL.fieldOf("optional").orElse(false).forGetter(c -> c.optional),
                        Codec.floatRange(0, 1).fieldOf("base_chance").orElse(0f).forGetter(c -> c.baseChance),
                        Codec.floatRange(0, 1).fieldOf("chance_per_resonator").orElse(0f).forGetter(c -> c.chancePerResonator),
                        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("min_resonators").orElse(Integer.MAX_VALUE).forGetter(c -> c.minResonators))
                .apply(i, factory));
    }

    public final boolean optional;
    public final float baseChance;
    public final float chancePerResonator;
    public final int minResonators;

    public AbstractResonanceCatalystRequirement(boolean optional, float baseChance, float chancePerResonator, int minResonators) {
        this.optional = optional;
        this.baseChance = baseChance;
        this.chancePerResonator = chancePerResonator;
        this.minResonators = minResonators;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }
}

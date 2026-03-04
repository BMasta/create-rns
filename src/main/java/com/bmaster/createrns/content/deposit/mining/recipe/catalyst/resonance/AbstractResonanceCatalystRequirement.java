package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractResonanceCatalystRequirement extends CatalystRequirement {
    // One resonator per side directly adjacent to the drill head
    public static final int MAX_RESONATORS = 4;

    public static <T extends AbstractResonanceCatalystRequirement> Codec<T> codec(
            Function<Integer, T> factory
    ) {
        return RecordCodecBuilder.create(i -> i.group(
                        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("min_resonators")
                                .orElse(Integer.MAX_VALUE)
                                .forGetter(c -> c.minResonators))
                .apply(i, factory));
    }

    public static <T extends AbstractResonanceCatalystRequirement> Codec<T> streamCodec(
            Function<Integer, T> factory
    ) {
        return RecordCodecBuilder.create(i -> i.group(
                        Codec.INT.fieldOf("min_resonators").orElse(Integer.MAX_VALUE).forGetter(c -> c.minResonators))
                .apply(i, factory));
    }

    public final int minResonators;

    public AbstractResonanceCatalystRequirement(int minResonators) {
        this.minResonators = minResonators;
    }

    @Override
    public boolean useCatalyst(Catalyst catalyst, boolean simulate) {
        return isSatisfiedBy(catalyst);
    }
}

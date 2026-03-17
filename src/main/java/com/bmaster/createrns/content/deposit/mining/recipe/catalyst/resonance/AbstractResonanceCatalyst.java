package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractResonanceCatalyst extends Catalyst {
    public final int resonatorCount;

    public AbstractResonanceCatalyst(int resonatorCount) {
        this.resonatorCount = resonatorCount;
    }
}

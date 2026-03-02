package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirement;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractResonanceCatalyst extends Catalyst {
    public final int resonatorCount;

    public AbstractResonanceCatalyst(int resonatorCount) {
        this.resonatorCount = resonatorCount;
    }

    @Override
    public boolean use(CatalystRequirement requirement, boolean simulate) {
        if (!(requirement instanceof AbstractResonanceCatalystRequirement resCR)) return false;
        return resonatorCount >= resCR.minResonators;
    }
}

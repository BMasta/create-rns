package com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;

public abstract class AbstractResonanceCatalyst extends Catalyst {
    public final int resonatorCount;

    public AbstractResonanceCatalyst(int resonatorCount) {
        this.resonatorCount = resonatorCount;
    }

    public boolean use(boolean simulate) {
        return true;
    }
}

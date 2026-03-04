package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CatalystRequirement {
    public abstract boolean isSatisfiedBy(Catalyst catalyst);

    public abstract boolean useCatalyst(Catalyst catalyst, boolean simulate);
}

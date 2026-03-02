package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class Catalyst {
    public abstract boolean use(CatalystRequirement requirement, boolean simulate);
}

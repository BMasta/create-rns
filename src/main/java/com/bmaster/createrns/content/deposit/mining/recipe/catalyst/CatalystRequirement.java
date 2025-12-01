package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

public abstract class CatalystRequirement {
    public abstract boolean isOptional();

    public abstract boolean isSatisfiedBy(Catalyst catalyst);

    public abstract float getChance(Catalyst catalyst);
}

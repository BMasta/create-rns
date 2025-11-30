package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

public abstract class CatalystRequirement {
    public abstract boolean isOptional();

    public abstract float getChance(Catalyst catalyst);
}

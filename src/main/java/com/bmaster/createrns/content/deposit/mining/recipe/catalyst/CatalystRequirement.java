package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CatalystRequirement {
    public abstract boolean isSatisfiedBy(Catalyst catalyst);

    public abstract boolean useCatalyst(Catalyst catalyst, boolean simulate);

    public abstract float getChanceMult(Catalyst catalyst);

    public abstract float getMaxChance();

    public abstract List<MutableComponent> jeiRequirementDescriptions();

    public abstract List<MutableComponent> jeiChanceDescriptions(float weightRatio);
}

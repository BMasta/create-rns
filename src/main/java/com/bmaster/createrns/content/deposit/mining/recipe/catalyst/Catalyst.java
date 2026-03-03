package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class Catalyst {
    protected Set<String> crsNames = new ObjectOpenHashSet<>();

    public void assignCRS(String crsName) {
        crsNames.add(crsName);
    }
}

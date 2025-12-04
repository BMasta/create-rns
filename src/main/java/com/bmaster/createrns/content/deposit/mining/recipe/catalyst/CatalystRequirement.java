package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.AbstractResonanceCatalystRequirement;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CatalystRequirement implements Comparable<CatalystRequirement> {
    public abstract boolean isOptional();

    public abstract boolean isSatisfiedBy(Catalyst catalyst);

    public abstract float getChance(Catalyst catalyst);

    public abstract float getMaxChance();

    /// Needed to avoid duplicate output sections in recipe descriptions in JEI
    public abstract boolean equalsSameType(CatalystRequirement obj);

    /// Needed to avoid duplicate output sections in recipe descriptions in JEI
    @Override
    public abstract int hashCode();

    /// Needed to get a stable order of output sections in recipe descriptions in JEI
    public abstract int compareToSameType(CatalystRequirement obj);

    public abstract List<MutableComponent> JEIRequirementDescriptions();

    public abstract List<MutableComponent> JEIChanceDescriptions(float weightRatio);

    @Override
    public final boolean equals(Object obj) {
        // Different classes always return false
        var c = getClass();
        if (c != obj.getClass()) return false;

        // Same class equality checks are delegated to subclasses
        return equalsSameType(c.cast(obj));
    }

    @Override
    public final int compareTo(CatalystRequirement o) {
        var c = getClass();
        var oc = o.getClass();
        int to = typeOrder();
        int oto = o.typeOrder();

        // Different known classes are ordered according to the pre-defined type order
        if (to != Integer.MAX_VALUE && oto != Integer.MAX_VALUE && to != oto) return Integer.compare(to, oto);

        // Unknown classes with different names are ordered by class name
        int classCmp = c.getName().compareTo(oc.getName());
        if (classCmp != 0) return classCmp;

        // Different classes with the same name (by some miracle) are ordered by their hash
        if (c != oc) return Integer.compare(System.identityHashCode(this), System.identityHashCode(o));

        // Same class comparisons are delegated to subclasses
        return compareToSameType(o);
    }

    /// Defines predictable order of catalysts of different types.
    /// If a catalyst is not on the list, a stable order is still guaranteed.
    protected final int typeOrder() {
        if (this instanceof AbstractResonanceCatalystRequirement) return 1;
        return Integer.MAX_VALUE;
    }
}

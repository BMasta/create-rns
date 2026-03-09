package com.bmaster.createrns.util;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HOFs {
    /// Compares elements in each list in pairs. Returns the comparison result of the first non-equal pair.
    /// Lists of equal sizes with equal pairs are considered the same.
    /// For lists of different sizes, depending on the boolean argument, sizes are compared first or last.
    public static <T, E extends Comparable<? super E>, U extends List<E>> Comparator<T>
    comparingLists(Function<? super T, ? extends U> listKeyExtractor, boolean sizeFirst) {
        return (Comparator<T> & Serializable) (t1, t2) -> {
            var l1 = listKeyExtractor.apply(t1);
            var l2 = listKeyExtractor.apply(t2);
            int l1s = l1.size();
            int l2s = l2.size();
            int sCmp = Integer.compare(l1s, l2s);

            if (sizeFirst && sCmp != 0) return sCmp;

            int minSize = Math.min(l1s, l2s);
            for (int i = 0; i < minSize; ++i) {
                var cmp = l1.get(i).compareTo(l2.get(i));
                if (cmp != 0) return cmp;
            }

            return sCmp;
        };
    }
}

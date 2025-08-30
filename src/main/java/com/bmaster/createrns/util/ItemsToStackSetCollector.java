package com.bmaster.createrns.util;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;


/// Counts all Item's in a stream and returns an unmodifiable set of ItemStack's with their counts set accordingly.
public class ItemsToStackSetCollector implements Collector<Item, Object2IntOpenHashMap<Item>, Object2IntOpenHashMap<Item>> {
    @Override
    public Supplier<Object2IntOpenHashMap<Item>> supplier() {
        return Object2IntOpenHashMap::new;
    }

    @Override
    public BiConsumer<Object2IntOpenHashMap<Item>, Item> accumulator() {
        return (i2c, i) -> {
            i2c.computeIfAbsent(i, it -> 0);
            i2c.addTo(i, 1);
        };
    }

    @Override
    public BinaryOperator<Object2IntOpenHashMap<Item>> combiner() {
        return (a, b) -> {
            b.forEach(a::addTo);
            return a;
        };
    }

    @Override
    public Function<Object2IntOpenHashMap<Item>, Object2IntOpenHashMap<Item>> finisher() {
        return m -> m;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.UNORDERED);
    }
}

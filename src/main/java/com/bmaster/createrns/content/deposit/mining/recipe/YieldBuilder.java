package com.bmaster.createrns.content.deposit.mining.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class YieldBuilder {
    private final List<ConfiguredWeightedItem> items = new ArrayList<>();
    private final List<String> catalysts = new ArrayList<>();

    private float chance = 1;
    private int jeiSlotColor = 0;

    public YieldBuilder chance(float chance) {
        if (chance < 0 || chance > 1) throw new IllegalArgumentException("Yield chance must be between 0 and 1");
        this.chance = chance;
        return this;
    }

    public YieldBuilder item(String itemId) {
        return item(itemId, 1);
    }

    public YieldBuilder item(String itemId, int weight) {
        if (weight <= 0) throw new IllegalArgumentException("Yield item weight must be positive");
        items.add(new ConfiguredWeightedItem(ResourceLocation.parse(itemId), weight));
        return this;
    }

    public YieldBuilder catalyst(String catalyst) {
        if (catalyst.isBlank()) throw new IllegalArgumentException("Catalyst name cannot be blank");
        catalysts.add(catalyst);
        return this;
    }

    public YieldBuilder jeiSlotColor(int jeiSlotColor) {
        this.jeiSlotColor = jeiSlotColor;
        return this;
    }

    public ConfiguredYield build() {
        if (items.isEmpty()) throw new IllegalStateException("Yield must define at least one item");
        return new ConfiguredYield(chance, List.copyOf(items), List.copyOf(catalysts), jeiSlotColor);
    }

    public record ConfiguredYield(
            float chance,
            List<ConfiguredWeightedItem> items,
            List<String> catalysts,
            int jeiSlotColor
    ) {
    }

    public record ConfiguredWeightedItem(ResourceLocation itemId, int weight) {
    }
}

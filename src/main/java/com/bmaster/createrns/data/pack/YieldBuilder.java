package com.bmaster.createrns.data.pack;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class YieldBuilder {
    private final List<ConfiguredWeightedItem> items = new ArrayList<>();
    private final List<String> catalysts = new ArrayList<>();

    private boolean compat = false;
    private float chance = 1;
    private int jeiSlotColor = 0;

    public YieldBuilder compat() {
        compat = true;
        return this;
    }

    public YieldBuilder chance(float chance) {
        if (chance < 0 || chance > 1) throw new IllegalArgumentException("Yield chance must be between 0 and 1");
        this.chance = chance;
        return this;
    }

    public YieldBuilder item(List<String> itemIds) {
        return itemAndTag(itemIds, List.of(), 1);
    }

    public YieldBuilder item(List<String> itemIds, int weight) {
        return itemAndTag(itemIds, List.of(), weight);
    }

    public YieldBuilder itemTag(List<String> tagIds) {
        return itemAndTag(List.of(), tagIds, 1);
    }

    public YieldBuilder itemTag(List<String> tagIds, int weight) {
        return itemAndTag(List.of(), tagIds, weight);
    }

    public YieldBuilder itemAndTag(List<String> itemIds, List<String> tagIds) {
        return itemAndTag(itemIds, tagIds, 1);
    }

    public YieldBuilder itemAndTag(List<String> itemIds, List<String> tagIds, int weight) {
        if (weight <= 0) throw new IllegalArgumentException("Yield item weight must be positive");
        if (itemIds == null && tagIds == null) throw new IllegalArgumentException("Either item or tag must be specified");
        var itemRls = itemIds.stream().map(ResourceLocation::parse).toList();
        var tagRls = tagIds.stream().map(ResourceLocation::parse).toList();
        items.add(new ConfiguredWeightedItem(itemRls, tagRls, weight));
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

    public YieldBuilder transform(UnaryOperator<YieldBuilder> transform) {
        return transform.apply(this);
    }

    public YieldBuilder transformIf(boolean condition, UnaryOperator<YieldBuilder> transform) {
        if (!condition) return this;
        return transform.apply(this);
    }

    public ConfiguredYield build() {
        if (items.isEmpty()) throw new IllegalStateException("Yield must define at least one item");
        return new ConfiguredYield(compat, chance, List.copyOf(items), List.copyOf(catalysts), jeiSlotColor);
    }

    public record ConfiguredYield(
            boolean compat,
            float chance,
            List<ConfiguredWeightedItem> items,
            List<String> catalysts,
            int jeiSlotColor
    ) {
    }

    public record ConfiguredWeightedItem(
            List<ResourceLocation> itemIds, List<ResourceLocation> tagIds, int weight
    ) {}
}

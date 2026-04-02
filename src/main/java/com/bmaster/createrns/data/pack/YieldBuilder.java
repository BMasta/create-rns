package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.content.deposit.mining.recipe.Yield.WeightedItem;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class YieldBuilder {
    protected final List<ConfiguredWeightedItem> items = new ArrayList<>();
    private final List<String> catalysts = new ArrayList<>();

    private float chance = 1;
    private int jeiSlotColor = 0;

    public YieldBuilder chance(float chance) {
        if (chance < 0 || chance > 1) throw new IllegalArgumentException("Yield chance must be between 0 and 1");
        this.chance = chance;
        return this;
    }

    public YieldBuilder item(List<String> candidateIds, int weight) {
        return item(candidateIds, weight, false);
    }

    public YieldBuilder item(List<String> candidateIds) {
        return item(candidateIds, WeightedItem.DEFAULT_WEIGHT, false);
    }

    public YieldBuilder item(String candidateId, int weight) {
        return item(List.of(candidateId), weight, false);
    }

    public YieldBuilder item(String candidateId) {
        return item(List.of(candidateId), WeightedItem.DEFAULT_WEIGHT, false);
    }

    public YieldBuilder compatItem(List<String> candidateIds, int weight) {
        return item(candidateIds, weight, true);
    }

    public YieldBuilder compatItem(List<String> candidateIds) {
        return item(candidateIds, WeightedItem.DEFAULT_WEIGHT, true);
    }

    public YieldBuilder compatItem(String candidateId, int weight) {
        return item(List.of(candidateId), weight, true);
    }

    public YieldBuilder compatItem(String candidateId) {
        return item(List.of(candidateId), WeightedItem.DEFAULT_WEIGHT, true);
    }

    protected YieldBuilder item(List<String> candidateIds, int weight, boolean compat) {
        if (weight <= 0) throw new IllegalArgumentException("Yield item weight must be positive");
        items.add(new ConfiguredWeightedItem(candidateIds, weight, compat));
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

    protected ConfiguredYield build() {
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

    public record ConfiguredWeightedItem(List<String> candidateIds, int weight, boolean compat) {
    }
}

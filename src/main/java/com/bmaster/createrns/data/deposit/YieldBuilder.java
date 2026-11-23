package com.bmaster.createrns.data.deposit;

import com.bmaster.createrns.content.deposit.mining.recipe.Yield.WeightedItemStack;
import com.bmaster.createrns.data.deposit.DepositResources.WeightedItemStackEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class YieldBuilder {
    public final List<WeightedItemStackEntry> items = new ArrayList<>();
    public final List<ResourceLocation> catalysts = new ArrayList<>();

    public float chance = 1;
    public int jeiSlotColor = 0;

    public YieldBuilder chance(float chance) {
        if (chance < 0 || chance > 1) throw new IllegalArgumentException("Yield chance must be between 0 and 1");
        this.chance = chance;
        return this;
    }

    public YieldBuilder item(String candidateId) {
        return addItemStack(List.of(candidateId), 1, WeightedItemStack.DEFAULT_WEIGHT, false);
    }

    public YieldBuilder item(String candidateId, int weight) {
        return addItemStack(List.of(candidateId), 1, weight, false);
    }

    public YieldBuilder item(List<String> candidateIds) {
        return addItemStack(candidateIds, 1, WeightedItemStack.DEFAULT_WEIGHT, false);
    }

    public YieldBuilder item(List<String> candidateIds, int weight) {
        return addItemStack(candidateIds, 1, weight, false);
    }

    public YieldBuilder itemStack(String candidateId, int count) {
        return addItemStack(List.of(candidateId), count, WeightedItemStack.DEFAULT_WEIGHT, false);
    }

    public YieldBuilder itemStack(String candidateId, int count, int weight) {
        return addItemStack(List.of(candidateId), count, weight, false);
    }

    public YieldBuilder itemStack(List<String> candidateIds, int count) {
        return addItemStack(candidateIds, count, WeightedItemStack.DEFAULT_WEIGHT, false);
    }

    public YieldBuilder itemStack(List<String> candidateIds, int count, int weight) {
        return addItemStack(candidateIds, count, weight, false);
    }

    public YieldBuilder compatItem(String candidateId) {
        return addItemStack(List.of(candidateId), 1, WeightedItemStack.DEFAULT_WEIGHT, true);
    }

    public YieldBuilder compatItem(String candidateId, int weight) {
        return addItemStack(List.of(candidateId), 1, weight, true);
    }

    public YieldBuilder compatItem(List<String> candidateIds) {
        return addItemStack(candidateIds, 1, WeightedItemStack.DEFAULT_WEIGHT, true);
    }

    public YieldBuilder compatItem(List<String> candidateIds, int weight) {
        return addItemStack(candidateIds, 1, weight, true);
    }

    public YieldBuilder compatItemStack(String candidateId, int count) {
        return addItemStack(List.of(candidateId), count, WeightedItemStack.DEFAULT_WEIGHT, true);
    }

    public YieldBuilder compatItemStack(String candidateId, int count, int weight) {
        return addItemStack(List.of(candidateId), count, weight, true);
    }

    public YieldBuilder compatItemStack(List<String> candidateIds, int count) {
        return addItemStack(candidateIds, count, WeightedItemStack.DEFAULT_WEIGHT, true);
    }

    public YieldBuilder compatItemStack(List<String> candidateIds, int count, int weight) {
        return addItemStack(candidateIds, count, weight, true);
    }

    private YieldBuilder addItemStack(List<String> candidateIds, int count, int weight, boolean compat) {
        if (count < 1 || count > Item.ABSOLUTE_MAX_STACK_SIZE) {
            throw new IllegalArgumentException("Yield item count must be between 1 and " + Item.ABSOLUTE_MAX_STACK_SIZE);
        }
        if (weight <= 0) throw new IllegalArgumentException("Yield item weight must be positive");
        items.add(new WeightedItemStackEntry(candidateIds, count, weight, compat));
        return this;
    }

    public YieldBuilder catalyst(String catalyst) {
        if (catalyst.isBlank()) throw new IllegalArgumentException("Catalyst id cannot be blank");

        var parsedId = ResourceLocation.tryParse(catalyst);
        if (parsedId == null) throw new IllegalArgumentException("Invalid catalyst id: " + catalyst);

        catalysts.add(parsedId);
        return this;
    }

    public YieldBuilder catalyst(ResourceLocation catalyst) {
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
}

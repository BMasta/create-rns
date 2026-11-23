package com.bmaster.createrns.compat.kubejs.deposit.recipe;

import com.bmaster.createrns.compat.kubejs.util.KubeSourceLine;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.util.codec.ResourceOrTagLocation;
import com.mojang.datafixers.util.Either;
import dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.RegistryAccessContainer;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class YieldKubeBuilder {
    private static final int DEFAULT_WEIGHT = 1;
    private static final int DEFAULT_COUNT = 1;
    private static final int RGB_STRING_LENGTH = 7;
    private static final long OPAQUE_ARGB_MASK = 0xFF000000L;

    private final List<List<CustomObjectRecipeComponent.Value>> items =
            new ArrayList<>();
    private final List<String> catalysts = new ArrayList<>();

    private SourceLine recipeErrorSourceLine = SourceLine.UNKNOWN;
    private float chance = 1;
    private int jeiSlotColor = 0;

    @Info("Sets the chance for this yield to roll. Must be between 0 and 1.")
    public YieldKubeBuilder chance(Context cx, float chance) {
        var coarseSourceLine = SourceLine.of(cx);
        return chance(chance, KubeSourceLine.methodCall(coarseSourceLine, "chance"));
    }

    @HideFromJS
    public YieldKubeBuilder chance(float chance) {
        return chance(chance, SourceLine.UNKNOWN);
    }

    YieldKubeBuilder chance(float chance, SourceLine sourceLine) {
        if (recipeErrorSourceLine.isUnknown() && (chance < 0 || chance > 1)) recipeErrorSourceLine = sourceLine;

        this.chance = chance;
        return this;
    }

    @HideFromJS
    public YieldKubeBuilder item(String itemOrTagId) {
        return addItemStack(itemOrTagId, DEFAULT_COUNT, DEFAULT_WEIGHT, SourceLine.UNKNOWN);
    }

    @Info("Adds an item with the default weight of 1. Passing an item tag will pick the first available item.")
    public YieldKubeBuilder item(Context cx, String itemOrTagId) {
        return addItemStack(itemOrTagId, DEFAULT_COUNT, DEFAULT_WEIGHT, SourceLine.of(cx));
    }

    @HideFromJS
    public YieldKubeBuilder item(String itemOrTagId, int weight) {
        return addItemStack(itemOrTagId, DEFAULT_COUNT, weight, SourceLine.UNKNOWN);
    }

    @Info("Adds an item with configurable weight. Passing an item tag will pick the first available item.")
    public YieldKubeBuilder item(Context cx, String itemOrTagId, int weight) {
        return addItemStack(itemOrTagId, DEFAULT_COUNT, weight, SourceLine.of(cx));
    }

    @HideFromJS
    public YieldKubeBuilder itemStack(String itemOrTagId, int count) {
        return addItemStack(itemOrTagId, count, DEFAULT_WEIGHT, SourceLine.UNKNOWN);
    }

    @Info("Adds an item stack with the default weight of 1. Passing an item tag will pick the first available item.")
    public YieldKubeBuilder itemStack(Context cx, String itemOrTagId, int count) {
        return addItemStack(itemOrTagId, count, DEFAULT_WEIGHT, SourceLine.of(cx));
    }

    @HideFromJS
    public YieldKubeBuilder itemStack(String itemOrTagId, int count, int weight) {
        return addItemStack(itemOrTagId, count, weight, SourceLine.UNKNOWN);
    }

    @Info("Adds an item stack with configurable weight. Passing an item tag will pick the first available item.")
    public YieldKubeBuilder itemStack(Context cx, String itemOrTagId, int count, int weight) {
        return addItemStack(itemOrTagId, count, weight, SourceLine.of(cx));
    }

    @Info("Requires the specified catalyst requirement set for this yield.")
    public YieldKubeBuilder catalyst(Context cx, String catalyst) {
        var coarseSourceLine = SourceLine.of(cx);
        return catalyst(catalyst, KubeSourceLine.methodCall(coarseSourceLine, "catalyst"),
                RegistryAccessContainer.of(cx).access());
    }

    @HideFromJS
    public YieldKubeBuilder catalyst(String catalyst) {
        return catalyst(catalyst, SourceLine.UNKNOWN, RegistryAccess.EMPTY);
    }

    YieldKubeBuilder catalyst(String catalyst, SourceLine sourceLine, RegistryAccess registryAccess) {
        var parsed = ResourceLocation.tryParse(catalyst);
        var exists = parsed != null && catalystExists(registryAccess, parsed);
        if (recipeErrorSourceLine.isUnknown() && !exists) recipeErrorSourceLine = sourceLine;

        catalysts.add(parsed == null ? catalyst : parsed.toString());
        return this;
    }

    @Info("Sets the JEI/EMI slot background color for this yield as a 32-bit ARGB integer.")
    public YieldKubeBuilder jeiSlotColor(int jeiSlotColor) {
        this.jeiSlotColor = jeiSlotColor;
        return this;
    }

    @Info("Sets the JEI/EMI slot background color for this yield as a #rrggbb string.")
    public YieldKubeBuilder jeiSlotColor(String jeiSlotColor) {
        this.jeiSlotColor = parseJeiSlotColor(jeiSlotColor);
        return this;
    }

    boolean hasItems() {
        return !items.isEmpty();
    }

    SourceLine recipeErrorSourceLine() {
        return recipeErrorSourceLine;
    }

    List<CustomObjectRecipeComponent.Value> build() {
        if (items.isEmpty()) throw new IllegalStateException("Yield must define at least one item");

        var keys = MiningRecipeKubeSchema.YIELD_COMPONENT.keys();
        var yield = new ArrayList<CustomObjectRecipeComponent.Value>(keys.size());
        if (chance != 1) {
            yield.add(new CustomObjectRecipeComponent.Value(keys.getFirst(), 0, chance));
        }

        yield.add(new CustomObjectRecipeComponent.Value(keys.get(1), 1, List.copyOf(items)));

        if (!catalysts.isEmpty()) {
            yield.add(new CustomObjectRecipeComponent.Value(keys.get(2), 2, List.copyOf(catalysts)));
        }

        if (jeiSlotColor != 0) {
            yield.add(new CustomObjectRecipeComponent.Value(keys.get(3), 3, jeiSlotColor));
        }

        return List.copyOf(yield);
    }

    YieldKubeBuilder addItemStack(String itemOrTagId, int count, int weight, SourceLine sourceLine) {
        var parsed = ResourceOrTagLocation.tryParse(itemOrTagId);
        if (recipeErrorSourceLine.isUnknown() && (count < 1 || count > Item.ABSOLUTE_MAX_STACK_SIZE
                || weight <= 0 || parsed == null
                || (!parsed.isTag() && BuiltInRegistries.ITEM.get(parsed.id()) == Items.AIR))) {
            recipeErrorSourceLine = sourceLine;
        }

        var keys = MiningRecipeKubeSchema.WEIGHTED_ITEM_COMPONENT.keys();
        var item = new ArrayList<CustomObjectRecipeComponent.Value>(keys.size());
        var id = parsed == null ? itemOrTagId : parsed.encode();
        item.add(new CustomObjectRecipeComponent.Value(keys.getFirst(), 0, Either.left(id)));
        if (count != DEFAULT_COUNT) {
            item.add(new CustomObjectRecipeComponent.Value(keys.get(1), 1, count));
        }
        if (weight != DEFAULT_WEIGHT) {
            item.add(new CustomObjectRecipeComponent.Value(keys.get(2), 2, weight));
        }
        item.add(new CustomObjectRecipeComponent.Value(keys.get(3), 3, false));

        items.add(List.copyOf(item));
        return this;
    }

    private static boolean catalystExists(RegistryAccess registryAccess, ResourceLocation id) {
        var registry = registryAccess.registry(CatalystRequirementSet.REGISTRY_KEY);
        //noinspection OptionalIsPresent
        if (registry.isEmpty()) return true;

        return registry.get().getHolder(ResourceKey.create(CatalystRequirementSet.REGISTRY_KEY, id)).isPresent();
    }

    private static int parseJeiSlotColor(String jeiSlotColor) {
        if (jeiSlotColor.length() != RGB_STRING_LENGTH || jeiSlotColor.charAt(0) != '#')
            throw new IllegalArgumentException("JEI slot color must be in the form #rrggbb");

        var rgb = jeiSlotColor.substring(1);
        for (var i = 0; i < rgb.length(); i++) {
            if (Character.digit(rgb.charAt(i), 16) >= 0) continue;

            throw new IllegalArgumentException("JEI slot color must be in the form #rrggbb");
        }

        return (int) (OPAQUE_ARGB_MASK | Long.parseLong(rgb, 16));
    }
}

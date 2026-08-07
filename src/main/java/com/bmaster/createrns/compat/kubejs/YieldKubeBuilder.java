package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSetLookup;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class YieldKubeBuilder {
    private static final int DEFAULT_WEIGHT = 1;
    private static final int RGB_STRING_LENGTH = 7;
    private static final long OPAQUE_ARGB_MASK = 0xFF000000L;

    private final List<List<dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent.Value>> items =
            new ArrayList<>();
    private final List<String> catalysts = new ArrayList<>();
    private final List<List<String>> unresolvedRequiredItems = new ArrayList<>();

    private float chance = 1;
    private int jeiSlotColor = 0;

    @Info("Sets the chance for this yield to roll. Must be between 0 and 1.")
    public YieldKubeBuilder chance(float chance) {
        if (chance < 0 || chance > 1) throw new IllegalArgumentException("Yield chance must be between 0 and 1");

        this.chance = chance;
        return this;
    }

    @Info("Adds an item or ordered item-tag fallback list with an explicit weight.")
    public YieldKubeBuilder item(List<String> candidateIds, int weight) {
        return item(candidateIds, weight, false);
    }

    @Info("Adds an item or ordered item-tag fallback list with the default weight of 1.")
    public YieldKubeBuilder item(List<String> candidateIds) {
        return item(candidateIds, DEFAULT_WEIGHT, false);
    }

    @Info("Adds a single item or item tag with an explicit weight.")
    public YieldKubeBuilder item(String candidateId, int weight) {
        return item(List.of(candidateId), weight, false);
    }

    @Info("Adds a single item or item tag with the default weight of 1.")
    public YieldKubeBuilder item(String candidateId) {
        return item(List.of(candidateId), DEFAULT_WEIGHT, false);
    }

    @Info("""
            Adds a compat item or ordered item-tag fallback list with an explicit weight.
            Compat items are allowed to resolve to nothing without making the whole recipe invalid.
            """)
    public YieldKubeBuilder compatItem(List<String> candidateIds, int weight) {
        return item(candidateIds, weight, true);
    }

    @Info("""
            Adds a compat item or ordered item-tag fallback list with the default weight of 1.
            Compat items are allowed to resolve to nothing without making the whole recipe invalid.
            """)
    public YieldKubeBuilder compatItem(List<String> candidateIds) {
        return item(candidateIds, DEFAULT_WEIGHT, true);
    }

    @Info("""
            Adds a single compat item or item tag with an explicit weight.
            Compat items are allowed to resolve to nothing without making the whole recipe invalid.
            """)
    public YieldKubeBuilder compatItem(String candidateId, int weight) {
        return item(List.of(candidateId), weight, true);
    }

    @Info("""
            Adds a single compat item or item tag with the default weight of 1.
            Compat items are allowed to resolve to nothing without making the whole recipe invalid.
            """)
    public YieldKubeBuilder compatItem(String candidateId) {
        return item(List.of(candidateId), DEFAULT_WEIGHT, true);
    }

    @Info("Requires the specified catalyst requirement set for this yield.")
    public YieldKubeBuilder catalyst(String catalyst) {
        if (catalyst.isBlank()) throw new IllegalArgumentException("Catalyst id cannot be blank");

        catalysts.add(CatalystRequirementSetLookup.parseId(catalyst)
                .result()
                .orElseThrow(() -> new IllegalArgumentException("Invalid catalyst id: " + catalyst))
                .toString());
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

    List<List<String>> unresolvedRequiredItems() {
        return List.copyOf(unresolvedRequiredItems);
    }

    List<dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent.Value> build() {
        if (items.isEmpty()) throw new IllegalStateException("Yield must define at least one item");

        var keys = MiningRecipeKubeSchema.YIELD_COMPONENT.keys();
        var yield = new ArrayList<dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent.Value>(keys.size());
        if (chance != 1) {
            yield.add(new dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent.Value(keys.get(0), 0, chance));
        }

        yield.add(new dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent.Value(keys.get(1), 1, List.copyOf(items)));

        if (!catalysts.isEmpty()) {
            yield.add(new dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent.Value(keys.get(2), 2, List.copyOf(catalysts)));
        }

        if (jeiSlotColor != 0) {
            yield.add(new dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent.Value(keys.get(3), 3, jeiSlotColor));
        }

        return List.copyOf(yield);
    }

    private YieldKubeBuilder item(List<String> candidateIds, int weight, boolean compat) {
        if (weight <= 0) throw new IllegalArgumentException("Yield item weight must be positive");

        var normalizedIds = candidateIds.stream()
                .map(YieldKubeBuilder::normalizeCandidateId)
                .toList();
        if (!compat && normalizedIds.stream().noneMatch(YieldKubeBuilder::isTag)
                && normalizedIds.stream().noneMatch(YieldKubeBuilder::isRegisteredItem)) {
            unresolvedRequiredItems.add(normalizedIds);
        }

        var keys = MiningRecipeKubeSchema.WEIGHTED_ITEM_COMPONENT.keys();
        var item = new ArrayList<dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent.Value>(keys.size());

        item.add(new dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent.Value(keys.get(0), 0, normalizedIds));
        if (compat) {
            item.add(new dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent.Value(keys.get(1), 1, true));
        }
        if (weight != DEFAULT_WEIGHT) {
            item.add(new dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent.Value(keys.get(2), 2, weight));
        }

        items.add(List.copyOf(item));
        return this;
    }

    private static String normalizeCandidateId(String candidateId) {
        if (candidateId.startsWith("#")) {
            var tagId = candidateId.substring(1);
            return "#" + (tagId.contains(":") ? tagId : "minecraft:" + tagId);
        }

        return candidateId.contains(":") ? candidateId : "minecraft:" + candidateId;
    }

    private static boolean isRegisteredItem(String candidateId) {
        var id = ResourceLocation.tryParse(candidateId);
        if (id == null) return false;

        return BuiltInRegistries.ITEM.getOptional(id)
                .filter(item -> item != Items.AIR)
                .isPresent();
    }

    private static boolean isTag(String candidateId) {
        return candidateId.startsWith("#");
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

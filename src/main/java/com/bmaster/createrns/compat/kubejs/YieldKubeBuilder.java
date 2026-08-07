package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.recipe.component.RecipeComponentBuilderMap;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.ListJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class YieldKubeBuilder {
    private static final int DEFAULT_WEIGHT = 1;
    private static final int RGB_STRING_LENGTH = 7;
    private static final long OPAQUE_ARGB_MASK = 0xFF000000L;

    private final List<RecipeComponentBuilderMap> items = new ArrayList<>();
    private final List<String> catalysts = new ArrayList<>();

    private float chance = 1;
    private int jeiSlotColor = 0;

    @Info("Sets the chance for this yield to roll. Must be between 0 and 1.")
    public YieldKubeBuilder chance(float chance) {
        this.chance = chance;
        return this;
    }

    @Info("Adds an item or ordered item-tag fallback list with an explicit weight.")
    public YieldKubeBuilder item(Object candidateIds, int weight) {
        return item(candidateIds(candidateIds), weight, false);
    }

    @Info("Adds an item or ordered item-tag fallback list with the default weight of 1.")
    public YieldKubeBuilder item(Object candidateIds) {
        return item(candidateIds(candidateIds), DEFAULT_WEIGHT, false);
    }

    @Info("""
            Adds a compat item or ordered item-tag fallback list with an explicit weight.
            Compat items are allowed to resolve to nothing without making the whole recipe invalid.
            """)
    public YieldKubeBuilder compatItem(Object candidateIds, int weight) {
        return item(candidateIds(candidateIds), weight, true);
    }

    @Info("""
            Adds a compat item or ordered item-tag fallback list with the default weight of 1.
            Compat items are allowed to resolve to nothing without making the whole recipe invalid.
            """)
    public YieldKubeBuilder compatItem(Object candidateIds) {
        return item(candidateIds(candidateIds), DEFAULT_WEIGHT, true);
    }

    @Info("Requires the specified catalyst requirement set for this yield.")
    public YieldKubeBuilder catalyst(String catalyst) {
        var parsed = ResourceLocation.tryParse(catalyst);
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

    RecipeComponentBuilderMap build() {
        if (items.isEmpty()) throw new IllegalStateException("Yield must define at least one item");

        return MiningRecipeKubeSchema.yield(
                chance,
                items.toArray(RecipeComponentBuilderMap[]::new),
                catalysts.toArray(String[]::new),
                jeiSlotColor != 0 ? jeiSlotColor : null
        );
    }

    private YieldKubeBuilder item(List<String> candidateIds, int weight, boolean compat) {
        var normalizedIds = candidateIds.stream()
                .map(YieldKubeBuilder::normalizeCandidateId)
                .toArray(String[]::new);
        items.add(MiningRecipeKubeSchema.weightedItem(
                normalizedIds,
                compat,
                weight != DEFAULT_WEIGHT ? weight : null
        ));
        return this;
    }

    private static String normalizeCandidateId(String candidateId) {
        if (candidateId.startsWith("#")) {
            var tagId = candidateId.substring(1);
            return "#" + (tagId.contains(":") ? tagId : "minecraft:" + tagId);
        }

        return candidateId.contains(":") ? candidateId : "minecraft:" + candidateId;
    }

    private static List<String> candidateIds(Object candidateIds) {
        return ListJS.orSelf(candidateIds).stream()
                .map(String::valueOf)
                .toList();
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

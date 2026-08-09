package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipeKubeSchema {
    static final String AIR_BLOCK_ID = "minecraft:air";
    static final String OVERWORLD_ID = "minecraft:overworld";
    static final String NETHER_ID = "minecraft:the_nether";
    static final int DEFAULT_WEIGHT = 1;

    private static final RecipeKey<Long> DURABILITY_CORE =
            NumberComponent.longRange(1, Long.MAX_VALUE).key("core");
    private static final RecipeKey<Long> DURABILITY_EDGE =
            NumberComponent.longRange(1, Long.MAX_VALUE).key("edge");
    private static final RecipeKey<Float> DURABILITY_RANDOM_SPREAD =
            NumberComponent.floatRange(0, 1).key("random_spread");
    static final RecipeComponentBuilder DURABILITY_COMPONENT =
            RecipeComponent.builder(DURABILITY_CORE, DURABILITY_EDGE, DURABILITY_RANDOM_SPREAD);

    static final ArrayRecipeComponent<String> CANDIDATE_IDS_COMPONENT = StringComponent.ANY.asArray();
    private static final RecipeKey<String[]> WEIGHTED_ITEM_IDS = CANDIDATE_IDS_COMPONENT.key("item");
    private static final RecipeKey<Boolean> WEIGHTED_ITEM_COMPAT =
            BooleanComponent.BOOLEAN.key("compat").optional(false);
    private static final RecipeKey<Integer> WEIGHTED_ITEM_WEIGHT =
            NumberComponent.intRange(1, Integer.MAX_VALUE).key("weight").optional(DEFAULT_WEIGHT);
    static final RecipeComponentBuilder WEIGHTED_ITEM_COMPONENT =
            RecipeComponent.builder(WEIGHTED_ITEM_IDS, WEIGHTED_ITEM_COMPAT, WEIGHTED_ITEM_WEIGHT);

    static final ArrayRecipeComponent<String> CATALYSTS_COMPONENT = StringComponent.NON_EMPTY.asArray();
    static final ArrayRecipeComponent<RecipeComponentBuilderMap> YIELD_ITEMS_COMPONENT = WEIGHTED_ITEM_COMPONENT.asArray();
    private static final RecipeKey<Float> YIELD_CHANCE =
            NumberComponent.floatRange(0, 1).key("chance").optional(1F);
    private static final RecipeKey<RecipeComponentBuilderMap[]> YIELD_ITEMS = YIELD_ITEMS_COMPONENT.key("items");
    private static final RecipeKey<String[]> YIELD_CATALYSTS =
            CATALYSTS_COMPONENT.key("catalysts").optional(new String[0]);
    private static final RecipeKey<Integer> YIELD_JEI_SLOT_COLOR =
            NumberComponent.INT.key("jei_slot_color").optional(0);
    static final RecipeComponentBuilder YIELD_COMPONENT =
            RecipeComponent.builder(YIELD_ITEMS, YIELD_CHANCE, YIELD_CATALYSTS, YIELD_JEI_SLOT_COLOR);
    static final ArrayRecipeComponent<RecipeComponentBuilderMap> YIELDS_COMPONENT = YIELD_COMPONENT.asArray();

    static final RecipeKey<String> DEPOSIT_BLOCK = StringComponent.ID.key("deposit_block")
            .defaultOptional()
            .exclude()
            .noBuilders();
    static final RecipeKey<String> DIMENSION = StringComponent.ID.key("dimension")
            .optional(OVERWORLD_ID)
            .exclude()
            .noBuilders();
    static final RecipeKey<String> REPLACE_WHEN_DEPLETED = StringComponent.ID.key("replace_when_depleted")
            .optional(AIR_BLOCK_ID)
            .exclude()
            .noBuilders();
    static final RecipeKey<RecipeComponentBuilderMap> DURABILITY = DURABILITY_COMPONENT.key("durability")
            .defaultOptional()
            .exclude()
            .noBuilders();
    static final RecipeKey<RecipeComponentBuilderMap[]> YIELDS = YIELDS_COMPONENT.key("yields")
            .defaultOptional()
            .exclude()
            .noBuilders();

    public static RecipeSchema schema() {
        return new RecipeSchema(MiningRecipeKubeRecipe.class, MiningRecipeKubeRecipe::new,
                DEPOSIT_BLOCK, YIELDS, DIMENSION, REPLACE_WHEN_DEPLETED, DURABILITY)
                .constructor()
                .uniqueId(recipe -> {
                    var depositBlock = recipe.getValue(DEPOSIT_BLOCK);
                    var dimension = recipe.getValue(DIMENSION);
                    return RecipeSchema.normalizeId(normalizeForId(
                            depositBlock != null ? depositBlock : "missing_deposit_block")
                            + "_" + normalizeForId(dimension != null ? dimension : OVERWORLD_ID));
                });
    }

    static RecipeComponentBuilderMap durability(long core, long edge, float randomSpread) {
        var durability = new RecipeComponentBuilderMap(DURABILITY_COMPONENT);
        durability.put(DURABILITY_CORE, core);
        durability.put(DURABILITY_EDGE, edge);
        durability.put(DURABILITY_RANDOM_SPREAD, randomSpread);
        return durability;
    }

    static RecipeComponentBuilderMap weightedItem(String[] candidateIds, boolean compat, @Nullable Integer weight) {
        var item = new RecipeComponentBuilderMap(WEIGHTED_ITEM_COMPONENT);
        item.put(WEIGHTED_ITEM_IDS, candidateIds);
        if (compat)
            item.put(WEIGHTED_ITEM_COMPAT, true);
        if (weight != null)
            item.put(WEIGHTED_ITEM_WEIGHT, weight);
        return item;
    }

    static RecipeComponentBuilderMap yield(
            float chance,
            RecipeComponentBuilderMap[] items,
            String[] catalysts,
            @Nullable Integer jeiSlotColor
    ) {
        var yield = new RecipeComponentBuilderMap(YIELD_COMPONENT);
        if (chance != 1F)
            yield.put(YIELD_CHANCE, chance);
        yield.put(YIELD_ITEMS, items);
        if (catalysts.length > 0)
            yield.put(YIELD_CATALYSTS, catalysts);
        if (jeiSlotColor != null)
            yield.put(YIELD_JEI_SLOT_COLOR, jeiSlotColor);
        return yield;
    }

    private static String normalizeForId(String value) {
        return value.replace(':', '_').replace('/', '_');
    }

    private MiningRecipeKubeSchema() {
    }
}

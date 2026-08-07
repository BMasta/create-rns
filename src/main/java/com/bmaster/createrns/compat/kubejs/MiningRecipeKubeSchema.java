package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.schema.RecipeConstructor;
import dev.latvian.mods.kubejs.recipe.component.BlockComponent;
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent;
import dev.latvian.mods.kubejs.recipe.component.ComponentRole;
import dev.latvian.mods.kubejs.recipe.component.CustomObjectRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.ResourceKeyComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.util.IntBounds;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MiningRecipeKubeSchema {
    static final RecipeComponent<Float> CHANCE_COMPONENT = NumberComponent.floatRange(0, 1)
            .withCodec(Codec.of(Codec.FLOAT, Codec.floatRange(0, 1)));
    static final RecipeComponent<Integer> WEIGHT_COMPONENT = NumberComponent.POSITIVE_INT.instance()
            .withCodec(Codec.of(Codec.INT, Codec.intRange(1, Integer.MAX_VALUE)));
    static final CustomObjectRecipeComponent DURABILITY_COMPONENT = new CustomObjectRecipeComponent(List.of(
            new CustomObjectRecipeComponent.Key("core", NumberComponent.POSITIVE_LONG.instance()),
            new CustomObjectRecipeComponent.Key("edge", NumberComponent.POSITIVE_LONG.instance()),
            new CustomObjectRecipeComponent.Key("random_spread", NumberComponent.floatRange(0, 1))
    ));
    static final ListRecipeComponent<String> CANDIDATE_IDS_COMPONENT = ListRecipeComponent.create(
            StringComponent.STRING.instance(), true, false, IntBounds.DEFAULT, Optional.empty());
    static final CustomObjectRecipeComponent WEIGHTED_ITEM_COMPONENT = new CustomObjectRecipeComponent(List.of(
            new CustomObjectRecipeComponent.Key("item", CANDIDATE_IDS_COMPONENT),
            new CustomObjectRecipeComponent.Key("compat", BooleanComponent.BOOLEAN.instance(), true),
            new CustomObjectRecipeComponent.Key("weight", WEIGHT_COMPONENT, true)
    ));
    static final ListRecipeComponent<String> CATALYSTS_COMPONENT = ListRecipeComponent.create(
            StringComponent.STRING.instance(), false, false, IntBounds.OPTIONAL, Optional.empty());
    static final ListRecipeComponent<List<CustomObjectRecipeComponent.Value>> YIELD_ITEMS_COMPONENT =
            ListRecipeComponent.create(WEIGHTED_ITEM_COMPONENT, false, false, IntBounds.DEFAULT, Optional.empty());
    static final CustomObjectRecipeComponent YIELD_COMPONENT = new CustomObjectRecipeComponent(List.of(
            new CustomObjectRecipeComponent.Key("chance", CHANCE_COMPONENT, true),
            new CustomObjectRecipeComponent.Key("items", YIELD_ITEMS_COMPONENT),
            new CustomObjectRecipeComponent.Key("catalysts", CATALYSTS_COMPONENT, true),
            new CustomObjectRecipeComponent.Key("jei_slot_color", NumberComponent.INT, true)
    ));
    static final ListRecipeComponent<List<CustomObjectRecipeComponent.Value>> YIELDS_COMPONENT =
            ListRecipeComponent.create(YIELD_COMPONENT, false, false, IntBounds.DEFAULT, Optional.empty());

    static final RecipeKey<net.minecraft.world.level.block.Block> DEPOSIT_BLOCK =
            BlockComponent.OPTIONAL_BLOCK.inputKey("deposit_block")
                    .optional(Blocks.AIR)
                    .exclude()
                    .noFunctions();
    static final RecipeKey<net.minecraft.resources.ResourceKey<Level>> DIMENSION =
            ResourceKeyComponent.DIMENSION.otherKey("dimension")
                    .optional(Level.OVERWORLD)
                    .exclude()
                    .noFunctions();
    static final RecipeKey<net.minecraft.world.level.block.Block> REPLACE_WHEN_DEPLETED =
            BlockComponent.OPTIONAL_BLOCK.otherKey("replace_when_depleted")
                    .optional(Blocks.AIR)
                    .exclude()
                    .noFunctions();
    static final RecipeKey<List<CustomObjectRecipeComponent.Value>> DURABILITY =
            DURABILITY_COMPONENT.key("durability", ComponentRole.OTHER)
                    .defaultOptional()
                    .exclude()
                    .noFunctions();
    static final RecipeKey<List<List<CustomObjectRecipeComponent.Value>>> YIELDS =
            YIELDS_COMPONENT.key("yields", ComponentRole.OTHER)
                    .defaultOptional()
                    .exclude()
                    .noFunctions();

    public static RecipeSchema schema() {
        return new RecipeSchema(DEPOSIT_BLOCK, DIMENSION, REPLACE_WHEN_DEPLETED, DURABILITY, YIELDS)
                .constructor(new RecipeConstructor())
                .factory(new KubeRecipeFactory(CreateRNS.asResource("mining"),
                        MiningRecipeKubeRecipe.class, MiningRecipeKubeRecipe::new))
                .uniqueIds(List.of(DEPOSIT_BLOCK, DIMENSION));
    }

    static List<CustomObjectRecipeComponent.Value> durability(long core, long edge, float randomSpread) {
        var keys = DURABILITY_COMPONENT.keys();
        var values = new ArrayList<CustomObjectRecipeComponent.Value>(keys.size());
        values.add(new CustomObjectRecipeComponent.Value(keys.get(0), 0, core));
        values.add(new CustomObjectRecipeComponent.Value(keys.get(1), 1, edge));
        values.add(new CustomObjectRecipeComponent.Value(keys.get(2), 2, randomSpread));
        return List.copyOf(values);
    }

    private MiningRecipeKubeSchema() {
    }
}

package com.bmaster.createrns;

import com.bmaster.createrns.mining.MiningLevel;
import com.bmaster.createrns.mining.recipe.AdvancedMiningRecipe;
import com.bmaster.createrns.mining.recipe.BasicMiningRecipe;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class RNSRecipeTypes {
    // Basic mining recipe
    public static final RegistryEntry<RecipeType<?>, RecipeType<?>> BASIC_MINING_TYPE = CreateRNS.REGISTRATE.simple(
            MiningLevel.BASIC.getRecipeID(), Registries.RECIPE_TYPE, () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return CreateRNS.MOD_ID + ":" + MiningLevel.BASIC.getRecipeID();
                }
            });

    // Advanced mining recipe
    public static final RegistryEntry<RecipeSerializer<?>, RecipeSerializer<?>> ADVANCED_MINING_SERIALIZER =
            CreateRNS.REGISTRATE.simple(MiningLevel.ADVANCED.getRecipeID(), Registries.RECIPE_SERIALIZER,
                    AdvancedMiningRecipe.Serializer::new);

    public static final RegistryEntry<RecipeType<?>, RecipeType<?>> ADVANCED_MINING_TYPE = CreateRNS.REGISTRATE.simple(
            MiningLevel.ADVANCED.getRecipeID(), Registries.RECIPE_TYPE, () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return CreateRNS.MOD_ID + ":" + MiningLevel.ADVANCED.getRecipeID();
                }
            });

    public static void register() {
    }
}

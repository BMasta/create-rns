package com.bmaster.createrns;

import com.bmaster.createrns.mining.MiningLevel;
import com.bmaster.createrns.mining.recipe.AdvancedMiningRecipe;
import com.bmaster.createrns.mining.recipe.BasicMiningRecipe;
import com.simibubi.create.api.data.recipe.WashingRecipeGen;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;

public class RNSRecipeTypes {
    // Basic mining recipe
    public static final RegistryEntry<RecipeSerializer<BasicMiningRecipe>> BASIC_MINING_SERIALIZER =
            CreateRNS.REGISTRATE.simple(MiningLevel.BASIC.getRecipeID(), ForgeRegistries.Keys.RECIPE_SERIALIZERS,
                    BasicMiningRecipe.Serializer::new);

    public static final RegistryEntry<RecipeType<BasicMiningRecipe>> BASIC_MINING_TYPE = CreateRNS.REGISTRATE.simple(
            MiningLevel.BASIC.getRecipeID(), ForgeRegistries.Keys.RECIPE_TYPES,
            () -> new RecipeType<>() {
                @Override
                public String toString() {return CreateRNS.MOD_ID + ":" + MiningLevel.BASIC.getRecipeID();
                }
            });

    // Advanced mining recipe
    public static final RegistryEntry<RecipeSerializer<AdvancedMiningRecipe>> ADVANCED_MINING_SERIALIZER =
            CreateRNS.REGISTRATE.simple(MiningLevel.ADVANCED.getRecipeID(), ForgeRegistries.Keys.RECIPE_SERIALIZERS,
                    AdvancedMiningRecipe.Serializer::new);

    public static final RegistryEntry<RecipeType<AdvancedMiningRecipe>> ADVANCED_MINING_TYPE = CreateRNS.REGISTRATE.simple(
            MiningLevel.ADVANCED.getRecipeID(), ForgeRegistries.Keys.RECIPE_TYPES,
            () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return CreateRNS.MOD_ID + ":" + MiningLevel.ADVANCED.getRecipeID();
                }
            });

    public static void register() {
    }
}

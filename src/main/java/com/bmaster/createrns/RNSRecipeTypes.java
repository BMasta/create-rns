package com.bmaster.createrns;

import com.bmaster.createrns.mining.recipe.MiningRecipe;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;

public class RNSRecipeTypes {

    public static final RegistryEntry<RecipeSerializer<MiningRecipe>> MINING_SERIALIZER =
            CreateRNS.REGISTRATE.simple("mining", ForgeRegistries.Keys.RECIPE_SERIALIZERS,
                    MiningRecipe.Serializer::new);

    public static final RegistryEntry<RecipeType<MiningRecipe>> MINING_RECIPE_TYPE =
            CreateRNS.REGISTRATE.simple("mining", ForgeRegistries.Keys.RECIPE_TYPES, () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return CreateRNS.MOD_ID + ":mining";
                }
            });

    public static void register() {
    }
}

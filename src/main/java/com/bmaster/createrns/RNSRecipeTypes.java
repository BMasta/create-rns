package com.bmaster.createrns;

import com.bmaster.createrns.mining.recipe.MiningRecipe;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class RNSRecipeTypes {
    public static final RegistryEntry<RecipeSerializer<?>, RecipeSerializer<MiningRecipe>> MINING_SERIALIZER =
            CreateRNS.REGISTRATE.simple("mining", Registries.RECIPE_SERIALIZER,
                    () -> MiningRecipe.Serializer.INSTANCE);

    public static final RegistryEntry<RecipeType<?>, RecipeType<MiningRecipe>> MINING_RECIPE_TYPE =
            CreateRNS.REGISTRATE.simple("mining", Registries.RECIPE_TYPE, () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return CreateRNS.MOD_ID + ":mining";
                }
            });

    public static void register() {
    }
}

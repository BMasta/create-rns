package com.bmaster.createrns;

import com.bmaster.createrns.mining.MiningRecipe;
import com.simibubi.create.Create;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class RNSRecipes {
    public static final RegistryEntry<RecipeSerializer<MiningRecipe>> MINING_SERIALIZER = CreateRNS.REGISTRATE.simple(
            "mining", ForgeRegistries.Keys.RECIPE_SERIALIZERS, MiningRecipe.Serializer::new);

    public static final RegistryEntry<RecipeType<MiningRecipe>> MINING_TYPE = CreateRNS.REGISTRATE.simple(
            "mining", ForgeRegistries.Keys.RECIPE_TYPES,
            () -> new RecipeType<>() {
                @Override public String toString() { return CreateRNS.MOD_ID + ":mining"; }
            });

    public static void register() {
    }
}

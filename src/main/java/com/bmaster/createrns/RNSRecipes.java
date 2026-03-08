package com.bmaster.createrns;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import com.simibubi.create.api.data.recipe.PolishingRecipeGen;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class RNSRecipes {
    static {
        CreateRNS.REGISTRATE.addDataGenerator(ProviderType.RECIPE, prov -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.REDSTONE)
                    .requires(RNSItems.REDSTONE_SMALL_DUST.get(), 9)
                    .unlockedBy("has_item", RegistrateRecipeProvider.has(RNSItems.REDSTONE_SMALL_DUST))
                    .save(prov, CreateRNS.asResource("redstone_from_small_dust"));
        });
    }

    public static class MechanicalCrafting extends MechanicalCraftingRecipeGen {
        GeneratedRecipe RESONANCE_BUFFER = create(RNSBlocks.RESONANCE_BUFFER::get).returns(2)
                .recipe(b -> b
                        .key('S', AllItems.STURDY_SHEET)
                        .key('A', RNSItems.POLISHED_RESONANT_AMETHYST)
                        .patternLine(" SSS ")
                        .patternLine("SAAAS")
                        .patternLine("SAAAS")
                        .patternLine("SAAAS")
                        .patternLine(" SSS ")
                        .disallowMirrored());

        public MechanicalCrafting(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, CreateRNS.ID);
        }
    }

    public static class Polishing extends PolishingRecipeGen {
        GeneratedRecipe POLISHED_RESONANT_AMETHYST = create(RNSItems.RESONANT_AMETHYST::get,
                b -> b.output(RNSItems.POLISHED_RESONANT_AMETHYST.get()));

        public Polishing(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, CreateRNS.ID);
        }
    }

    public static void register() {
    }
}

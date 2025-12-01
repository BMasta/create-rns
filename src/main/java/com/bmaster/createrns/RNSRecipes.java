package com.bmaster.createrns;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import com.simibubi.create.api.data.recipe.WashingRecipeGen;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

public class RNSRecipes {
    static {
        CreateRNS.REGISTRATE.addDataGenerator(ProviderType.RECIPE, prov -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.REDSTONE)
                    .requires(RNSContent.REDSTONE_SMALL_DUST.get(), 9)
                    .unlockedBy("has_item", RegistrateRecipeProvider.has(RNSContent.REDSTONE_SMALL_DUST))
                    .save(prov, ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "redstone_from_small_dust"));
        });
    }

    public static class Washing extends WashingRecipeGen {
        GeneratedRecipe IMPURE_IRON_ORE = fromImpure("impure_iron_ore", RNSContent.IMPURE_IRON_ORE, Items.IRON_NUGGET);
        GeneratedRecipe IMPURE_COPPER_ORE = fromImpure("impure_copper_ore", RNSContent.IMPURE_COPPER_ORE, AllItems.COPPER_NUGGET);
        GeneratedRecipe IMPURE_ZINC_ORE = fromImpure("impure_zinc_ore", RNSContent.IMPURE_ZINC_ORE, AllItems.ZINC_NUGGET);
        GeneratedRecipe IMPURE_GOLD_ORE = fromImpure("impure_gold_ore", RNSContent.IMPURE_GOLD_ORE, Items.GOLD_NUGGET);
        GeneratedRecipe IMPURE_REDSTONE_DUST = fromImpure("impure_redstone_dust", RNSContent.IMPURE_REDSTONE_DUST, RNSContent.REDSTONE_SMALL_DUST);

        public Washing(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
            super(output, provider, CreateRNS.MOD_ID);
        }

        private GeneratedRecipe fromImpure(String name, ItemLike in, ItemLike out) {
            return create(name, b -> b.require(in)
                    .output(Items.COBBLESTONE)
                    .output(0.5f, out));
        }
    }

    public static class MechanicalCrafting extends MechanicalCraftingRecipeGen {
        GeneratedRecipe SHATTERING_RESONATOR = create(RNSContent.SHATTERING_RESONATOR_BLOCK::get).returns(1)
                .recipe(b -> b
                        .key('T', Blocks.REDSTONE_BLOCK)
                        .key('A', RNSContent.RESONANT_AMETHYST)
                        .key('M', AllItems.PRECISION_MECHANISM)
                        .key('R', RNSContent.RESONATOR_BLOCK)
                        .key('S', AllItems.STURDY_SHEET)
                        .patternLine(" T ")
                        .patternLine("SAS")
                        .patternLine("SMS")
                        .patternLine("SRS")
                        .patternLine("SSS")
                        .disallowMirrored());

        GeneratedRecipe STABILIZING_RESONATOR = create(RNSContent.STABILIZING_RESONATOR_BLOCK::get).returns(1)
                .recipe(b -> b
                        .key('T', Blocks.DIAMOND_BLOCK)
                        .key('A', RNSContent.RESONANT_AMETHYST)
                        .key('M', AllItems.PRECISION_MECHANISM)
                        .key('R', RNSContent.RESONATOR_BLOCK)
                        .key('S', AllItems.STURDY_SHEET)
                        .patternLine(" T ")
                        .patternLine("SAS")
                        .patternLine("SMS")
                        .patternLine("SRS")
                        .patternLine("SSS")
                        .disallowMirrored());

        public MechanicalCrafting(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, CreateRNS.MOD_ID);
        }
    }

    public static void register() {
    }
}

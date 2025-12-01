package com.bmaster.createrns;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

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
        GeneratedRecipe SHATTERING_RESONATOR = create(RNSBlocks.SHATTERING_RESONATOR_BLOCK::get).returns(1)
                .recipe(b -> b
                        .key('T', Blocks.REDSTONE_BLOCK)
                        .key('A', RNSItems.RESONANT_AMETHYST)
                        .key('E', AllItems.ELECTRON_TUBE)
                        .key('R', RNSBlocks.RESONATOR_BLOCK)
                        .key('S', AllItems.STURDY_SHEET)
                        .patternLine(" T ")
                        .patternLine("SAS")
                        .patternLine("SES")
                        .patternLine("SRS")
                        .patternLine("SSS")
                        .disallowMirrored());

        GeneratedRecipe STABILIZING_RESONATOR = create(RNSBlocks.STABILIZING_RESONATOR_BLOCK::get).returns(1)
                .recipe(b -> b
                        .key('T', Blocks.DIAMOND_BLOCK)
                        .key('A', RNSItems.RESONANT_AMETHYST)
                        .key('M', AllItems.PRECISION_MECHANISM)
                        .key('R', RNSBlocks.RESONATOR_BLOCK)
                        .key('S', AllItems.STURDY_SHEET)
                        .patternLine(" T ")
                        .patternLine("SAS")
                        .patternLine("SMS")
                        .patternLine("SRS")
                        .patternLine("SSS")
                        .disallowMirrored());

        public MechanicalCrafting(PackOutput output) {
            super(output, CreateRNS.ID);
        }
    }

    public static void register() {
    }
}

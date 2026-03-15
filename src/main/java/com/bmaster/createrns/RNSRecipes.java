package com.bmaster.createrns;

import com.google.common.base.Supplier;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import com.simibubi.create.api.data.recipe.PolishingRecipeGen;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
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
                        .patternLine(" SSS "));

        GeneratedRecipe SHATTERING_RESONATOR = create(RNSBlocks.SHATTERING_RESONATOR::get)
                .recipe(b -> b
                        .key('Q', AllItems.POLISHED_ROSE_QUARTZ)
                        .key('R', RNSBlocks.RESONATOR)
                        .key('A', RNSItems.POLISHED_RESONANT_AMETHYST)
                        .key('B', AllItems.BRASS_SHEET)
                        .key('S', AllItems.STURDY_SHEET)
                        .patternLine("QAQ")
                        .patternLine("QAQ")
                        .patternLine("BRB")
                        .patternLine("SSS"));

        GeneratedRecipe STABILIZING_RESONATOR = create(RNSBlocks.STABILIZING_RESONATOR::get)
                .recipe(b -> b
                        .key('D', Items.DIAMOND)
                        .key('R', RNSBlocks.RESONATOR)
                        .key('A', RNSItems.POLISHED_RESONANT_AMETHYST)
                        .key('B', AllItems.BRASS_SHEET)
                        .key('S', AllItems.STURDY_SHEET)
                        .patternLine("DAD")
                        .patternLine("DAD")
                        .patternLine("BRB")
                        .patternLine("SSS"));

        protected GeneratedRecipeBuilder create(Supplier<ItemLike> result) {
            return new GeneratedRecipeBuilder(result);
        }

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

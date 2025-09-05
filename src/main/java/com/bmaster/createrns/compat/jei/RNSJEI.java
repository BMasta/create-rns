package com.bmaster.createrns.compat.jei;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.RNSRecipes;
import com.bmaster.createrns.mining.recipe.AdvancedMiningRecipe;
import com.bmaster.createrns.mining.recipe.BasicMiningRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@JeiPlugin
public class RNSJEI implements IModPlugin {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "jei_plugin");


    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        reg.addRecipeCategories(new BasicMiningRecipeCategory());
        reg.addRecipeCategories(new AdvancedMiningRecipeCategory());
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var basicRecipes = level.getRecipeManager().getAllRecipesFor(RNSRecipes.BASIC_MINING_TYPE.get());
        var advancedRecipes = level.getRecipeManager().getAllRecipesFor(RNSRecipes.ADVANCED_MINING_TYPE.get());
        reg.addRecipes(BasicMiningRecipeCategory.JEI_RECIPE_TYPE, basicRecipes);
        reg.addRecipes(AdvancedMiningRecipeCategory.JEI_RECIPE_TYPE, advancedRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(new ItemStack(RNSContent.MINER_MK1_BLOCK.get().asItem()), BasicMiningRecipeCategory.JEI_RECIPE_TYPE);
        reg.addRecipeCatalyst(new ItemStack(RNSContent.MINER_MK2_BLOCK.get().asItem()), AdvancedMiningRecipeCategory.JEI_RECIPE_TYPE);
    }
}

package com.bmaster.createrns.compat.jei;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.RNSRecipeTypes;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.infrastructure.ServerConfig;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;

@JeiPlugin
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSJEI implements IModPlugin {
    public static final ResourceLocation ID = CreateRNS.asResource("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        reg.addRecipeCategories(new MiningRecipeCategory(reg.getJeiHelpers().getGuiHelper()));
        reg.addRecipeCategories(new CatalystInfoCategory());
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var recipes = level.getRecipeManager().getAllRecipesFor(RNSRecipeTypes.MINING_RECIPE_TYPE.get());
        // Hide depleted deposit recipe when infinite deposits are configured
        if (ServerConfig.infiniteDeposits) {
            recipes = recipes.stream()
                    .filter(r -> r.getDepositBlock() != RNSBlocks.DEPLETED_DEPOSIT.get())
                    .toList();
        }
        reg.addRecipes(MiningRecipeCategory.JEI_RECIPE_TYPE, recipes);

        var crsRegistry = level.registryAccess().registryOrThrow(CatalystRequirementSet.REGISTRY_KEY);
        var catalystInfoRecipes = crsRegistry.stream()
                .sorted(Comparator.comparingInt(crs -> crs.displayPriority))
                .toList();
        reg.addRecipes(CatalystInfoCategory.JEI_RECIPE_TYPE, catalystInfoRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        for (var cs : MiningRecipeCategory.CATALYSTS) {
            reg.addRecipeCatalyst(cs.get(), MiningRecipeCategory.JEI_RECIPE_TYPE);
        }

        for (var cs : CatalystInfoCategory.CATALYSTS) {
            reg.addRecipeCatalyst(cs.get(), CatalystInfoCategory.JEI_RECIPE_TYPE);
        }
    }
}

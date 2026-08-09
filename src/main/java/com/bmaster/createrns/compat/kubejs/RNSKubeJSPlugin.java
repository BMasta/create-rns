package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.client.LangEventJS;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.recipe.RecipesEventJS;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import dev.latvian.mods.kubejs.util.ConsoleJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSPlugin extends KubeJSPlugin {
    static {
        RNSKubeJSPluginBridge.install(
                RNSKubeJSAssembler::isManagingDeposits,
                RNSKubeJSAssembler::getEnabledDepositBlocks,
                RNSKubeJSAssembler::getSelectedStructureIds
        );
    }

    @Override
    public void init() {
        RegistryInfo.BLOCK.addType(
                CreateRNS.asResource("deposit").toString(),
                DepositBlockKubeBuilder.class,
                DepositBlockKubeBuilder::new);
    }

    @Override
    public void registerEvents() {
        RNSStartupKubeEvents.init();
    }

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        event.register(CreateRNS.asResource("mining"), MiningRecipeKubeSchema.schema());
    }

    @Override
    public void injectRuntimeRecipes(RecipesEventJS event, RecipeManager manager,
                                     Map<ResourceLocation, Recipe<?>> recipesByName) {
        int failures = event.failedCount.get();
        if (failures == 0) return;

        ConsoleJS.SERVER.error(failures + (failures == 1
                ? " mining recipe failed to parse, check KubeJS warnings for details"
                : " mining recipes failed to parse, check KubeJS warnings for details"));
    }

    @Override
    public void generateDataJsons(DataJsonGenerator generator) {
        RNSKubeJSAssembler.fromCurrentEvents().generateData(generator);
    }

    @Override
    public void generateLang(LangEventJS event) {
        RNSKubeJSAssembler.fromCurrentEvents().generateLang(event);
    }

    @Override
    public void clearCaches() {
        RNSKubeJSAssembler.resetCaches();
    }
}

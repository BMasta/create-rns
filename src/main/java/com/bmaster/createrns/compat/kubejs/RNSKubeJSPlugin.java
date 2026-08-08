package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.registry.BuilderTypeRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSPlugin implements KubeJSPlugin {
    static {
        RNSKubeJSPluginBridge.install(
                RNSKubeJSAssembler::isManagingDeposits,
                RNSKubeJSAssembler::getEnabledDepositBlocks,
                RNSKubeJSAssembler::getSelectedStructureIds
        );
    }

    @Override
    public void registerBuilderTypes(BuilderTypeRegistry registry) {
        registry.of(Registries.BLOCK, reg -> reg.add(
                CreateRNS.asResource("deposit"),
                DepositBlockKubeBuilder.class,
                DepositBlockKubeBuilder::new));
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        RNSStartupKubeEvents.init();
    }

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        registry.register(CreateRNS.asResource("mining"), MiningRecipeKubeSchema.schema());
    }

    @Override
    public void generateData(KubeDataGenerator generator) {
        RNSKubeJSAssembler.fromCurrentEvents().generateData(generator);
    }

    @Override
    public void generateLang(dev.latvian.mods.kubejs.client.LangKubeEvent event) {
        RNSKubeJSAssembler.fromCurrentEvents().generateLang(event);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void clearCaches() {
        RNSKubeJSAssembler.resetCaches();
    }
}

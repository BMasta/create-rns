package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.client.LangEventJS;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

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
    public void generateDataJsons(DataJsonGenerator generator) {
        RNSKubeJSAssembler.resetDepositSelectionCache();
        RNSKubeJSAssembler.fromCurrentEvents().generateData(generator);
    }

    @Override
    public void generateLang(LangEventJS event) {
        RNSKubeJSAssembler.resetDepositSelectionCache();
        RNSKubeJSAssembler.fromCurrentEvents().generateLang(event);
    }
}

package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.client.LangEventJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTestHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSBuilderTestContext {
    private final RNSKubeJSBuilderTestHelper helper;
    private final CatalystsKubeEvent catalysts = new CatalystsKubeEvent();
    private final DepositStructuresKubeEvent depositStructures = new DepositStructuresKubeEvent();
    private final EnableDepositsKubeEvent structureSet =
            new EnableDepositsKubeEvent(() -> RNSKubeJSAssembler.availableStructures(
                    depositStructures.created(), depositStructures.tweaked()));
    private final RNSKubeJSAssembler assembler =
            new RNSKubeJSAssembler(
                    catalysts::created, depositStructures::created, depositStructures::tweaked, structureSet);

    public RNSKubeJSBuilderTestContext(GameTestHelper helper) {
        this.helper = new RNSKubeJSBuilderTestHelper(helper);
    }

    public RNSKubeJSBuilderTestHelper helper() {
        return helper;
    }

    public CatalystsKubeEvent catalysts() {
        return catalysts;
    }

    public DepositStructuresKubeEvent depositStructures() {
        return depositStructures;
    }

    public EnableDepositsKubeEvent structureSet() {
        return structureSet;
    }

    public RNSKubeJSAssembler assembler() {
        return assembler;
    }

    public MiningRecipeKubeRecipe miningRecipe(String id) {
        return MiningRecipeKubeRecipeTestHelper.create(id);
    }

    public RNSKubeJSBuilderTestData assembleData() {
        var generator = new RNSKubeJSBuilderTestGenerator();
        assembler.generateData(generator);
        return new RNSKubeJSBuilderTestData(generator.generatedById());
    }

    public LangEventJS assembleLang(String langId) {
        var event = new LangEventJS(langId, new LinkedHashMap<>());
        assembler.generateLang(event);
        return event;
    }

    public LangEventJS assembleLang() {
        return assembleLang("en_us");
    }
}

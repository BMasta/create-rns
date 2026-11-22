package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.compat.kubejs.deposit.catalysts.CatalystsKubeEvent;
import com.bmaster.createrns.compat.kubejs.deposit.enable.EnableDepositsKubeEvent;
import com.bmaster.createrns.compat.kubejs.deposit.recipe.MiningRecipeKubeRecipe;
import com.bmaster.createrns.compat.kubejs.deposit.recipe.MiningRecipeKubeRecipeTestHelper;
import com.bmaster.createrns.compat.kubejs.deposit.structures.DepositStructuresKubeEvent;
import com.bmaster.createrns.compat.kubejs.util.KubeStartupError.SourcedError;
import com.bmaster.createrns.compat.kubejs.util.KubeStartupEventProcessor;
import com.bmaster.createrns.data.deposit.DepositResources;
import dev.latvian.mods.kubejs.client.LangKubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTestHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSBuilderTestContext {
    private final RNSKubeJSBuilderTestHelper helper;
    private final CatalystsKubeEvent catalysts = new CatalystsKubeEvent();
    private final DepositStructuresKubeEvent depositStructures = new DepositStructuresKubeEvent();
    private final EnableDepositsKubeEvent structureSet =
            new EnableDepositsKubeEvent(depositStructures);
    private final KubeStartupEventProcessor processor =
            new KubeStartupEventProcessor(catalysts, depositStructures, structureSet);

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

    public KubeStartupEventProcessor processor() {
        return processor;
    }

    public DepositResources resources() {
        return processor.getProcessedEvents().resources();
    }

    public List<SourcedError> validationErrors() {
        return processor.getProcessedEvents().errors();
    }

    public MiningRecipeKubeRecipe miningRecipe(String id) {
        return MiningRecipeKubeRecipeTestHelper.create(id);
    }

    public RNSKubeJSBuilderTestData assembleData() {
        var generator = new RNSKubeJSBuilderTestGenerator();
        processor.generateData(generator);
        return new RNSKubeJSBuilderTestData(generator.generatedById());
    }

    public LangKubeEvent assembleLang(String langId) {
        var event = new LangKubeEvent(langId, new LinkedHashMap<>());
        processor.generateLang(event);
        return event;
    }

    public LangKubeEvent assembleLang() {
        return assembleLang("en_us");
    }
}

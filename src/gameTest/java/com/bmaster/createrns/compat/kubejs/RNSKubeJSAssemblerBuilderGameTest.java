package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class RNSKubeJSAssemblerBuilderGameTest {
    @GameTest(template = "empty16x16")
    public void builderEmissionScenarios(GameTestHelper helper) {
        RNSKubeJSBuilderTestRunner.runAll(helper, RNSKubeJSAssemblerBuilderGameTest.class);
    }

    @RNSKubeJSBuilderTest
    private static void freshAssemblerDoesNotEmitResources(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        helper.assertTrue(context.assembler() != null, "Expected builder test context to create an assembler eagerly");

        var generated = context.assembleData();
        helper.assertValueEqual(generated.size(), 0, "fresh assembler resource count");
    }

    @RNSKubeJSBuilderTest
    private static void catalystBuilderEmitsCatalystAndAttachmentTag(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var catalystId = CreateRNS.asResource("test_kubejs_stone_boost");

        var assembler = context.assembler();
        context.catalysts().create(catalystId.toString())
                .attachment("minecraft:stone", 2)
                .representativeItem("minecraft:stone")
                .displayPriority(123);

        var generated = context.assembleData();
        helper.assertTrue(assembler != null, "Expected the eagerly-created assembler to remain usable after configuration");

        var catalystPath = ResourceLocation.fromNamespaceAndPath(catalystId.getNamespace(),
                "create_rns/catalyst/" + catalystId.getPath());
        helper.assertTrue(generated.hasJson(catalystPath), "Expected generated catalyst JSON");
        helper.assertTrue(generated.hasJson(CreateRNS.asResource("tags/block/miner_attachments")),
                "Expected generated miner attachment tag");

        var catalystJson = generated.jsonObject(catalystPath);
        helper.assertValueEqual(catalystJson.get("display_priority").getAsInt(), 123, "display priority");
        var requirements = catalystJson.getAsJsonArray("requirements");
        helper.assertValueEqual(requirements.size(), 1, "requirement count");
        helper.assertValueEqual(requirements.get(0).getAsJsonObject().get("attachment").getAsString(),
                "minecraft:stone", "attachment block id");

        var tagJson = generated.jsonObject(CreateRNS.asResource("tags/block/miner_attachments"));
        helper.assertFalse(tagJson.get("replace").getAsBoolean(), "Attachment tag should append instead of replace");
        var values = tagJson.getAsJsonArray("values");
        helper.assertValueEqual(values.size(), 1, "attachment tag value count");
        helper.assertValueEqual(values.get(0).getAsString(), "minecraft:stone", "attachment tag block");
    }

    @RNSKubeJSBuilderTest
    private static void structureSetBuilderCanSeeCustomStructuresConfiguredLater(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_kubejs_custom_structure");

        var overworld = context.structureSet().overworld();
        context.depositStructures().create(structureId.toString())
                .block("minecraft:ancient_debris")
                .scannerIcon("minecraft:iron_ingot")
                .nbt("create_rns:ore_deposit_small", 1);
        overworld.deposit(structureId.toString(), 7, true);

        var generated = context.assembleData();
        helper.assertTrue(generated.hasJson(CreateRNS.asResource("worldgen/structure_set/deposits")),
                "Expected overworld structure-set JSON");
        helper.assertFalse(generated.hasJson(CreateRNS.asResource("worldgen/structure_set/nether_deposits")),
                "Did not expect nether structure-set JSON without calling event.nether()");
        helper.assertTrue(generated.hasJson(ResourceLocation.fromNamespaceAndPath(structureId.getNamespace(),
                "create_rns/deposit_spec/" + structureId.getPath())), "Expected custom deposit spec JSON");
        helper.assertTrue(generated.hasJson(CreateRNS.asResource("worldgen/structure/" + structureId.getPath())),
                "Expected custom deposit structure JSON");
        helper.assertTrue(generated.hasJson(CreateRNS.asResource(
                "worldgen/processor_list/replace_with_" + structureId.getNamespace() + "_" + structureId.getPath())),
                "Expected custom processor-list JSON");

        var structureSetJson = generated.jsonObject(CreateRNS.asResource("worldgen/structure_set/deposits"));
        var structures = structureSetJson.getAsJsonArray("structures");
        helper.assertValueEqual(structures.size(), 1, "overworld structure-set entry count");
        helper.assertValueEqual(structures.get(0).getAsJsonObject().get("structure").getAsString(),
                structureId.toString(), "overworld selected structure id");

        var depositsTagJson = generated.jsonObject(CreateRNS.asResource("tags/worldgen/structure/deposits"));
        var values = depositsTagJson.getAsJsonArray("values");
        helper.assertValueEqual(values.size(), 1, "deposit structure tag entry count");
        helper.assertValueEqual(values.get(0).getAsString(), structureId.toString(), "deposit structure tag id");
    }
}

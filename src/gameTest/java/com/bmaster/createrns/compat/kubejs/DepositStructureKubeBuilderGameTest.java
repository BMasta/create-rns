package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.google.gson.JsonArray;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestAssertions.*;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class DepositStructureKubeBuilderGameTest {
    private static final ResourceLocation DEFAULT_TEMPLATE_SMALL =
            ResourceLocation.fromNamespaceAndPath("create_rns", "ore_deposit_small");
    private static final ResourceLocation DEFAULT_TEMPLATE_MEDIUM =
            ResourceLocation.fromNamespaceAndPath("create_rns", "ore_deposit_medium");
    private static final ResourceLocation DEFAULT_TEMPLATE_LARGE =
            ResourceLocation.fromNamespaceAndPath("create_rns", "ore_deposit_large");

    @GameTest(template = "empty16x16")
    public void structureBuilderScenarios(GameTestHelper helper) {
        RNSKubeJSBuilderTestRunner.runAll(helper, DepositStructureKubeBuilderGameTest.class);
    }

    @RNSKubeJSBuilderTest
    private static void tweakInheritsBuiltInValuesWithoutRequiringStructureSetConfiguration(
            RNSKubeJSBuilderTestContext context
    ) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("deposit_iron");

        context.depositStructures().tweak(structureId.toString());

        var generated = context.assembleData();
        var depositSpecJson = generated.jsonObject(depositSpecPath(structureId));
        var structureJson = generated.jsonObject(structurePath(structureId));
        var structures = structureJson.getAsJsonArray("structures");

        assertArrayStrings(helper, depositSpecJson.get("scanner_icon_item"), "inherited scanner icons",
                "#forge:raw_materials/iron", "#forge:ores/iron", "#forge:ingots/iron", "#forge:nuggets/iron");
        helper.assertValueEqual(depositSpecJson.get("map_icon_item").getAsString(),
                "create_rns:iron_deposit_block", "inherited map icon");
        helper.assertFalse(depositSpecJson.has("dimension"),
                "Overworld built-in tweak should preserve omitted dimension");
        helper.assertValueEqual(structureJson.get("height").getAsInt(), -8, "inherited height");
        helper.assertValueEqual(structures.size(), 2, "inherited template count");
        assertStructureEntry(helper, structures, 0, DEFAULT_TEMPLATE_MEDIUM.toString(), 70,
                "create_rns:replace_with_create_rns_iron_deposit_block");
        assertStructureEntry(helper, structures, 1, DEFAULT_TEMPLATE_LARGE.toString(), 30,
                "create_rns:replace_with_create_rns_iron_deposit_block");
        helper.assertFalse(generated.hasJson(structureSetPath("deposits")),
                "A tweak without a weight change should not replace the default structure set");
    }

    @RNSKubeJSBuilderTest
    private static void tweakListMethodsReplaceBuiltInListsAndAccumulateInOrder(
            RNSKubeJSBuilderTestContext context
    ) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("deposit_iron");

        context.depositStructures().tweak(structureId.toString())
                .height(-27)
                .scannerIcon("mymod:refined_iron")
                .scannerIcon("minecraft:raw_iron")
                .mapIcon("minecraft:compass")
                .mapIcon("minecraft:iron_ingot")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 7)
                .nbt(DEFAULT_TEMPLATE_LARGE.toString(), 3);

        var generated = context.assembleData();
        var depositSpecJson = generated.jsonObject(depositSpecPath(structureId));
        var structureJson = generated.jsonObject(structurePath(structureId));
        var structures = structureJson.getAsJsonArray("structures");

        assertArrayStrings(helper, depositSpecJson.get("scanner_icon_item"), "replacement scanner icons",
                "mymod:refined_iron", "minecraft:raw_iron");
        assertArrayStrings(helper, depositSpecJson.get("map_icon_item"), "replacement map icons",
                "minecraft:compass", "minecraft:iron_ingot");
        helper.assertValueEqual(structureJson.get("height").getAsInt(), -27, "replacement height");
        helper.assertValueEqual(structures.size(), 2, "replacement template count");
        assertStructureEntry(helper, structures, 0, DEFAULT_TEMPLATE_SMALL.toString(), 7,
                "create_rns:replace_with_create_rns_iron_deposit_block");
        assertStructureEntry(helper, structures, 1, DEFAULT_TEMPLATE_LARGE.toString(), 3,
                "create_rns:replace_with_create_rns_iron_deposit_block");
    }

    @RNSKubeJSBuilderTest
    private static void tweakPresetReplacesBuiltInPlacementTemplatesAndDefaultWeight(
            RNSKubeJSBuilderTestContext context
    ) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("deposit_iron");

        context.depositStructures().tweak(structureId.toString())
                .preset("overworld_rare");

        var generated = context.assembleData();
        var structureJson = generated.jsonObject(structurePath(structureId));
        var structures = structureJson.getAsJsonArray("structures");
        var structureSetEntries = generated.jsonObject(structureSetPath("deposits"))
                .getAsJsonArray("structures");

        helper.assertValueEqual(structureJson.get("height").getAsInt(), -12, "preset height");
        helper.assertValueEqual(structures.size(), 3, "preset template count");
        assertStructureEntry(helper, structures, 0, DEFAULT_TEMPLATE_SMALL.toString(), 70,
                "create_rns:replace_with_create_rns_iron_deposit_block");
        assertStructureEntry(helper, structures, 1, DEFAULT_TEMPLATE_MEDIUM.toString(), 28,
                "create_rns:replace_with_create_rns_iron_deposit_block");
        assertStructureEntry(helper, structures, 2, DEFAULT_TEMPLATE_LARGE.toString(), 2,
                "create_rns:replace_with_create_rns_iron_deposit_block");
        assertStructureSetWeight(helper, structureSetEntries, structureId, 20);
    }

    @RNSKubeJSBuilderTest
    private static void tweakRejectsUnknownDuplicateAndBlockReplacement(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("deposit_iron");
        var builder = context.depositStructures().tweak(structureId.toString());

        assertThrows(helper, IllegalArgumentException.class, "Unknown built-in deposit structure",
                () -> context.depositStructures().tweak("create_rns:missing_deposit"));
        assertThrows(helper, IllegalStateException.class, "already tweaked",
                () -> context.depositStructures().tweak(structureId.toString()));
        assertThrows(helper, UnsupportedOperationException.class, "block cannot be changed",
                () -> builder.block("minecraft:gold_block"));
        assertThrows(helper, IllegalArgumentException.class, "must be modified with tweak",
                () -> context.depositStructures().create(structureId.toString()));
    }

    @RNSKubeJSBuilderTest
    private static void assemblyRejectsMissingDepositBlock(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_missing_deposit_block");

        context.depositStructures().create(structureId.toString())
                .scannerIcon("minecraft:raw_iron")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        assertThrows(helper, IllegalStateException.class, "must specify a deposit block", context::assembleData);
    }

    @RNSKubeJSBuilderTest
    private static void assemblyRejectsMissingScannerIcon(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_missing_scanner_icon");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:raw_iron_block")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        assertThrows(helper, IllegalStateException.class, "must specify at least one scanner icon", context::assembleData);
    }

    @RNSKubeJSBuilderTest
    private static void assemblyRejectsMissingTemplate(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_missing_template");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:raw_iron_block")
                .scannerIcon("minecraft:raw_iron");
        context.structureSet().overworld().deposit(structureId.toString(), false);

        assertThrows(helper, IllegalStateException.class, "must specify at least one nbt template", context::assembleData);
    }

    @RNSKubeJSBuilderTest
    private static void blockAndOptionalDefaultsSerializeCorrectly(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("deposit_test_default_structure");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:raw_iron_block")
                .scannerIcon("minecraft:raw_iron")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        var generated = context.assembleData();
        var depositSpecJson = generated.jsonObject(depositSpecPath(structureId));
        var processorJson = generated.jsonObject(processorPath(structureId));
        var structureJson = generated.jsonObject(structurePath(structureId));
        var lang = context.assembleLang();

        helper.assertValueEqual(depositSpecJson.get("scanner_icon_item").getAsString(), "minecraft:raw_iron",
                "default scanner icon");
        helper.assertValueEqual(depositSpecJson.get("map_icon_item").getAsString(), "minecraft:raw_iron_block",
                "default map icon");
        helper.assertValueEqual(depositSpecJson.get("structure").getAsString(), structureId.toString(),
                "deposit spec structure id");
        helper.assertValueEqual(depositSpecJson.get("dimension").getAsString(), "minecraft:overworld",
                "default structure dimension");

        helper.assertValueEqual(structureJson.get("type").getAsString(), "create_rns:deposit", "structure type");
        helper.assertValueEqual(structureJson.get("biomes").getAsString(), "#create_rns:has_deposit", "biome tag");
        helper.assertValueEqual(structureJson.get("placement_strategy").getAsString(), "overworld", "placement strategy");
        helper.assertValueEqual(structureJson.get("height").getAsInt(), -8, "default overworld height");
        var structures = structureJson.getAsJsonArray("structures");
        helper.assertValueEqual(structures.size(), 1, "template count");
        assertStructureEntry(helper, structures, 0, DEFAULT_TEMPLATE_SMALL.toString(), 1,
                "create_rns:replace_with_create_rns_deposit_test_default_structure");

        var processors = processorJson.getAsJsonArray("processors");
        helper.assertValueEqual(processors.size(), 1, "processor count");
        var rule = processors.get(0).getAsJsonObject().getAsJsonArray("rules").get(0).getAsJsonObject();
        helper.assertValueEqual(rule.getAsJsonObject("input_predicate").get("block").getAsString(), "minecraft:end_stone",
                "processor input block");
        helper.assertValueEqual(rule.getAsJsonObject("output_state").get("Name").getAsString(), "minecraft:raw_iron_block",
                "processor output block");

        helper.assertTrue(lang.map.isEmpty(), "Expected no lang entries without displayName()");
    }

    @RNSKubeJSBuilderTest
    private static void scannerIconHelpersAndMapIconsAccumulateInOrder(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_helper_icons");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:copper_block")
                .displayName("Helper Icons")
                .scannerIconMetal("copper")
                .scannerIconGem("diamond")
                .scannerIconDust("redstone")
                .mapIcon("minecraft:compass")
                .mapIcon("#forge:gems/diamond")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        var generated = context.assembleData();
        var depositSpecJson = generated.jsonObject(depositSpecPath(structureId));
        var lang = context.assembleLang();

        assertArrayStrings(helper, depositSpecJson.get("scanner_icon_item"), "scanner icon candidates",
                "#forge:raw_materials/copper",
                "#forge:ores/copper",
                "#forge:ingots/copper",
                "#forge:nuggets/copper",
                "#forge:gems/diamond",
                "#forge:dusts/redstone");
        assertArrayStrings(helper, depositSpecJson.get("map_icon_item"), "map icon candidates",
                "minecraft:compass",
                "#forge:gems/diamond");
        assertLangValue(helper, lang, "create_rns", "en_us", "create_rns.structure.test_helper_icons", "Helper Icons");
    }

    @RNSKubeJSBuilderTest
    private static void templatesAccumulateInDeclaredOrder(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_template_accumulation");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:gold_block")
                .scannerIcon("minecraft:raw_gold")
                .mapIcon("minecraft:gold_ingot")
                .mapIcon("#forge:ingots/gold")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 7)
                .nbt(DEFAULT_TEMPLATE_LARGE.toString(), 3);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        var generated = context.assembleData();
        var depositSpecJson = generated.jsonObject(depositSpecPath(structureId));
        var structureJson = generated.jsonObject(structurePath(structureId));
        var structures = structureJson.getAsJsonArray("structures");

        assertArrayStrings(helper, depositSpecJson.get("map_icon_item"), "explicit map icons",
                "minecraft:gold_ingot",
                "#forge:ingots/gold");
        helper.assertValueEqual(structures.size(), 2, "template count");
        assertStructureEntry(helper, structures, 0, DEFAULT_TEMPLATE_SMALL.toString(), 7,
                "create_rns:replace_with_create_rns_test_template_accumulation");
        assertStructureEntry(helper, structures, 1, DEFAULT_TEMPLATE_LARGE.toString(), 3,
                "create_rns:replace_with_create_rns_test_template_accumulation");
    }

    @RNSKubeJSBuilderTest
    private static void explicitAssignmentsUseLastValueAndSingleMapIconStaysScalar(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("deposit_test_last_wins");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:iron_block")
                .block("minecraft:gold_block")
                .displayName("Old Name")
                .displayName("Final Name")
                .height(7)
                .height(23)
                .scannerIcon("minecraft:raw_gold")
                .mapIcon("minecraft:clock")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        var generated = context.assembleData();
        var depositSpecJson = generated.jsonObject(depositSpecPath(structureId));
        var processorJson = generated.jsonObject(processorPath(structureId));
        var structureJson = generated.jsonObject(structurePath(structureId));
        var lang = context.assembleLang();

        helper.assertTrue(depositSpecJson.get("map_icon_item").isJsonPrimitive(),
                "A single explicit map icon should stay a JSON string");
        helper.assertValueEqual(depositSpecJson.get("map_icon_item").getAsString(), "minecraft:clock",
                "single explicit map icon");
        helper.assertValueEqual(structureJson.get("height").getAsInt(), 23, "last explicit height wins");

        var rule = processorJson.getAsJsonArray("processors").get(0).getAsJsonObject()
                .getAsJsonArray("rules").get(0).getAsJsonObject();
        helper.assertValueEqual(rule.getAsJsonObject("output_state").get("Name").getAsString(), "minecraft:gold_block",
                "last explicit deposit block wins");
        assertLangValue(helper, lang, "create_rns", "en_us",
                "create_rns.structure.deposit_test_last_wins", "Final Name");
    }

    @RNSKubeJSBuilderTest
    private static void idsWithDepositPrefixTrimThatPrefixInDepositSpecPath(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("deposit_prefixed_path");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:iron_block")
                .scannerIcon("minecraft:raw_iron")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        var generated = context.assembleData();
        helper.assertTrue(generated.hasJson(depositSpecPath(structureId)), "Expected trimmed deposit spec path");
        helper.assertFalse(generated.hasJson(ResourceLocation.fromNamespaceAndPath("create_rns",
                        "create_rns/deposit_spec/" + structureId.getPath())),
                "Did not expect an untrimmed deposit spec path");
    }

    @RNSKubeJSBuilderTest
    private static void overworldPresetsSerializeTheirDefaultFamilies(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var commonId = CreateRNS.asResource("test_overworld_common_preset");
        var uncommonId = CreateRNS.asResource("test_overworld_uncommon_preset");
        var rareId = CreateRNS.asResource("test_overworld_rare_preset");

        context.depositStructures().create(commonId.toString())
                .block("minecraft:iron_block")
                .scannerIcon("minecraft:raw_iron")
                .preset("overworld_common");
        context.depositStructures().create(uncommonId.toString())
                .block("minecraft:copper_block")
                .scannerIcon("minecraft:raw_copper")
                .preset("overworld_uncommon");
        context.depositStructures().create(rareId.toString())
                .block("minecraft:diamond_block")
                .scannerIcon("minecraft:diamond")
                .preset("overworld_rare");
        context.structureSet().overworld()
                .deposit(commonId.toString())
                .deposit(uncommonId.toString())
                .deposit(rareId.toString());

        var generated = context.assembleData();
        var structureSetEntries = generated.jsonObject(structureSetPath("deposits")).getAsJsonArray("structures");
        assertPresetStructure(helper, generated, commonId, "#create_rns:has_deposit", "overworld", -8,
                new PresetTemplate(DEFAULT_TEMPLATE_MEDIUM, 70),
                new PresetTemplate(DEFAULT_TEMPLATE_LARGE, 30));
        assertPresetStructure(helper, generated, uncommonId, "#create_rns:has_deposit", "overworld", -10,
                new PresetTemplate(DEFAULT_TEMPLATE_SMALL, 30),
                new PresetTemplate(DEFAULT_TEMPLATE_MEDIUM, 60),
                new PresetTemplate(DEFAULT_TEMPLATE_LARGE, 10));
        assertPresetStructure(helper, generated, rareId, "#create_rns:has_deposit", "overworld", -12,
                new PresetTemplate(DEFAULT_TEMPLATE_SMALL, 70),
                new PresetTemplate(DEFAULT_TEMPLATE_MEDIUM, 28),
                new PresetTemplate(DEFAULT_TEMPLATE_LARGE, 2));
        assertStructureSetEntry(helper, structureSetEntries, 0, commonId.toString(), 50);
        assertStructureSetEntry(helper, structureSetEntries, 1, uncommonId.toString(), 35);
        assertStructureSetEntry(helper, structureSetEntries, 2, rareId.toString(), 20);
    }

    @RNSKubeJSBuilderTest
    private static void netherPresetsSerializeTheirDefaultFamilies(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var commonId = CreateRNS.asResource("test_nether_common_preset");
        var uncommonId = CreateRNS.asResource("test_nether_uncommon_preset");
        var rareId = CreateRNS.asResource("test_nether_rare_preset");

        context.depositStructures().create(commonId.toString())
                .block("minecraft:gold_block")
                .scannerIcon("minecraft:raw_gold")
                .preset("nether_common");
        context.depositStructures().create(uncommonId.toString())
                .block("minecraft:ancient_debris")
                .scannerIcon("minecraft:netherite_scrap")
                .preset("nether_uncommon");
        context.depositStructures().create(rareId.toString())
                .block("minecraft:quartz_block")
                .scannerIcon("minecraft:quartz")
                .preset("nether_rare");
        context.structureSet().nether()
                .deposit(commonId.toString())
                .deposit(uncommonId.toString())
                .deposit(rareId.toString());

        var generated = context.assembleData();
        var structureSetEntries = generated.jsonObject(structureSetPath("nether_deposits")).getAsJsonArray("structures");
        assertPresetStructure(helper, generated, commonId, "#create_rns:has_deposit_nether", "nether", -4,
                new PresetTemplate(DEFAULT_TEMPLATE_MEDIUM, 70),
                new PresetTemplate(DEFAULT_TEMPLATE_LARGE, 30));
        assertPresetStructure(helper, generated, uncommonId, "#create_rns:has_deposit_nether", "nether", -4,
                new PresetTemplate(DEFAULT_TEMPLATE_SMALL, 30),
                new PresetTemplate(DEFAULT_TEMPLATE_MEDIUM, 60),
                new PresetTemplate(DEFAULT_TEMPLATE_LARGE, 10));
        assertPresetStructure(helper, generated, rareId, "#create_rns:has_deposit_nether", "nether", -4,
                new PresetTemplate(DEFAULT_TEMPLATE_SMALL, 70),
                new PresetTemplate(DEFAULT_TEMPLATE_MEDIUM, 28),
                new PresetTemplate(DEFAULT_TEMPLATE_LARGE, 2));
        assertStructureSetEntry(helper, structureSetEntries, 0, commonId.toString(), 50);
        assertStructureSetEntry(helper, structureSetEntries, 1, uncommonId.toString(), 35);
        assertStructureSetEntry(helper, structureSetEntries, 2, rareId.toString(), 20);
    }

    @RNSKubeJSBuilderTest
    private static void manualTemplatesCanExtendPresetTemplates(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_preset_then_manual_template");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:emerald_block")
                .scannerIcon("minecraft:emerald")
                .preset("overworld_common")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 5);
        context.structureSet().overworld().deposit(structureId.toString());

        var generated = context.assembleData();
        var structures = generated.jsonObject(structurePath(structureId)).getAsJsonArray("structures");

        helper.assertValueEqual(structures.size(), 3, "preset plus manual template count");
        assertStructureEntry(helper, structures, 0, DEFAULT_TEMPLATE_MEDIUM.toString(), 70,
                "create_rns:replace_with_create_rns_test_preset_then_manual_template");
        assertStructureEntry(helper, structures, 1, DEFAULT_TEMPLATE_LARGE.toString(), 30,
                "create_rns:replace_with_create_rns_test_preset_then_manual_template");
        assertStructureEntry(helper, structures, 2, DEFAULT_TEMPLATE_SMALL.toString(), 5,
                "create_rns:replace_with_create_rns_test_preset_then_manual_template");
    }

    @RNSKubeJSBuilderTest
    private static void assembleLangUsesTheSameValidationAsAssembleData(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_lang_validation");

        context.depositStructures().create(structureId.toString())
                .scannerIcon("minecraft:raw_iron")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        assertThrows(helper, IllegalStateException.class, "must specify a deposit block", context::assembleLang);
    }

    @RNSKubeJSBuilderTest
    private static void presetClearsManualTemplatesAndAssignsNetherDefaults(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_nether_preset");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:ancient_debris")
                .scannerIcon("minecraft:netherite_scrap")
                .nbt(DEFAULT_TEMPLATE_LARGE.toString(), 99)
                .preset("nether_uncommon");
        context.structureSet().nether().deposit(structureId.toString());

        var generated = context.assembleData();
        var depositSpecJson = generated.jsonObject(depositSpecPath(structureId));
        var structureJson = generated.jsonObject(structurePath(structureId));
        var structureSetJson = generated.jsonObject(structureSetPath("nether_deposits"));
        var structures = structureJson.getAsJsonArray("structures");

        helper.assertValueEqual(depositSpecJson.get("dimension").getAsString(), "minecraft:the_nether",
                "nether deposit dimension");
        helper.assertValueEqual(structureJson.get("biomes").getAsString(), "#create_rns:has_deposit_nether",
                "nether biome tag");
        helper.assertValueEqual(structureJson.get("placement_strategy").getAsString(), "nether", "nether placement");
        helper.assertValueEqual(structureJson.get("height").getAsInt(), -4, "nether preset height");
        helper.assertValueEqual(structures.size(), 3, "nether preset template count");
        assertStructureEntry(helper, structures, 0, DEFAULT_TEMPLATE_SMALL.toString(), 30,
                "create_rns:replace_with_create_rns_test_nether_preset");
        assertStructureEntry(helper, structures, 1, DEFAULT_TEMPLATE_MEDIUM.toString(), 60,
                "create_rns:replace_with_create_rns_test_nether_preset");
        assertStructureEntry(helper, structures, 2, DEFAULT_TEMPLATE_LARGE.toString(), 10,
                "create_rns:replace_with_create_rns_test_nether_preset");

        var structureSetEntries = structureSetJson.getAsJsonArray("structures");
        helper.assertValueEqual(structureSetEntries.size(), 1, "nether structure-set entry count");
        assertStructureSetEntry(helper, structureSetEntries, 0, structureId.toString(), 35);
    }

    @RNSKubeJSBuilderTest
    private static void explicitHeightAndWeightOverridePresetValues(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_preset_override");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:emerald_block")
                .scannerIcon("minecraft:emerald")
                .preset("overworld_common")
                .height(23)
                .weight(9);
        context.structureSet().overworld().deposit(structureId.toString());

        var generated = context.assembleData();
        var structureJson = generated.jsonObject(structurePath(structureId));
        var structureSetJson = generated.jsonObject(structureSetPath("deposits"));
        var structures = structureJson.getAsJsonArray("structures");
        var structureSetEntries = structureSetJson.getAsJsonArray("structures");

        helper.assertValueEqual(structureJson.get("height").getAsInt(), 23, "overridden height");
        helper.assertValueEqual(structures.size(), 2, "common preset template count");
        assertStructureEntry(helper, structures, 0, DEFAULT_TEMPLATE_MEDIUM.toString(), 70,
                "create_rns:replace_with_create_rns_test_preset_override");
        assertStructureEntry(helper, structures, 1, DEFAULT_TEMPLATE_LARGE.toString(), 30,
                "create_rns:replace_with_create_rns_test_preset_override");

        helper.assertValueEqual(structureSetEntries.size(), 1, "structure-set entry count");
        assertStructureSetEntry(helper, structureSetEntries, 0, structureId.toString(), 9);
    }

    @RNSKubeJSBuilderTest
    private static void weightRejectsNonPositiveValues(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.depositStructures().create(CreateRNS.asResource("test_invalid_weight").toString());

        assertThrows(helper, IllegalArgumentException.class, "must be positive", () -> builder.weight(0));
        assertThrows(helper, IllegalArgumentException.class, "must be positive", () -> builder.weight(-1));
    }

    @RNSKubeJSBuilderTest
    private static void nbtRejectsNonPositiveTemplateWeights(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.depositStructures().create(CreateRNS.asResource("test_invalid_nbt_weight").toString());

        assertThrows(helper, IllegalArgumentException.class, "must be positive",
                () -> builder.nbt(DEFAULT_TEMPLATE_SMALL.toString(), 0));
        assertThrows(helper, IllegalArgumentException.class, "must be positive",
                () -> builder.nbt(DEFAULT_TEMPLATE_SMALL.toString(), -1));
    }

    @RNSKubeJSBuilderTest
    private static void presetRejectsUnknownIds(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.depositStructures().create(CreateRNS.asResource("test_unknown_preset").toString());

        assertThrows(helper, IllegalArgumentException.class, "Unknown deposit preset",
                () -> builder.preset("moon_legendary"));
    }

    private static void assertPresetStructure(
            RNSKubeJSBuilderTestHelper helper, RNSKubeJSBuilderTestData generated, ResourceLocation structureId,
            String biomeTag, String placementStrategy, int height, PresetTemplate... templates
    ) {
        var structureJson = generated.jsonObject(structurePath(structureId));
        var structures = structureJson.getAsJsonArray("structures");

        helper.assertValueEqual(structureJson.get("biomes").getAsString(), biomeTag, "preset biome tag");
        helper.assertValueEqual(structureJson.get("placement_strategy").getAsString(),
                placementStrategy, "preset placement strategy");
        helper.assertValueEqual(structureJson.get("height").getAsInt(), height, "preset height");
        helper.assertValueEqual(structures.size(), templates.length, "preset template count");

        for (var i = 0; i < templates.length; i++) {
            assertStructureEntry(helper, structures, i, templates[i].template().toString(), templates[i].weight(),
                    "create_rns:replace_with_" + structureId.getNamespace() + "_" + structureId.getPath());
        }
    }

    private static void assertStructureSetWeight(
            RNSKubeJSBuilderTestHelper helper, JsonArray structures, ResourceLocation structureId, int expectedWeight
    ) {
        for (var element : structures) {
            var structure = element.getAsJsonObject();
            if (!structure.get("structure").getAsString().equals(structureId.toString())) continue;

            helper.assertValueEqual(structure.get("weight").getAsInt(), expectedWeight, "tweaked structure-set weight");
            return;
        }

        helper.assertTrue(false, "Missing structure-set entry for " + structureId);
    }

    private record PresetTemplate(ResourceLocation template, int weight) {
    }
}

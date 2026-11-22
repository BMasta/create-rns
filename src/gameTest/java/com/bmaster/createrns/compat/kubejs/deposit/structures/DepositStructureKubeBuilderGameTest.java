package com.bmaster.createrns.compat.kubejs.deposit.structures;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.kubejs.*;
import com.bmaster.createrns.compat.kubejs.deposit.catalysts.CatalystsKubeEvent;
import com.bmaster.createrns.compat.kubejs.deposit.enable.EnableDepositsKubeEvent;
import com.bmaster.createrns.compat.kubejs.util.KubeStartupError;
import com.bmaster.createrns.compat.kubejs.util.KubeStartupEventProcessor;
import com.bmaster.createrns.content.deposit.spec.DepositSpec;
import com.bmaster.createrns.data.deposit.DepositResources;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import com.bmaster.createrns.util.CodecHelper;
import com.google.gson.JsonArray;
import dev.latvian.mods.kubejs.script.SourceLine;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

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
    private static void eventsGatherTheirDefinitionsWithoutAnAssembler(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var catalysts = new CatalystsKubeEvent();
        var structures = new DepositStructuresKubeEvent();
        var enable = new EnableDepositsKubeEvent(structures);
        var customId = CreateRNS.asResource("test_event_owned_structure");
        var ironId = CreateRNS.asResource("deposit_iron");
        catalysts.create("create_rns:test_event_owned_catalyst").fluid("water", 1).displayName("Water Catalyst");
        enable.nether().deposit(customId.toString(), false);
        structures.create(customId.toString()).block("minecraft:gold_block").scannerIcon("minecraft:gold_ingot")
                .preset("overworld_common").displayName("Selected Gold");
        structures.tweak(ironId.toString()).weight(9);

        catalysts.assertValid();
        structures.assertValid();
        enable.assertValid();
        var selection = enable.selectedStructuresByDimension();
        var resources = new DepositResources();
        catalysts.gatherResources(resources);
        structures.gatherResources(resources, selection);
        enable.gatherResources(resources, selection);
        helper.assertValueEqual(resources.catalysts.getFirst().id().toString(),
                "create_rns:test_event_owned_catalyst", "Catalyst event gathers its own definitions");
        helper.assertValueEqual(resources.getSpec(customId).dimension(), Level.NETHER,
                "Structure event uses the enable event's selection");
        var overworldSet = resources.structureSets.stream()
                .filter(set -> set.id().equals(CreateRNS.asResource("deposits"))).findFirst().orElseThrow();
        helper.assertValueEqual(overworldSet.structureIds().stream()
                .filter(entry -> entry.structureId().equals(ironId)).findFirst().orElseThrow().weight(),
                9, "Default set retains tweaked weight");
        var netherSet = resources.structureSets.stream()
                .filter(set -> set.id().equals(CreateRNS.asResource("nether_deposits"))).findFirst().orElseThrow();
        helper.assertTrue(netherSet.structureIds().isEmpty(), "Enable event gathers a scan-only set");
        helper.assertTrue(resources.structureTags.getFirst().structureIds().stream().anyMatch(entry -> entry.id().equals(customId)),
                "Selected structure tag");
        helper.assertTrue(resources.langEntries.contains(new DepositResources.LangEntry("create_rns",
                DepositResources.structureLangKey(customId), "Selected Gold")), "Structure translation is gathered");
        helper.assertTrue(resources.langEntries.contains(new DepositResources.LangEntry("create_rns",
                "create_rns.catalyst.test_event_owned_catalyst.name", "Water Catalyst")), "Catalyst translation is gathered");
    }
    @RNSKubeJSBuilderTest
    private static void languageFirstInitializesOneIndependentResourceSnapshot(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var id = CreateRNS.asResource("test_resource_snapshot");
        var structure = context.depositStructures().create(id.toString())
                .block("minecraft:gold_block").scannerIcon("minecraft:gold_ingot")
                .preset("overworld_common").displayName("Original Name");
        context.structureSet().nether().deposit(id.toString(), false);
        var lang = context.assembleLang();
        int specCount = context.resources().specs.size();
        helper.assertValueEqual(context.resources().structures.size(), 1, "Language initializes structure records");
        helper.assertValueEqual(context.resources().getSpec(id).dimension(), Level.NETHER, "Final selected dimension");
        assertLangValue(helper, lang, "create_rns", "en_us", DepositResources.structureLangKey(id), "Original Name");

        // Generation consumes the completed snapshot, not subsequent changes to the builder.
        structure.height(31).displayName("Later Name").scannerIcon("minecraft:diamond");
        var first = context.assembleData();
        var second = context.assembleData();
        helper.assertValueEqual(first.ids(), second.ids(), "Repeated output contains the same files");
        for (var path : first.ids()) helper.assertValueEqual(first.json(path), second.json(path), "Stable output " + path);
        helper.assertValueEqual(first.jsonObject(structurePath(id)).get("height").getAsInt(), -8,
                "Recorded height stays unchanged");
        helper.assertValueEqual(context.resources().getSpec(id).scannerIconCandidates().size(), 1,
                "Candidate lists are copied into records");
        helper.assertValueEqual(context.resources().specs.size(), specCount, "Repeated generation does not append specs");
        assertLangValue(helper, context.assembleLang(), "create_rns", "en_us",
                DepositResources.structureLangKey(id), "Original Name");

        var other = new RNSKubeJSBuilderTestContext(helper.raw());
        other.processor().getProcessedEvents();
        helper.assertTrue(other.resources().specs.isEmpty(), "Independent assemblers do not share definitions");
        helper.assertValueEqual(context.resources().specs.size(), specCount, "Other initialization preserves the first snapshot");
        helper.assertTrue(context.resources() != other.resources(), "Tests use isolated resource containers");
    }

    @RNSKubeJSBuilderTest
    private static void sharedBlocksProduceOneProcessorAndSpecOnlyOverridesStayPartial(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        for (var name : List.of("first", "second")) {
            var id = CreateRNS.asResource("test_shared_processor_" + name);
            context.depositStructures().create(id.toString()).block("minecraft:gold_block")
                    .scannerIcon("minecraft:gold_ingot").preset("overworld_common");
            context.structureSet().overworld().deposit(id.toString(), false);
        }
        var data = context.assembleData();
        helper.assertValueEqual(context.resources().processors.size(), 1, "Shared processor is recorded once");
        helper.assertValueEqual(context.resources().structures.size(), 2, "Only custom structures are emitted");
        var ironId = CreateRNS.asResource("deposit_iron");
        helper.assertTrue(data.hasJson(depositSpecPath(ironId)), "Selection overrides built-in spec visibility");
        helper.assertFalse(data.hasJson(structurePath(ironId)), "Spec override does not rewrite built-in structure");
        helper.assertTrue(context.processor().getProcessedEvents().selectedDepositBlocks().get(Level.OVERWORLD)
                .contains(ResourceLocation.withDefaultNamespace("gold_block")), "Runtime enabled blocks include the selected custom block");
    }

    @RNSKubeJSBuilderTest
    private static void failedGatherDoesNotPublishPartialResources(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var catalysts = new CatalystsKubeEvent() {
            private boolean fail = true;

            @Override
            public void gatherResources(@NotNull DepositResources resources) {
                super.gatherResources(resources);
                if (fail) {
                    fail = false;
                    KubeStartupError.add("failed attempt", SourceLine.UNKNOWN);
                    throw new IllegalStateException("test gathering failure");
                }
                KubeStartupError.add("successful retry", SourceLine.UNKNOWN);
            }
        };
        catalysts.create("create_rns:test_retry_snapshot").fluid("water", 1).displayName("Retry Catalyst");
        var structures = new DepositStructuresKubeEvent();
        var processor = new KubeStartupEventProcessor(catalysts, structures, new EnableDepositsKubeEvent(structures));
        assertThrows(helper, IllegalStateException.class, "test gathering failure", processor::getProcessedEvents);
        var snapshot = processor.getProcessedEvents();
        helper.assertTrue(processor.getProcessedEvents() == snapshot, "Successful retry is cached");
        helper.assertValueEqual(snapshot.resources().catalysts.size(), 1, "Retry publishes one complete snapshot");
        helper.assertValueEqual(snapshot.resources().langEntries.size(), 1, "Retry publishes the translation once");
        helper.assertValueEqual(snapshot.errors().stream().map(KubeStartupError.SourcedError::message).toList(),
                List.of("successful retry"), "Retry retains only its own errors");
        helper.assertTrue(context.validationErrors().isEmpty(), "Independent processors do not share errors");
    }

    @RNSKubeJSBuilderTest
    private static void unchangedTweaksMatchDynamicDatapackResources(RNSKubeJSBuilderTestContext context) {
        for (var structure : DynamicDatapackContent.RESOURCES.structures) {
            context.depositStructures().tweak(structure.id().toString());
        }
        var generated = context.assembleData();
        var providers = List.of(DynamicDatapackContent.depositProcessorLists(),
                DynamicDatapackContent.depositStructures(), DynamicDatapackContent.depositSpecs());
        for (var provider : providers) {
            for (var file : provider.get()) {
                int slash = file.path().indexOf('/');
                var id = ResourceLocation.fromNamespaceAndPath(
                        file.path().substring(0, slash), file.path().substring(slash + 1));
                // Blocks without structures still have processor lists in the default pack.
                if (file.path().contains("/worldgen/processor_list/") && !generated.hasJson(id)) continue;
                context.helper().assertTrue(generated.hasJson(id), "Expected tweaked resource " + id);
                context.helper().assertValueEqual(generated.json(id), file.data(), "provider parity for " + id);
            }
        }
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
                "#c:raw_materials/iron", "#c:ores/iron", "#c:ingots/iron", "#c:nuggets/iron");
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
                .biome("minecraft:badlands")
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
        helper.assertValueEqual(structureJson.get("biomes").getAsString(), "minecraft:badlands",
                "replacement biome");
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
    private static void defaultWeightOverridesPreserveOtherEntriesInBothDimensions(
            RNSKubeJSBuilderTestContext context
    ) {
        var helper = context.helper();
        for (var dimension : List.of(Level.OVERWORLD, Level.NETHER)) {
            var defaults = DynamicDatapackContent.RESOURCES.getScannableStructures(dimension);
            helper.assertFalse(defaults.isEmpty(), "Expected default deposits in " + dimension.location());
            var first = defaults.getFirst();
            context.depositStructures().tweak(first.id().toString()).weight(Objects.requireNonNull(first.weight()) + 1);
        }

        var generated = context.assembleData();
        for (var dimension : List.of(Level.OVERWORLD, Level.NETHER)) {
            var defaults = DynamicDatapackContent.RESOURCES.getScannableStructures(dimension);
            var path = dimension.equals(Level.NETHER) ? "nether_deposits" : "deposits";
            var entries = generated.jsonObject(structureSetPath(path)).getAsJsonArray("structures");
            helper.assertValueEqual(entries.size(), defaults.size(), "All default deposits remain in " + path);
            for (var entry : defaults) {
                int expectedWeight = Objects.requireNonNull(entry.weight()) + (entry == defaults.getFirst() ? 1 : 0);
                assertStructureSetWeight(helper, entries, entry.id(), expectedWeight);
            }
        }
    }

    @RNSKubeJSBuilderTest
    private static void structureFactoriesRejectUnknownAndConflictingIds(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("deposit_iron");
        context.depositStructures().tweak(structureId.toString());

        assertThrows(helper, IllegalArgumentException.class, "Unknown built-in deposit structure",
                () -> context.depositStructures().tweak("create_rns:missing_deposit"));
        assertThrows(helper, IllegalStateException.class, "already tweaked",
                () -> context.depositStructures().tweak(structureId.toString()));
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

        assertValidationError(context, "must specify a deposit block");
        helper.assertFalse(context.assembleData().hasJson(structurePath(structureId)), "Invalid structure omitted");
    }

    @RNSKubeJSBuilderTest
    private static void assemblyRejectsMissingScannerIcon(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_missing_scanner_icon");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:raw_iron_block")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        assertValidationError(context, "must specify at least one scanner icon");
        helper.assertFalse(context.assembleData().hasJson(depositSpecPath(structureId)), "Invalid spec omitted");
    }

    @RNSKubeJSBuilderTest
    private static void assemblyRejectsMissingTemplate(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_missing_template");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:raw_iron_block")
                .scannerIcon("minecraft:raw_iron");
        context.structureSet().overworld().deposit(structureId.toString(), false);

        assertValidationError(context, "must specify at least one nbt template");
        helper.assertFalse(context.assembleData().hasJson(structurePath(structureId)), "Invalid structure omitted");
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
        var processorJson = generated.jsonObject(processorPath(structureId.getNamespace(), "minecraft:raw_iron_block"));
        var structureJson = generated.jsonObject(structurePath(structureId));
        var lang = context.assembleLang();

        helper.assertValueEqual(depositSpecJson.get("scanner_icon_item").getAsString(), "minecraft:raw_iron",
                "default scanner icon");
        helper.assertValueEqual(depositSpecJson.get("map_icon_item").getAsString(), "minecraft:raw_iron_block",
                "default map icon");
        CodecHelper.assertParses(helper.raw(), DepositSpec.CODEC, CodecHelper.registries(helper.raw()),
                depositSpecJson.toString(), "generated custom deposit spec");
        helper.assertValueEqual(depositSpecJson.get("structure").getAsString(), structureId.toString(),
                "deposit spec structure id");
        helper.assertFalse(depositSpecJson.has("dimension"),
                "Default overworld dimension should be omitted");

        helper.assertValueEqual(structureJson.get("type").getAsString(), "create_rns:deposit", "structure type");
        helper.assertValueEqual(structureJson.get("biomes").getAsString(), "#create_rns:has_deposit", "biome tag");
        helper.assertValueEqual(structureJson.get("placement_strategy").getAsString(), "overworld", "placement strategy");
        helper.assertValueEqual(structureJson.get("height").getAsInt(), -8, "default overworld height");
        var structures = structureJson.getAsJsonArray("structures");
        helper.assertValueEqual(structures.size(), 1, "template count");
        assertStructureEntry(helper, structures, 0, DEFAULT_TEMPLATE_SMALL.toString(), 1,
                "create_rns:replace_with_minecraft_raw_iron_block");

        var processors = processorJson.getAsJsonArray("processors");
        helper.assertValueEqual(processors.size(), 1, "processor count");
        var rule = processors.get(0).getAsJsonObject().getAsJsonArray("rules").get(0).getAsJsonObject();
        helper.assertValueEqual(rule.getAsJsonObject("input_predicate").get("block").getAsString(), "minecraft:end_stone",
                "processor input block");
        helper.assertValueEqual(rule.getAsJsonObject("output_state").get("Name").getAsString(), "minecraft:raw_iron_block",
                "processor output block");

        helper.assertTrue(lang.map().isEmpty(), "Expected no lang entries without displayName()");
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
                .mapIcon("#c:gems/diamond")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        var generated = context.assembleData();
        var depositSpecJson = generated.jsonObject(depositSpecPath(structureId));
        var lang = context.assembleLang();

        assertArrayStrings(helper, depositSpecJson.get("scanner_icon_item"), "scanner icon candidates",
                "#c:raw_materials/copper",
                "#c:ores/copper",
                "#c:ingots/copper",
                "#c:nuggets/copper",
                "#c:gems/diamond",
                "#c:dusts/redstone");
        assertArrayStrings(helper, depositSpecJson.get("map_icon_item"), "map icon candidates",
                "minecraft:compass",
                "#c:gems/diamond");
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
                .mapIcon("#c:ingots/gold")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 7)
                .nbt(DEFAULT_TEMPLATE_LARGE.toString(), 3);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        var generated = context.assembleData();
        var depositSpecJson = generated.jsonObject(depositSpecPath(structureId));
        var structureJson = generated.jsonObject(structurePath(structureId));
        var structures = structureJson.getAsJsonArray("structures");

        assertArrayStrings(helper, depositSpecJson.get("map_icon_item"), "explicit map icons",
                "minecraft:gold_ingot",
                "#c:ingots/gold");
        helper.assertValueEqual(structures.size(), 2, "template count");
        assertStructureEntry(helper, structures, 0, DEFAULT_TEMPLATE_SMALL.toString(), 7,
                "create_rns:replace_with_minecraft_gold_block");
        assertStructureEntry(helper, structures, 1, DEFAULT_TEMPLATE_LARGE.toString(), 3,
                "create_rns:replace_with_minecraft_gold_block");
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
                .biome("minecraft:plains")
                .biome("#minecraft:is_badlands")
                .scannerIcon("minecraft:raw_gold")
                .mapIcon("minecraft:clock")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        var generated = context.assembleData();
        var depositSpecJson = generated.jsonObject(depositSpecPath(structureId));
        var processorJson = generated.jsonObject(processorPath(structureId.getNamespace(), "minecraft:gold_block"));
        var structureJson = generated.jsonObject(structurePath(structureId));
        var lang = context.assembleLang();

        helper.assertTrue(depositSpecJson.get("map_icon_item").isJsonPrimitive(),
                "A single explicit map icon should stay a JSON string");
        helper.assertValueEqual(depositSpecJson.get("map_icon_item").getAsString(), "minecraft:clock",
                "single explicit map icon");
        helper.assertValueEqual(structureJson.get("height").getAsInt(), 23, "last explicit height wins");
        helper.assertValueEqual(structureJson.get("biomes").getAsString(), "#minecraft:is_badlands",
                "last explicit biome selector wins");

        var rule = processorJson.getAsJsonArray("processors").get(0).getAsJsonObject()
                .getAsJsonArray("rules").get(0).getAsJsonObject();
        helper.assertValueEqual(rule.getAsJsonObject("output_state").get("Name").getAsString(), "minecraft:gold_block",
                "last explicit deposit block wins");
        assertLangValue(helper, lang, "create_rns", "en_us",
                "create_rns.structure.deposit_test_last_wins", "Final Name");
    }

    @RNSKubeJSBuilderTest
    private static void customBiomeSurvivesDimensionSelection(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_custom_biome");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:gold_block")
                .biome("soul_sand_valley")
                .scannerIcon("minecraft:raw_gold")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().nether().deposit(structureId.toString(), false);

        var structureJson = context.assembleData().jsonObject(structurePath(structureId));
        helper.assertValueEqual(structureJson.get("biomes").getAsString(), "minecraft:soul_sand_valley",
                "normalized custom biome");
        helper.assertValueEqual(structureJson.get("placement_strategy").getAsString(), "nether",
                "selected dimension placement strategy");
    }

    @RNSKubeJSBuilderTest
    private static void biomeRejectsBlankAndMalformedIds(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.depositStructures().create(CreateRNS.asResource("test_invalid_biome").toString());

        assertFatalBuilderError(helper, builder, "cannot be blank", () -> builder.biome(""));
        assertFatalBuilderError(helper, builder, "Invalid biome or biome tag id",
                () -> builder.biome("#not a tag"));
        assertFatalBuilderError(helper, builder, "Unknown vanilla biome",
                () -> builder.biome("minecraft:ld_growth_birch_forest"));
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
        assertPresetStructure(helper, generated, commonId, "minecraft:iron_block",
                "#create_rns:has_deposit", "overworld", -8,
                new PresetTemplate(DEFAULT_TEMPLATE_MEDIUM, 70),
                new PresetTemplate(DEFAULT_TEMPLATE_LARGE, 30));
        assertPresetStructure(helper, generated, uncommonId, "minecraft:copper_block",
                "#create_rns:has_deposit", "overworld", -10,
                new PresetTemplate(DEFAULT_TEMPLATE_SMALL, 30),
                new PresetTemplate(DEFAULT_TEMPLATE_MEDIUM, 60),
                new PresetTemplate(DEFAULT_TEMPLATE_LARGE, 10));
        assertPresetStructure(helper, generated, rareId, "minecraft:diamond_block",
                "#create_rns:has_deposit", "overworld", -12,
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
        assertPresetStructure(helper, generated, commonId, "minecraft:gold_block",
                "#create_rns:has_deposit_nether", "nether", -4,
                new PresetTemplate(DEFAULT_TEMPLATE_MEDIUM, 70),
                new PresetTemplate(DEFAULT_TEMPLATE_LARGE, 30));
        assertPresetStructure(helper, generated, uncommonId, "minecraft:ancient_debris",
                "#create_rns:has_deposit_nether", "nether", -4,
                new PresetTemplate(DEFAULT_TEMPLATE_SMALL, 30),
                new PresetTemplate(DEFAULT_TEMPLATE_MEDIUM, 60),
                new PresetTemplate(DEFAULT_TEMPLATE_LARGE, 10));
        assertPresetStructure(helper, generated, rareId, "minecraft:quartz_block",
                "#create_rns:has_deposit_nether", "nether", -4,
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
                "create_rns:replace_with_minecraft_emerald_block");
        assertStructureEntry(helper, structures, 1, DEFAULT_TEMPLATE_LARGE.toString(), 30,
                "create_rns:replace_with_minecraft_emerald_block");
        assertStructureEntry(helper, structures, 2, DEFAULT_TEMPLATE_SMALL.toString(), 5,
                "create_rns:replace_with_minecraft_emerald_block");
    }

    @RNSKubeJSBuilderTest
    private static void assembleLangUsesTheSameValidationAsAssembleData(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_lang_validation");

        context.depositStructures().create(structureId.toString())
                .scannerIcon("minecraft:raw_iron")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(structureId.toString(), false);

        helper.assertTrue(context.assembleLang().map().isEmpty(), "Invalid structure emits no language entries");
        helper.assertTrue(context.processor().getProcessedEvents().errors().stream()
                        .anyMatch(d -> d.message().contains("must specify a deposit block")),
                "Language-first validation retains the diagnostic for server data loading");
        helper.assertFalse(context.depositStructures().created().getFirst().isValid(),
                "Language generation must validate structures before emitting entries");
    }

    @RNSKubeJSBuilderTest
    private static void languageBeforeDataPreservesSelectionAndDimension(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var selectedId = CreateRNS.asResource("test_lang_first_nether");
        var unselectedId = CreateRNS.asResource("test_unselected_lang");
        context.depositStructures().create(selectedId.toString())
                .block("minecraft:quartz_block").scannerIcon("minecraft:quartz")
                .preset("overworld_common").displayName("Selected Quartz");
        context.depositStructures().create(unselectedId.toString())
                .block("minecraft:coal_block").scannerIcon("minecraft:coal")
                .preset("overworld_common").displayName("Unselected Coal");
        context.depositStructures().tweak("create_rns:deposit_iron").displayName("Tweaked Iron");
        context.structureSet().nether().deposit(selectedId.toString(), false);

        var langBefore = context.assembleLang();
        helper.assertValueEqual(langBefore.map().size(), 2, "Only the selected custom structure and built-in tweak have names");
        assertLangValue(helper, langBefore, "create_rns", "en_us",
                DepositResources.structureLangKey(selectedId), "Selected Quartz");
        assertLangValue(helper, langBefore, "create_rns", "en_us",
                DepositResources.structureLangKey(CreateRNS.asResource("deposit_iron")), "Tweaked Iron");

        var data = context.assembleData();
        var spec = data.jsonObject(depositSpecPath(selectedId));
        helper.assertValueEqual(spec.get("dimension").getAsString(), "minecraft:the_nether", "Selected dimension");
        helper.assertTrue(spec.get("scannable").getAsBoolean(), "Scan-only selection stays scannable");
        helper.assertFalse(data.hasJson(structurePath(unselectedId)), "Unselected custom structure emits no data");
        helper.assertValueEqual(data.jsonObject(structureSetPath("nether_deposits"))
                .getAsJsonArray("structures").size(), 0, "Scan-only selection does not enable worldgen");
        helper.assertValueEqual(context.assembleLang().map(), langBefore.map(), "Language output is independent of generation order");
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
                "create_rns:replace_with_minecraft_ancient_debris");
        assertStructureEntry(helper, structures, 1, DEFAULT_TEMPLATE_MEDIUM.toString(), 60,
                "create_rns:replace_with_minecraft_ancient_debris");
        assertStructureEntry(helper, structures, 2, DEFAULT_TEMPLATE_LARGE.toString(), 10,
                "create_rns:replace_with_minecraft_ancient_debris");

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
                "create_rns:replace_with_minecraft_emerald_block");
        assertStructureEntry(helper, structures, 1, DEFAULT_TEMPLATE_LARGE.toString(), 30,
                "create_rns:replace_with_minecraft_emerald_block");

        helper.assertValueEqual(structureSetEntries.size(), 1, "structure-set entry count");
        assertStructureSetEntry(helper, structureSetEntries, 0, structureId.toString(), 9);
    }

    @RNSKubeJSBuilderTest
    private static void weightRejectsNonPositiveValues(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.depositStructures().create(CreateRNS.asResource("test_invalid_weight").toString());

        assertFatalBuilderError(helper, builder, "must be positive", () -> builder.weight(0));
        assertFatalBuilderError(helper, builder, "must be positive", () -> builder.weight(-1));
    }

    @RNSKubeJSBuilderTest
    private static void nbtRejectsNonPositiveTemplateWeights(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.depositStructures().create(CreateRNS.asResource("test_invalid_nbt_weight").toString());

        assertFatalBuilderError(helper, builder, "must be positive",
                () -> builder.nbt(DEFAULT_TEMPLATE_SMALL.toString(), 0));
        assertFatalBuilderError(helper, builder, "must be positive",
                () -> builder.nbt(DEFAULT_TEMPLATE_SMALL.toString(), -1));
    }

    @RNSKubeJSBuilderTest
    private static void presetRejectsUnknownIds(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.depositStructures().create(CreateRNS.asResource("test_unknown_preset").toString());

        assertThrows(helper, IllegalArgumentException.class, "Unknown deposit preset",
                () -> builder.preset("moon_legendary"));
    }

    @RNSKubeJSBuilderTest
    private static void customIdsPreservePathsAndShareProcessorsForTheSameBlock(
            RNSKubeJSBuilderTestContext context
    ) {
        var helper = context.helper();
        var firstId = ResourceLocation.parse("mymod:veins/tin");
        var secondId = ResourceLocation.parse("mymod:deposit_other_tin");
        context.depositStructures().create(firstId.toString())
                .block("minecraft:raw_iron_block")
                .displayName("Tin Vein")
                .scannerIcon("minecraft:raw_iron")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.depositStructures().create(secondId.toString())
                .block("minecraft:gold_block")
                .block("minecraft:raw_iron_block")
                .scannerIcon("minecraft:iron_ingot")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(firstId.toString(), false).deposit(secondId.toString(), false);

        var generated = context.assembleData();
        helper.assertValueEqual(generated.jsonObject(ResourceLocation.parse("mymod:create_rns/deposit_spec/veins/tin"))
                .get("structure").getAsString(), firstId.toString(), "Unprefixed structure id is preserved");
        helper.assertValueEqual(generated.jsonObject(ResourceLocation.parse("mymod:create_rns/deposit_spec/other_tin"))
                .get("structure").getAsString(), secondId.toString(), "Only the spec id loses its prefix");
        for (var id : List.of(firstId, secondId)) {
            assertStructureEntry(helper, generated.jsonObject(structurePath(id)).getAsJsonArray("structures"),
                    0, DEFAULT_TEMPLATE_SMALL.toString(), 1, "mymod:replace_with_minecraft_raw_iron_block");
        }
        helper.assertValueEqual(generated.ids().stream()
                .filter(id -> id.getNamespace().equals("mymod")
                        && id.getPath().startsWith("worldgen/processor_list/"))
                .count(), 1L, "Both structures share one processor using the final block id");
        var processor = generated.jsonObject(processorPath("mymod", "minecraft:raw_iron_block"));
        helper.assertValueEqual(processor.getAsJsonArray("processors").get(0).getAsJsonObject()
                .getAsJsonArray("rules").get(0).getAsJsonObject().getAsJsonObject("output_state")
                .get("Name").getAsString(), "minecraft:raw_iron_block", "Shared processor output block");
        assertLangValue(helper, context.assembleLang(), "mymod", "en_us", "mymod.structure.veins.tin", "Tin Vein");
    }

    private static void assertPresetStructure(
            RNSKubeJSBuilderTestHelper helper, RNSKubeJSBuilderTestData generated, ResourceLocation structureId,
            String blockId, String biomeTag, String placementStrategy, int height, PresetTemplate... templates
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
                    structureId.getNamespace() + ":replace_with_" + blockId.replace(':', '_'));
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

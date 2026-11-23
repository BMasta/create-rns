package com.bmaster.createrns.compat.kubejs.deposit.enable;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.kubejs.*;
import com.bmaster.createrns.compat.kubejs.util.StartupErrorTestHelper;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import com.bmaster.createrns.util.CodecHelper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import static com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestAssertions.*;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class EnableDepositsKubeBuilderGameTest {
    private static final ResourceLocation DEFAULT_TEMPLATE_SMALL =
            ResourceLocation.fromNamespaceAndPath("create_rns", "ore_deposit_small");

    @GameTest(template = "empty16x16")
    public void structureSetBuilderScenarios(GameTestHelper helper) {
        RNSKubeJSBuilderTestRunner.runAll(helper, EnableDepositsKubeBuilderGameTest.class);
    }

    @RNSKubeJSBuilderTest
    private static void builtInSelectionsUseDefaultPlacementAndBuiltInWeights(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var overworld = firstBuiltInStructure(helper, Level.OVERWORLD);
        var nether = firstBuiltInStructure(helper, Level.NETHER);

        context.structureSet().overworld().deposit(overworld.id().toString());
        context.structureSet().nether().deposit(nether.id().toString());

        var generated = context.assembleData();
        var overworldSetJson = generated.jsonObject(structureSetPath("deposits"));
        var netherSetJson = generated.jsonObject(structureSetPath("nether_deposits"));
        var overworldEntries = overworldSetJson.getAsJsonArray("structures");
        var netherEntries = netherSetJson.getAsJsonArray("structures");

        assertPlacement(helper, overworldSetJson, 4, 24, 591646342);
        assertPlacement(helper, netherSetJson, 2, 8, 781087034);
        helper.assertValueEqual(overworldEntries.size(), 1, "overworld built-in entry count");
        helper.assertValueEqual(netherEntries.size(), 1, "nether built-in entry count");
        assertStructureSetEntry(helper, overworldEntries, 0, overworld.id().toString(), overworld.weight());
        assertStructureSetEntry(helper, netherEntries, 0, nether.id().toString(), nether.weight());
    }

    @RNSKubeJSBuilderTest
    private static void builtInSelectionUsesTweakedWeightUnlessDepositCallOverridesIt(
            RNSKubeJSBuilderTestContext context
    ) {
        var helper = context.helper();
        var builtIns = enabledBuiltInStructures(Level.OVERWORLD);
        helper.assertTrue(builtIns.size() >= 2, "Expected at least two overworld built-in deposit structures");
        var tweakedDefault = builtIns.get(0);
        var explicitlyOverridden = builtIns.get(1);

        context.depositStructures().tweak(tweakedDefault.id().toString()).weight(9);
        context.depositStructures().tweak(explicitlyOverridden.id().toString()).weight(11);
        context.structureSet().overworld()
                .deposit(tweakedDefault.id().toString())
                .deposit(explicitlyOverridden.id().toString(), 17);

        var structures = context.assembleData().jsonObject(structureSetPath("deposits"))
                .getAsJsonArray("structures");

        helper.assertValueEqual(structures.size(), 2, "selected tweaked built-in count");
        assertStructureSetEntry(helper, structures, 0, tweakedDefault.id().toString(), 9);
        assertStructureSetEntry(helper, structures, 1, explicitlyOverridden.id().toString(), 17);
    }

    @RNSKubeJSBuilderTest
    private static void depositOverloadsApplyBuilderAndOverrideWeights(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var firstId = CreateRNS.asResource("test_deposit_default_weight");
        var secondId = CreateRNS.asResource("test_deposit_override_weight");

        var overworld = context.structureSet().overworld();
        context.depositStructures().create(firstId.toString())
                .block("minecraft:iron_block")
                .scannerIcon("minecraft:raw_iron")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1)
                .weight(9);
        context.depositStructures().create(secondId.toString())
                .block("minecraft:gold_block")
                .scannerIcon("minecraft:raw_gold")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1)
                .weight(5);
        overworld.deposit(firstId.toString());
        overworld.deposit(secondId.toString(), 14);

        var generated = context.assembleData();
        var structures = generated.jsonObject(structureSetPath("deposits")).getAsJsonArray("structures");

        helper.assertValueEqual(structures.size(), 2, "selected worldgen structure count");
        assertStructureSetEntry(helper, structures, 0, firstId.toString(), 9);
        assertStructureSetEntry(helper, structures, 1, secondId.toString(), 14);
    }

    @RNSKubeJSBuilderTest
    private static void nonWorldgenSelectionsRemainScannableButSkipStructureSetOutput(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var firstId = CreateRNS.asResource("test_non_worldgen_default_overload");
        var secondId = CreateRNS.asResource("test_non_worldgen_direct_overload");

        context.depositStructures().create(firstId.toString())
                .block("minecraft:lapis_block")
                .scannerIcon("minecraft:lapis_lazuli")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.depositStructures().create(secondId.toString())
                .block("minecraft:redstone_block")
                .scannerIcon("minecraft:redstone")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);

        var overworld = context.structureSet().overworld();
        overworld.deposit(firstId.toString(), false);
        overworld.deposit(secondId.toString(), Integer.valueOf(23), false);

        var generated = context.assembleData();
        var structureSetJson = generated.jsonObject(structureSetPath("deposits"));
        var structures = structureSetJson.getAsJsonArray("structures");
        var depositsTagJson = generated.jsonObject(CreateRNS.asResource("tags/worldgen/structure/deposits"));

        assertPlacement(helper, structureSetJson, 4, 24, 591646342);
        helper.assertValueEqual(structures.size(), 0, "worldgen structure count");
        assertArrayStrings(helper, depositsTagJson.get("values"), "selected structure ids",
                expectedTagForOverworldSelection(firstId, secondId));
        helper.assertTrue(generated.hasJson(depositSpecPath(firstId)), "Expected first custom deposit spec JSON");
        helper.assertTrue(generated.hasJson(depositSpecPath(secondId)), "Expected second custom deposit spec JSON");
        helper.assertTrue(generated.hasJson(structurePath(firstId)), "Expected first custom structure JSON");
        helper.assertTrue(generated.hasJson(structurePath(secondId)), "Expected second custom structure JSON");
    }

    @RNSKubeJSBuilderTest
    private static void builtInSelectionsRewriteGlobalSpecVisibilityAndSortSelectedIds(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var overworldBuiltIns = enabledBuiltInStructures(Level.OVERWORLD).stream()
                .sorted(java.util.Comparator.comparing(structure -> structure.id().toString()))
                .toList();
        var netherBuiltIn = firstBuiltInStructure(helper, Level.NETHER);
        helper.assertTrue(overworldBuiltIns.size() >= 3, "Expected at least three overworld built-in structures");

        var selectedFirst = overworldBuiltIns.getFirst();
        var omittedOverworld = overworldBuiltIns.get(1);
        var selectedLast = overworldBuiltIns.getLast();

        context.structureSet().overworld()
                .deposit(selectedLast.id().toString(), false)
                .deposit(selectedFirst.id().toString(), false);

        var generated = context.assembleData();
        var overworldSetJson = generated.jsonObject(structureSetPath("deposits"));
        var depositsTagJson = generated.jsonObject(CreateRNS.asResource("tags/worldgen/structure/deposits"));

        assertPlacement(helper, overworldSetJson, 4, 24, 591646342);
        helper.assertValueEqual(overworldSetJson.getAsJsonArray("structures").size(), 0,
                "non-worldgen built-in selections should not emit structure-set entries");
        helper.assertFalse(generated.hasJson(structureSetPath("nether_deposits")),
                "Did not expect a nether structure-set file without configuring nether()");
        assertArrayStrings(helper, depositsTagJson.get("values"), "selected structure ids",
                expectedTagForOverworldSelection(selectedFirst.id(), selectedLast.id()));

        assertBuiltInScannableState(helper, generated, selectedFirst.id(), true);
        assertBuiltInScannableState(helper, generated, selectedLast.id(), true);
        assertBuiltInScannableState(helper, generated, omittedOverworld.id(), false);
        helper.assertFalse(generated.hasJson(depositSpecPath(netherBuiltIn.id())),
                "Did not expect a nether deposit-spec override without configuring nether()");
    }

    @RNSKubeJSBuilderTest
    private static void emptyDimensionBuilderEmitsEmptyOverrideOnlyForThatDimension(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();

        context.structureSet().overworld();

        var generated = context.assembleData();
        var overworldSetJson = generated.jsonObject(structureSetPath("deposits"));

        assertPlacement(helper, overworldSetJson, 4, 24, 591646342);
        helper.assertValueEqual(overworldSetJson.getAsJsonArray("structures").size(), 0,
                "empty overworld builder should emit an empty structure-set file");
        helper.assertFalse(generated.hasJson(structureSetPath("nether_deposits")),
                "Did not expect a nether structure-set file without configuring nether()");
        assertBuiltInScannableState(helper, generated, firstBuiltInStructure(helper, Level.OVERWORLD).id(), false);
        helper.assertFalse(generated.hasJson(depositSpecPath(firstBuiltInStructure(helper, Level.NETHER).id())),
                "An unconfigured dimension must retain its default deposit specs");
        var builtin = DynamicDatapackContent.RESOURCES;
        var selectedBlocks = context.processor().getProcessedEvents().selectedDepositBlocks();
        var expectedNetherBlocks = builtin.getScannableStructures(Level.NETHER).stream()
                .map(structure -> builtin.getProcessor(structure.processorId()).targetBlock())
                .collect(java.util.stream.Collectors.toSet());
        helper.assertValueEqual(selectedBlocks.get(Level.NETHER), expectedNetherBlocks,
                "Unconfigured Nether uses default scannability, including installed compat");
        var tag = generated.jsonObject(CreateRNS.asResource("tags/worldgen/structure/deposits"));
        helper.assertTrue(tag.get("replace").getAsBoolean(), "Explicit empty selection replaces the structure tag");
        assertArrayStrings(helper, tag.get("values"), "Unconfigured dimensions keep all built-in tag members",
                expectedTagForOverworldSelection());
    }

    private static String[] expectedTagForOverworldSelection(ResourceLocation... selectedOverworld) {
        var ids = new TreeSet<String>();
        for (var id : selectedOverworld) ids.add(id.toString());
        for (var structure : DynamicDatapackContent.RESOURCES.structures) {
            if (!structure.dimension().equals(Level.OVERWORLD)) ids.add(structure.id().toString());
        }
        return ids.toArray(String[]::new);
    }

    @RNSKubeJSBuilderTest
    private static void sharedBlockSelectionTracksItsDimension(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        context.structureSet().overworld().deposit("create_rns:deposit_gold", false);
        context.structureSet().nether();

        var selection = context.processor().getProcessedEvents().selectedDepositBlocks();
        var gold = CreateRNS.asResource("gold_deposit_block");
        var depleted = CreateRNS.asResource("depleted_deposit_block");
        helper.assertTrue(selection.get(Level.OVERWORLD).contains(gold), "Scan-only gold is enabled in the Overworld");
        helper.assertTrue(selection.get(Level.NETHER).isEmpty(), "An empty Nether selection excludes shared gold");
        helper.assertTrue(selection.get(Level.OVERWORLD).contains(depleted), "Depleted block stays enabled");
    }

    @RNSKubeJSBuilderTest
    private static void worldgenSelectionRequiresResolvableWeight(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_missing_structure_weight");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:coal_block")
                .scannerIcon("minecraft:coal")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        context.structureSet().overworld().deposit(structureId.toString());

        assertValidationError(context, "does not have an assigned weight");
        var generated = context.assembleData();
        helper.assertTrue(generated.jsonObject(depositSpecPath(structureId)).get("scannable").getAsBoolean(),
                "Missing worldgen weight must not disable scanning");
        helper.assertValueEqual(generated.jsonObject(structureSetPath("deposits"))
                .getAsJsonArray("structures").size(), 0, "Only the missing-weight worldgen entry is skipped");
        var repeatedErrors = StartupErrorTestHelper.capture(() -> {
            context.processor().getProcessedEvents();
            context.assembleData();
            context.assembleLang();
        });
        helper.assertTrue(repeatedErrors.isEmpty(), "Non-fatal validation errors must only be reported once");
    }

    @RNSKubeJSBuilderTest
    private static void depositRejectsUnknownStructures(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var overworld = context.structureSet().overworld();

        overworld.deposit("create_rns:missing_structure");
        assertValidationError(context, "Unknown deposit structure");
        helper.assertFalse(overworld.isValid(), "Unknown structure invalidates the selection builder");
    }

    @RNSKubeJSBuilderTest
    private static void builtInSelectionRejectsWrongDimension(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var quartz = CreateRNS.asResource("deposit_nether_quartz");
        var overworld = context.structureSet().overworld().deposit(quartz.toString());
        context.structureSet().nether();

        assertValidationError(context, quartz + " belongs to " + Level.NETHER.location());
        helper.assertFalse(overworld.isValid(), "Wrong dimension invalidates the selection");
        assertBuiltInScannableState(helper, context.assembleData(), quartz, false);
        var selected = context.processor().getProcessedEvents().selectedDepositBlocks();
        var block = CreateRNS.asResource("quartz_deposit_block");
        helper.assertTrue(selected.values().stream().noneMatch(ids -> ids.contains(block)),
                "Rejected quartz selection must not enable its block or mining recipe");
    }

    @RNSKubeJSBuilderTest
    private static void depositRejectsDuplicateSelections(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var overworld = context.structureSet().overworld();
        var builtIn = firstBuiltInStructure(helper, Level.OVERWORLD);

        overworld.deposit(builtIn.id().toString(), false);

        assertFatalBuilderError(helper, overworld, "already selected in structure set",
                () -> overworld.deposit(builtIn.id().toString(), false));
    }

    @RNSKubeJSBuilderTest
    private static void depositIntOverloadRejectsNonPositiveWeights(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builtIn = firstBuiltInStructure(helper, Level.OVERWORLD);
        var overworld = context.structureSet().overworld();

        assertFatalBuilderError(helper, overworld, "must be positive",
                () -> overworld.deposit(builtIn.id().toString(), 0));
        assertFatalBuilderError(helper, overworld, "must be positive",
                () -> overworld.deposit(builtIn.id().toString(), -1));
    }

    @RNSKubeJSBuilderTest
    private static void depositDirectOverloadRejectsNonPositiveWeights(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builtIn = firstBuiltInStructure(helper, Level.OVERWORLD);
        var overworld = context.structureSet().overworld();

        assertFatalBuilderError(helper, overworld, "must be positive",
                () -> overworld.deposit(builtIn.id().toString(), Integer.valueOf(0), true));
        assertFatalBuilderError(helper, overworld, "must be positive",
                () -> overworld.deposit(builtIn.id().toString(), Integer.valueOf(-1), false));
    }

    @RNSKubeJSBuilderTest
    private static void spacingSeparationAndSaltSerializeExplicitValues(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builtIn = firstBuiltInStructure(helper, Level.OVERWORLD);

        context.structureSet().overworld()
                .deposit(builtIn.id().toString())
                .spacing(4096)
                .separation(4095)
                .salt(321);

        var generated = context.assembleData();
        assertPlacement(helper, generated.jsonObject(structureSetPath("deposits")), 4095, 4096, 321);
    }

    @RNSKubeJSBuilderTest
    private static void partialPlacementOverridesAndZeroSaltSerialize(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var overworldBuiltIn = firstBuiltInStructure(helper, Level.OVERWORLD);
        var netherBuiltIn = firstBuiltInStructure(helper, Level.NETHER);

        context.structureSet().overworld()
                .deposit(overworldBuiltIn.id().toString())
                .spacing(40);
        context.structureSet().nether()
                .deposit(netherBuiltIn.id().toString())
                .salt(0);

        var generated = context.assembleData();
        assertPlacement(helper, generated.jsonObject(structureSetPath("deposits")), 4, 40, 591646342);
        assertPlacement(helper, generated.jsonObject(structureSetPath("nether_deposits")), 2, 8, 0);
    }

    @RNSKubeJSBuilderTest
    private static void placementRejectsOutOfRangeValues(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.structureSet().overworld();

        assertFatalBuilderError(helper, builder, "between 1 and 4096", () -> builder.spacing(0));
        assertFatalBuilderError(helper, builder, "between 1 and 4096", () -> builder.spacing(4097));
        assertFatalBuilderError(helper, builder, "between 0 and 4096", () -> builder.separation(-1));
        assertFatalBuilderError(helper, builder, "between 0 and 4096", () -> builder.separation(4097));
        assertFatalBuilderError(helper, builder, "salt must be non-negative", () -> builder.salt(-7));
    }

    @RNSKubeJSBuilderTest
    private static void spacingAndSeparationValidationWorksInEitherCallOrder(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var overworld = context.structureSet().overworld();
        var nether = context.structureSet().nether();

        overworld.spacing(4).separation(4);
        nether.separation(3).spacing(3);
        assertValidationError(context, "greater than separation");
        helper.assertTrue(!overworld.isValid() && !nether.isValid(), "Both final configurations are invalid");
    }

    @RNSKubeJSBuilderTest
    private static void customStructuresCannotBeAssignedToBothDimensions(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_cross_dimension_conflict");

        context.depositStructures().create(structureId.toString())
                .block("minecraft:diamond_block")
                .scannerIcon("minecraft:diamond")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1)
                .weight(5);
        context.structureSet().overworld().deposit(structureId.toString(), false);
        context.structureSet().nether().deposit(structureId.toString(), false);

        assertValidationError(context, "cannot belong to both minecraft:overworld and minecraft:the_nether");
        helper.assertFalse(context.structureSet().hasConfiguredDimensions(), "Conflicting selections are invalidated");
    }

    @RNSKubeJSBuilderTest
    private static void selectionResolvesCompletedDefinitionsAndFinalPlacementValues(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var structureId = CreateRNS.asResource("test_later_definition");
        var overworld = context.structureSet().overworld().deposit(structureId.toString());
        // An intermediate invalid relationship is allowed; only the final values matter.
        overworld.spacing(4).separation(4).spacing(6);
        var structure = context.depositStructures().create(structureId.toString())
                .block("minecraft:coal_block")
                .scannerIcon("minecraft:coal")
                .nbt(DEFAULT_TEMPLATE_SMALL.toString(), 1);
        structure.weight(7);

        helper.assertTrue(context.validationErrors().isEmpty(), "Final configuration is valid");
        var json = context.assembleData().jsonObject(structureSetPath("deposits"));
        assertStructureSetEntry(helper, json.getAsJsonArray("structures"), 0, structureId.toString(), 7);
        helper.assertValueEqual(json.getAsJsonObject("placement").get("spacing").getAsInt(), 6, "Final spacing");
    }

    @RNSKubeJSBuilderTest
    private static void placementValidationIncludesDefaults(RNSKubeJSBuilderTestContext context) {
        var overworld = context.structureSet().overworld().spacing(4);
        var nether = context.structureSet().nether().separation(8);
        assertValidationError(context, "greater than separation");
        context.helper().assertTrue(!overworld.isValid() && !nether.isValid(),
                "Explicit values must be compatible with the omitted setting's default");
    }

    @RNSKubeJSBuilderTest
    private static void customDimensionGeneratesMatchingResources(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var event = context.structureSet();
        var dimension = "mymod:moon";
        var builder = event.dimension(dimension, "nether");
        helper.assertTrue(builder == event.dimension(dimension), "Repeated calls reuse the dimension builder");
        helper.assertTrue(event.nether() == event.dimension("minecraft:the_nether"), "Nether alias");

        var structure = ResourceLocation.parse("mymod:deposit_lunar_iron");
        context.depositStructures().create(structure.toString()).block("minecraft:iron_block")
                .scannerIcon("minecraft:iron_ingot").preset("overworld_common").biome("#mymod:lunar_biomes");
        builder.deposit(structure.toString()).jeiMiningTitle("Lunar Mining");
        var data = context.assembleData();
        var json = data.jsonObject(structurePath(structure));
        helper.assertValueEqual(json.get("placement_strategy").getAsString(), "nether", "Explicit vertical placement");
        helper.assertValueEqual(json.get("biomes").getAsString(), "#mymod:lunar_biomes", "Structure biome selector");
        helper.assertValueEqual(data.jsonObject(depositSpecPath(structure)).get("dimension").getAsString(),
                dimension, "Scanner dimension");
        var set = data.jsonObject(structureSetPath("mymod_moon_deposits"));
        assertStructureSetEntry(helper, set.getAsJsonArray("structures"), 0, structure.toString(), 50);
        assertPlacement(helper, set, 4, 24, dimension.hashCode() & Integer.MAX_VALUE);
        assertLangValue(helper, context.assembleLang(), "create_rns", "en_us",
                "create_rns.jei.title.mining_mymod_moon", "Lunar Mining");
    }

    @RNSKubeJSBuilderTest
    private static void structureBiomesOverrideTheSharedSelector(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var iron = "create_rns:deposit_iron";
        var copper = "create_rns:deposit_copper";
        context.depositStructures().tweak(iron).weight(3);
        context.depositStructures().tweak(copper).biome("minecraft:desert");
        context.structureSet().overworld().deposit(iron).deposit(copper, 5)
                .biome("plains").biome("#minecraft:is_hill").biome("minecraft:plains");

        helper.assertTrue(context.validationErrors().isEmpty(), "Shared and structure biomes may be combined");
        var data = context.assembleData();
        var tag = data.jsonObject(CreateRNS.asResource("tags/worldgen/biome/has_deposit"));
        helper.assertTrue(tag.get("replace").getAsBoolean(), "Override the built-in biome list");
        var values = tag.getAsJsonArray("values");
        helper.assertValueEqual(values.size(), 2, "Ignore duplicate biome IDs");
        helper.assertValueEqual(values.get(0).getAsString(), "minecraft:plains", "Direct biome");
        helper.assertValueEqual(values.get(1).getAsString(), "#minecraft:is_hill", "Nested tag");
        var entries = data.jsonObject(structureSetPath("deposits")).getAsJsonArray("structures");
        helper.assertValueEqual(entries.size(), 2, "Both structures remain eligible for worldgen");
        assertStructureSetEntry(helper, entries, 0, iron, 3);
        assertStructureSetEntry(helper, entries, 1, copper, 5);
        helper.assertValueEqual(data.jsonObject(structurePath(ResourceLocation.parse(iron))).get("biomes").getAsString(),
                "#create_rns:has_deposit", "Iron inherits the shared biome tag");
        helper.assertValueEqual(data.jsonObject(structurePath(ResourceLocation.parse(copper))).get("biomes").getAsString(),
                "minecraft:desert", "Copper uses only its explicit biome");
        assertBuiltInScannableState(helper, data, ResourceLocation.parse(copper), true);
    }

    @RNSKubeJSBuilderTest
    private static void endUsesBundledTagAndCustomDimensionUsesGeneratedTag(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var end = context.structureSet().end();
        helper.assertTrue(end == context.structureSet().dimension("minecraft:the_end"), "End alias");
        var moon = context.structureSet().dimension("mymod:moon", "default").biome("mymod:lunar_plains");
        for (var name : List.of("end", "moon")) {
            var id = "mymod:" + name;
            context.depositStructures().create(id).block("minecraft:iron_block").scannerIcon("iron_ingot")
                    .preset("end_common");
            (name.equals("end") ? end : moon).deposit(id);
        }
        var data = context.assembleData();
        var endStructure = data.jsonObject(structurePath(ResourceLocation.parse("mymod:end")));
        helper.assertValueEqual(endStructure.get("placement_strategy").getAsString(), "default", "Placement in the End");
        helper.assertValueEqual(endStructure.get("biomes").getAsString(), "#create_rns:has_deposit_end", "End tag");
        assertPlacement(helper, data.jsonObject(structureSetPath("end_deposits")), 4, 24, 196423253);
        helper.assertFalse(data.hasJson(CreateRNS.asResource("tags/worldgen/biome/has_deposit_end")),
                "Omitting biome() uses the bundled End tag");
        var moonStructure = data.jsonObject(structurePath(ResourceLocation.parse("mymod:moon")));
        helper.assertValueEqual(moonStructure.get("placement_strategy").getAsString(), "default", "Explicit default placement");
        helper.assertValueEqual(moonStructure.get("biomes").getAsString(), "#create_rns:has_deposit_mymod_moon",
                "Custom dimension uses the generated tag");
        helper.assertTrue(data.hasJson(CreateRNS.asResource("tags/worldgen/biome/has_deposit_mymod_moon")),
                "Custom dimension tag is generated");
    }

    @RNSKubeJSBuilderTest
    private static void sharedBiomeTagRejectsSelfReference(RNSKubeJSBuilderTestContext context) {
        var builder = context.structureSet().end();
        assertFatalBuilderError(context.helper(), builder, "cannot include itself",
                () -> builder.biome("#create_rns:has_deposit_end"));
    }

    @RNSKubeJSBuilderTest
    private static void missingBiomeSkipsOnlyTheAffectedStructure(RNSKubeJSBuilderTestContext context) {
        var builder = context.structureSet().dimension("mymod:moon");
        for (var name : List.of("missing", "explicit")) {
            var structure = context.depositStructures().create("mymod:" + name).block("minecraft:iron_block")
                    .scannerIcon("minecraft:iron_ingot").preset("overworld_common");
            if (name.equals("explicit")) structure.biome("mymod:lunar_plains");
            builder.deposit("mymod:" + name, false);
        }

        assertValidationError(context, "mymod:missing must specify a biome for dimension mymod:moon");
        var data = context.assembleData();
        context.helper().assertFalse(data.hasJson(structurePath(ResourceLocation.parse("mymod:missing"))),
                "Missing selector excludes even a scan-only structure");
        var json = data.jsonObject(structurePath(ResourceLocation.parse("mymod:explicit")));
        context.helper().assertValueEqual(json.get("placement_strategy").getAsString(), "default",
                "Other structures still generate with the default strategy");
    }

    @RNSKubeJSBuilderTest
    private static void conflictingExplicitStrategiesInvalidateTheDimension(RNSKubeJSBuilderTestContext context) {
        var event = context.structureSet();
        var builder = event.dimension("mymod:moon", "nether");
        assertFatalBuilderError(context.helper(), builder, "Conflicting placement strategies",
                () -> event.dimension("mymod:moon", "default"));
        assertThrows(context.helper(), IllegalArgumentException.class, "Unknown placement strategy",
                () -> event.dimension("mymod:moon", "unknown"));
    }

    private static BuiltInStructure firstBuiltInStructure(
            RNSKubeJSBuilderTestHelper helper, ResourceKey<Level> dimension
    ) {
        var structures = enabledBuiltInStructures(dimension);
        helper.assertTrue(!structures.isEmpty(), "Expected built-in " + dimension.location() + " deposit structures");
        return structures.getFirst();
    }

    private static List<BuiltInStructure> enabledBuiltInStructures(ResourceKey<Level> dimension) {
        return DynamicDatapackContent.RESOURCES.getScannableStructures(dimension).stream()
                .map(entry -> new BuiltInStructure(entry.id(), Objects.requireNonNull(entry.weight())))
                .toList();
    }

    private static void assertBuiltInScannableState(
            RNSKubeJSBuilderTestHelper helper, RNSKubeJSBuilderTestData generated, ResourceLocation structureId,
            boolean expectedScannable
    ) {
        var depositSpecJson = generated.jsonObject(depositSpecPath(structureId));
        helper.assertValueEqual(!depositSpecJson.has("scannable") || depositSpecJson.get("scannable").getAsBoolean(),
                expectedScannable, "built-in scannability");
    }

    private static void assertPlacement(
            RNSKubeJSBuilderTestHelper helper, com.google.gson.JsonObject structureSetJson,
            int separation, int spacing, int salt
    ) {
        var placement = structureSetJson.getAsJsonObject("placement");
        CodecHelper.assertParses(helper.raw(), StructurePlacement.CODEC, CodecHelper.registries(helper.raw()),
                placement.toString(), "generated deposit placement");
        helper.assertValueEqual(placement.get("type").getAsString(), "minecraft:random_spread", "placement type");
        helper.assertValueEqual(placement.get("separation").getAsInt(), separation, "placement separation");
        helper.assertValueEqual(placement.get("spacing").getAsInt(), spacing, "placement spacing");
        helper.assertValueEqual(placement.get("salt").getAsInt(), salt, "placement salt");
    }

    private record BuiltInStructure(ResourceLocation id, int weight) {
    }
}

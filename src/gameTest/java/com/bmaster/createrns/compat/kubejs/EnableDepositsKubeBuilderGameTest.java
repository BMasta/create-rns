package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.pack.DepositDimension;
import com.bmaster.createrns.data.pack.DepositSpecBuilder;
import com.bmaster.createrns.data.pack.DepositStructureBuilder;
import com.bmaster.createrns.data.pack.DynamicDatapackContent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestAssertions.assertArrayStrings;
import static com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestAssertions.assertStructureSetEntry;
import static com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestAssertions.assertThrows;
import static com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestAssertions.depositSpecPath;
import static com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestAssertions.structurePath;
import static com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestAssertions.structureSetPath;

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
        var overworld = firstBuiltInStructure(helper, DepositDimension.OVERWORLD);
        var nether = firstBuiltInStructure(helper, DepositDimension.NETHER);

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
        var builtIns = enabledBuiltInStructures(DepositDimension.OVERWORLD);
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
                firstId.toString(),
                secondId.toString());
        helper.assertTrue(generated.hasJson(depositSpecPath(firstId)), "Expected first custom deposit spec JSON");
        helper.assertTrue(generated.hasJson(depositSpecPath(secondId)), "Expected second custom deposit spec JSON");
        helper.assertTrue(generated.hasJson(structurePath(firstId)), "Expected first custom structure JSON");
        helper.assertTrue(generated.hasJson(structurePath(secondId)), "Expected second custom structure JSON");
    }

    @RNSKubeJSBuilderTest
    private static void builtInSelectionsRewriteGlobalSpecVisibilityAndSortSelectedIds(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var overworldBuiltIns = enabledBuiltInStructures(DepositDimension.OVERWORLD).stream()
                .sorted(java.util.Comparator.comparing(structure -> structure.id().toString()))
                .toList();
        var netherBuiltIn = firstBuiltInStructure(helper, DepositDimension.NETHER);
        helper.assertTrue(overworldBuiltIns.size() >= 3, "Expected at least three overworld built-in structures");

        var selectedFirst = overworldBuiltIns.get(0);
        var omittedOverworld = overworldBuiltIns.get(1);
        var selectedLast = overworldBuiltIns.get(overworldBuiltIns.size() - 1);

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
                selectedFirst.id().toString(),
                selectedLast.id().toString());

        assertBuiltInScannableState(helper, generated, selectedFirst.id(), true);
        assertBuiltInScannableState(helper, generated, selectedLast.id(), true);
        assertBuiltInScannableState(helper, generated, omittedOverworld.id(), false);
        helper.assertFalse(generated.hasJson(DynamicDatapackContent.depositSpecPath(findSpecByStructure(netherBuiltIn.id()))),
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

        assertThrows(helper, IllegalStateException.class, "does not have an assigned weight", context::assembleData);
    }

    @RNSKubeJSBuilderTest
    private static void depositRejectsUnknownStructures(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var overworld = context.structureSet().overworld();

        assertThrows(helper, IllegalArgumentException.class, "Unknown deposit structure",
                () -> overworld.deposit("create_rns:missing_structure"));
    }

    @RNSKubeJSBuilderTest
    private static void depositRejectsDuplicateSelections(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var overworld = context.structureSet().overworld();
        var builtIn = firstBuiltInStructure(helper, DepositDimension.OVERWORLD);

        overworld.deposit(builtIn.id().toString(), false);

        assertThrows(helper, IllegalStateException.class, "already selected in structure set",
                () -> overworld.deposit(builtIn.id().toString(), false));
    }

    @RNSKubeJSBuilderTest
    private static void depositIntOverloadRejectsNonPositiveWeights(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builtIn = firstBuiltInStructure(helper, DepositDimension.OVERWORLD);
        var overworld = context.structureSet().overworld();

        assertThrows(helper, IllegalArgumentException.class, "must be positive",
                () -> overworld.deposit(builtIn.id().toString(), 0));
        assertThrows(helper, IllegalArgumentException.class, "must be positive",
                () -> overworld.deposit(builtIn.id().toString(), -1));
    }

    @RNSKubeJSBuilderTest
    private static void depositDirectOverloadRejectsNonPositiveWeights(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builtIn = firstBuiltInStructure(helper, DepositDimension.OVERWORLD);
        var overworld = context.structureSet().overworld();

        assertThrows(helper, IllegalArgumentException.class, "must be positive",
                () -> overworld.deposit(builtIn.id().toString(), Integer.valueOf(0), true));
        assertThrows(helper, IllegalArgumentException.class, "must be positive",
                () -> overworld.deposit(builtIn.id().toString(), Integer.valueOf(-1), false));
    }

    @RNSKubeJSBuilderTest
    private static void spacingSeparationAndSaltSerializeExplicitValues(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builtIn = firstBuiltInStructure(helper, DepositDimension.OVERWORLD);

        context.structureSet().overworld()
                .deposit(builtIn.id().toString())
                .spacing(40)
                .separation(7)
                .salt(321);

        var generated = context.assembleData();
        assertPlacement(helper, generated.jsonObject(structureSetPath("deposits")), 7, 40, 321);
    }

    @RNSKubeJSBuilderTest
    private static void partialPlacementOverridesAndNegativeSaltSerialize(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var overworldBuiltIn = firstBuiltInStructure(helper, DepositDimension.OVERWORLD);
        var netherBuiltIn = firstBuiltInStructure(helper, DepositDimension.NETHER);

        context.structureSet().overworld()
                .deposit(overworldBuiltIn.id().toString())
                .spacing(40);
        context.structureSet().nether()
                .deposit(netherBuiltIn.id().toString())
                .salt(-7);

        var generated = context.assembleData();
        assertPlacement(helper, generated.jsonObject(structureSetPath("deposits")), 4, 40, 591646342);
        assertPlacement(helper, generated.jsonObject(structureSetPath("nether_deposits")), 2, 8, -7);
    }

    @RNSKubeJSBuilderTest
    private static void spacingRejectsNonPositiveValues(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.structureSet().overworld();

        assertThrows(helper, IllegalArgumentException.class, "must be positive", () -> builder.spacing(0));
        assertThrows(helper, IllegalArgumentException.class, "must be positive", () -> builder.spacing(-1));
    }

    @RNSKubeJSBuilderTest
    private static void separationRejectsNegativeValues(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = context.structureSet().overworld();

        assertThrows(helper, IllegalArgumentException.class, "must be non-negative", () -> builder.separation(-1));
    }

    @RNSKubeJSBuilderTest
    private static void spacingAndSeparationValidationWorksInEitherCallOrder(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var overworld = context.structureSet().overworld();
        var nether = context.structureSet().nether();

        assertThrows(helper, IllegalArgumentException.class, "greater than separation", () -> overworld.spacing(4).separation(4));
        assertThrows(helper, IllegalArgumentException.class, "greater than separation", () -> nether.separation(3).spacing(3));
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

        assertThrows(helper, IllegalStateException.class, "cannot belong to both overworld and nether",
                context::assembleData);
    }

    private static BuiltInStructure firstBuiltInStructure(
            RNSKubeJSBuilderTestHelper helper, DepositDimension dimension
    ) {
        var structures = enabledBuiltInStructures(dimension);
        helper.assertTrue(!structures.isEmpty(), "Expected built-in " + dimension.getSerializedName() + " deposit structures");
        return structures.get(0);
    }

    private static List<BuiltInStructure> enabledBuiltInStructures(DepositDimension dimension) {
        return DepositStructureBuilder.getScannableDeposits(dimension).stream()
                .map(entry -> new BuiltInStructure(DepositStructureBuilder.structureId(entry), entry.structure().weight()))
                .toList();
    }

    private static void assertBuiltInScannableState(
            RNSKubeJSBuilderTestHelper helper, RNSKubeJSBuilderTestData generated, ResourceLocation structureId,
            boolean expectedScannable
    ) {
        var depositSpecJson = generated.jsonObject(DynamicDatapackContent.depositSpecPath(findSpecByStructure(structureId)));
        helper.assertValueEqual(!depositSpecJson.has("scannable"), expectedScannable,
                "selected built-in scannable field omission");
        if (!expectedScannable) {
            helper.assertFalse(depositSpecJson.get("scannable").getAsBoolean(),
                    "omitted built-in deposits should be forced non-scannable");
        }
    }

    private static DepositSpecBuilder.ConfiguredEntry findSpecByStructure(ResourceLocation structureId) {
        return DepositSpecBuilder.getSpecs().stream()
                .filter(entry -> entry.spec().structureId().equals(structureId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing built-in deposit spec for structure " + structureId));
    }

    private static void assertPlacement(
            RNSKubeJSBuilderTestHelper helper, com.google.gson.JsonObject structureSetJson,
            int separation, int spacing, int salt
    ) {
        var placement = structureSetJson.getAsJsonObject("placement");
        helper.assertValueEqual(placement.get("type").getAsString(), "minecraft:random_spread", "placement type");
        helper.assertValueEqual(placement.get("separation").getAsInt(), separation, "placement separation");
        helper.assertValueEqual(placement.get("spacing").getAsInt(), spacing, "placement spacing");
        helper.assertValueEqual(placement.get("salt").getAsInt(), salt, "placement salt");
    }

    private record BuiltInStructure(ResourceLocation id, int weight) {
    }
}

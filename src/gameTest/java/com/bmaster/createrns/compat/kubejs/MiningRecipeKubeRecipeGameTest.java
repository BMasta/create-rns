package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.util.CodecHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.script.SourceLine;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MiningRecipeKubeRecipeGameTest {
    private static final String VALID_DEPOSIT_BLOCK = "create_rns:iron_deposit_block";
    private static final int EXPLICIT_SLOT_COLOR = 0x12345678;
    private static final int STRING_SLOT_COLOR = (int) 0xFF123ABCL;

    @GameTest(template = "empty16x16")
    public void builderEmissionScenarios(GameTestHelper helper) {
        RNSKubeJSBuilderTestRunner.runAll(helper, MiningRecipeKubeRecipeGameTest.class);
    }

    @RNSKubeJSBuilderTest
    private static void invalidDurabilityValuesAreDroppedFromSerializedJson(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("invalid_durability");

        recipe.block(VALID_DEPOSIT_BLOCK)
                .durability(0, 4, 0.25f)
                .yield(yield -> yield.item("diamond"));

        var json = serialize(recipe);
        helper.assertFalse(json.has("durability"), "Invalid durability values should not serialize");

        var parsed = parseRecipe(helper, json, "recipe with dropped invalid durability");
        helper.assertValueEqual(parsed.getDurability().core(), 0L, "default durability core");
        helper.assertValueEqual(parsed.getDurability().edge(), 0L, "default durability edge");
        CodecHelper.assertFloat(helper.raw(), parsed.getDurability().randomSpread(), 0f, "default durability spread");
    }

    @RNSKubeJSBuilderTest
    private static void invalidDurabilityUsesItsCallSiteForKubeJSErrorAttribution(
            RNSKubeJSBuilderTestContext context
    ) {
        var expectedSource = SourceLine.of("server_scripts/main.js", 11);
        var recipe = context.miningRecipe("invalid_durability_source");

        recipe.block(VALID_DEPOSIT_BLOCK)
                .durability(8, 3, -0.1f, expectedSource)
                .yield(yield -> yield.item("diamond"));

        var marker = serialize(recipe).getAsJsonObject(KubeRecipe.CHANGED_MARKER);
        context.helper().assertValueEqual(marker.get("source").getAsString(), expectedSource.source(),
                "invalid durability error source");
        context.helper().assertValueEqual(marker.get("line").getAsInt(), expectedSource.line(),
                "invalid durability error line");
    }

    @RNSKubeJSBuilderTest
    private static void multilineRecipeSourceLinesAreRefinedFromRhinosOuterStatement(
            RNSKubeJSBuilderTestContext context
    ) {
        var script = """
                ServerEvents.recipes(event => {
                  event.recipes.create_rns.mining()
                    .id('my_mod:test')
                    .block('create_rns:iron_deposit_block')
                    .durability(8, 3, -0.1)
                    .yield(y => y
                      .chance(-0.1)
                      .item('minecraft:diamond')
                      .catalyst('create_rns:overclock'))
                })
                """;
        var coarse = SourceLine.of("server_scripts:main.js", 6);

        assertSourceLine(context.helper(), KubeJSSourceLine.expressionStart(coarse, script),
                SourceLine.of(coarse.source(), 2), "recipe expression start");
        assertSourceLine(context.helper(), KubeJSSourceLine.methodCall(coarse, script, "block"),
                SourceLine.of(coarse.source(), 4), "block call");
        assertSourceLine(context.helper(), KubeJSSourceLine.methodCall(coarse, script, "durability"),
                SourceLine.of(coarse.source(), 5), "durability call");
        assertSourceLine(context.helper(), KubeJSSourceLine.methodCall(coarse, script, "chance"),
                SourceLine.of(coarse.source(), 7), "chance call");
        assertSourceLine(context.helper(), KubeJSSourceLine.methodCall(coarse, script, "catalyst"),
                SourceLine.of(coarse.source(), 9), "catalyst call");
    }

    @RNSKubeJSBuilderTest
    private static void multilineStartupSourceLinesAreRefinedWithinTheirBuilderChain(
            RNSKubeJSBuilderTestContext context
    ) {
        var script = """
                StartupEvents.rnsDepositStructures(event => {
                  event.create('my_mod:deposit_test')
                    .block('my_mod:deposit_block')
                    .preset('overworld_common')
                    .scannerIcon('minecraft:iron_ingot')
                    .weight(-1)
                })

                StartupEvents.rnsEnableDeposits(event => {
                  event.overworld()
                    .deposit('my_mod:deposit_test')
                    .spacing(4)
                    .separation(4)
                })

                StartupEvents.rnsCatalysts(event => {
                  event.create('my_mod:test_catalyst')
                    .chanceMultiplier(1.5)
                    .hideIfPresent('create_rns:missing')
                    .attachment('minecraft:stone')
                })
                """;
        var structureCoarse = SourceLine.of("startup_scripts:main.js", 6);
        var selectionCoarse = SourceLine.of("startup_scripts:main.js", 13);
        var catalystCoarse = SourceLine.of("startup_scripts:main.js", 20);

        assertSourceLine(context.helper(), KubeJSSourceLine.startupBuilderStart(
                        structureCoarse, script, "rnsDepositStructures", "create|tweak"),
                SourceLine.of(structureCoarse.source(), 2), "deposit structure builder start");
        assertSourceLine(context.helper(), KubeJSSourceLine.startupMethodCall(
                        structureCoarse, script, "rnsDepositStructures", "create|tweak", "weight"),
                SourceLine.of(structureCoarse.source(), 6), "deposit structure method call");
        assertSourceLine(context.helper(), KubeJSSourceLine.startupBuilderStart(
                        selectionCoarse, script, "rnsEnableDeposits", "overworld|nether"),
                SourceLine.of(selectionCoarse.source(), 10), "deposit selection builder start");
        assertSourceLine(context.helper(), KubeJSSourceLine.startupMethodCall(
                        selectionCoarse, script, "rnsEnableDeposits", "overworld|nether", "separation"),
                SourceLine.of(selectionCoarse.source(), 13), "deposit selection method call");
        assertSourceLine(context.helper(), KubeJSSourceLine.startupBuilderStart(
                        catalystCoarse, script, "rnsCatalysts", "create"),
                SourceLine.of(catalystCoarse.source(), 17), "catalyst builder start");
        assertSourceLine(context.helper(), KubeJSSourceLine.startupMethodCall(
                        catalystCoarse, script, "rnsCatalysts", "create", "hideIfPresent"),
                SourceLine.of(catalystCoarse.source(), 19), "hide-if-present method call");
    }

    @RNSKubeJSBuilderTest
    private static void omittedDefaultsProduceMinimalValidRecipeJson(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("minimal_defaults");

        recipe.block(VALID_DEPOSIT_BLOCK)
                .yield(yield -> yield.item("diamond"));

        var json = serialize(recipe);
        helper.assertValueEqual(json.get("type").getAsString(), CreateRNS.asResource("mining").toString(), "recipe type");
        helper.assertValueEqual(json.get("deposit_block").getAsString(), VALID_DEPOSIT_BLOCK, "deposit block");
        helper.assertFalse(json.has("dimension"), "Default dimension should stay omitted");
        helper.assertFalse(json.has("replace_when_depleted"), "Default replacement block should stay omitted");
        helper.assertFalse(json.has("durability"), "Default durability should stay omitted");

        var yields = json.getAsJsonArray("yields");
        helper.assertValueEqual(yields.size(), 1, "yield count");
        var yield = yields.get(0).getAsJsonObject();
        helper.assertFalse(yield.has("chance"), "Default yield chance should stay omitted");
        helper.assertFalse(yield.has("catalysts"), "Empty catalyst list should stay omitted");
        helper.assertFalse(yield.has("jei_slot_color"), "Default JEI slot color should stay omitted");

        var items = yield.getAsJsonArray("items");
        helper.assertValueEqual(items.size(), 1, "yield item count");
        var item = items.get(0).getAsJsonObject();
        helper.assertValueEqual(item.get("item").getAsString(), "minecraft:diamond", "default yield item");
        helper.assertFalse(item.has("compat"), "Strict items should omit compat flag");
        helper.assertFalse(item.has("weight"), "Default item weight should stay omitted");

        var parsed = parseRecipe(helper, json, "minimal mining recipe");
        helper.assertTrue(parsed.getDepositBlock() == RNSDeposits.IRON_DEPOSIT.get(),
                "Expected parsed deposit block to be the iron deposit");
        helper.assertTrue(parsed.getDimension() == Level.OVERWORLD, "Expected default dimension to be overworld");
        helper.assertTrue(parsed.getReplacementBlock() == Blocks.AIR, "Expected default replacement block to be air");
        helper.assertValueEqual(parsed.getDurability().core(), 0L, "default durability core");
        helper.assertValueEqual(parsed.getDurability().edge(), 0L, "default durability edge");
        CodecHelper.assertFloat(helper.raw(), parsed.getDurability().randomSpread(), 0f, "default durability spread");
    }

    @RNSKubeJSBuilderTest
    private static void explicitDefaultReplacementAndYieldBoundaryValuesSerializeCorrectly(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("boundary_values");

        recipe.block(VALID_DEPOSIT_BLOCK)
                .replaceWhenDepleted("air")
                .yield(yield -> yield.chance(0f).item("diamond").jeiSlotColor(0))
                .yield(yield -> yield.chance(1f).item("emerald").jeiSlotColor("#000000"));

        var json = serialize(recipe);
        helper.assertValueEqual(json.get("replace_when_depleted").getAsString(), "minecraft:air",
                "Explicit default replacement block should serialize when set");

        var yields = json.getAsJsonArray("yields");
        var firstYield = yields.get(0).getAsJsonObject();
        var secondYield = yields.get(1).getAsJsonObject();

        helper.assertTrue(firstYield.has("chance"), "A zero chance should serialize explicitly");
        CodecHelper.assertFloat(helper.raw(), firstYield.get("chance").getAsFloat(), 0f, "zero chance");
        helper.assertFalse(firstYield.has("jei_slot_color"), "An explicit zero ARGB slot color should stay omitted");

        helper.assertFalse(secondYield.has("chance"), "A chance of one should stay omitted");
        helper.assertValueEqual(secondYield.get("jei_slot_color").getAsInt(), (int) 0xFF000000L,
                "Opaque black string slot color");
    }

    @RNSKubeJSBuilderTest
    private static void airDepositBlockSerializesAndFailsRecipeDecoding(RNSKubeJSBuilderTestContext context) {
        var json = serialize(context.miningRecipe("air_deposit_block")
                .block("air")
                .yield(yield -> yield.item("diamond")));

        assertRecipeFails(context.helper(), json, "Deposit block cannot be minecraft:air");
    }

    @RNSKubeJSBuilderTest
    private static void missingDepositBlockIdIsPreservedForOneAttributedRecipeError(
            RNSKubeJSBuilderTestContext context
    ) {
        var expectedSource = SourceLine.of("server_scripts/main.js", 9);
        var json = serialize(context.miningRecipe("missing_deposit_block_id")
                .block("create_rns:definitely_missing_block", expectedSource)
                .yield(yield -> yield.item("diamond")));

        context.helper().assertValueEqual(json.get("deposit_block").getAsString(),
                "create_rns:definitely_missing_block", "missing deposit block id");
        assertSourceLine(context.helper(), json, expectedSource, "missing deposit block id");
        assertRecipeFails(context.helper(), json, "Unknown registry key");
    }

    @RNSKubeJSBuilderTest
    private static void missingYieldsSerializeAndFailRecipeDecoding(RNSKubeJSBuilderTestContext context) {
        var expectedSource = SourceLine.of("server_scripts/main.js", 7);
        var json = serialize(context.miningRecipe("missing_yields")
                .block(VALID_DEPOSIT_BLOCK, expectedSource)
                .yield(yield -> {
                })
                .yield(yield -> yield.chance(0.5f)));

        assertSourceLine(context.helper(), json, expectedSource, "missing yields");
        assertRecipeFails(context.helper(), json, "No key yields");
    }

    @RNSKubeJSBuilderTest
    private static void missingDepositBlockSerializesAndFailsRecipeDecoding(RNSKubeJSBuilderTestContext context) {
        var expectedSource = SourceLine.of("server_scripts/main.js", 7);
        var json = serialize(context.miningRecipe("missing_block")
                .yield(yield -> yield.item("diamond"), expectedSource));

        assertSourceLine(context.helper(), json, expectedSource, "missing deposit block");
        assertRecipeFails(context.helper(), json, "No key deposit_block");
    }

    @RNSKubeJSBuilderTest
    private static void unregisteredRequiredItemsSerializeAndFailRecipeDecoding(RNSKubeJSBuilderTestContext context) {
        var json = serialize(context.miningRecipe("missing_required_item")
                .block(VALID_DEPOSIT_BLOCK)
                .yield(yield -> yield.item("create_rns:definitely_missing_item")));

        assertRecipeFails(context.helper(), json,
                "None of the items resolved: [create_rns:definitely_missing_item]");
    }

    @RNSKubeJSBuilderTest
    private static void invalidItemUsesItsCallSiteForKubeJSErrorAttribution(RNSKubeJSBuilderTestContext context) {
        var expectedSource = new SourceLine("server_scripts/main.js", 14);
        var json = serialize(context.miningRecipe("missing_required_item_source")
                .block(VALID_DEPOSIT_BLOCK)
                .yield(yield -> yield.item(
                        List.of("create_rns:definitely_missing_item"), 1, false, expectedSource)));

        var marker = json.getAsJsonObject(KubeRecipe.CHANGED_MARKER);
        context.helper().assertValueEqual(marker.get("source").getAsString(), expectedSource.source(),
                "invalid item error source");
        context.helper().assertValueEqual(marker.get("line").getAsInt(), expectedSource.line(),
                "invalid item error line");
        assertRecipeFails(context.helper(), json,
                "None of the items resolved: [create_rns:definitely_missing_item]");
    }

    @RNSKubeJSBuilderTest
    private static void missingCatalystUsesItsCallSiteForKubeJSErrorAttribution(
            RNSKubeJSBuilderTestContext context
    ) {
        var expectedSource = SourceLine.of("server_scripts/main.js", 15);
        var json = serialize(context.miningRecipe("missing_catalyst_source")
                .block(VALID_DEPOSIT_BLOCK)
                .yield(yield -> yield.item("diamond")
                        .catalyst("create_rns:definitely_missing_catalyst", expectedSource,
                                context.helper().raw().getLevel().registryAccess())));

        assertSourceLine(context.helper(), json, expectedSource, "missing catalyst");
        assertRecipeFails(context.helper(), json,
                "Failed to get element create_rns:definitely_missing_catalyst");
    }

    @RNSKubeJSBuilderTest
    private static void malformedCatalystProducesOneAttributedRecipeError(
            RNSKubeJSBuilderTestContext context
    ) {
        var expectedSource = SourceLine.of("server_scripts/main.js", 15);
        var json = serialize(context.miningRecipe("malformed_catalyst_source")
                .block(VALID_DEPOSIT_BLOCK)
                .yield(yield -> yield.item("diamond")
                        .catalyst("create_rns: malformed", expectedSource,
                                context.helper().raw().getLevel().registryAccess())));

        context.helper().assertTrue(json.has("yields"),
                "Malformed catalyst should not abort construction of the recipe yields");
        assertSourceLine(context.helper(), json, expectedSource, "malformed catalyst");
        assertRecipeFails(context.helper(), json, "Not a valid resource location");
    }

    @RNSKubeJSBuilderTest
    private static void invalidChanceProducesOneAttributedRecipeError(RNSKubeJSBuilderTestContext context) {
        var expectedSource = SourceLine.of("server_scripts/main.js", 15);
        var json = serialize(context.miningRecipe("invalid_chance_source")
                .block(VALID_DEPOSIT_BLOCK)
                .yield(yield -> yield.chance(-0.02f, expectedSource).item("diamond")));

        CodecHelper.assertFails(context.helper().raw(), MiningRecipeKubeSchema.CHANCE_COMPONENT.codec(),
                CodecHelper.registries(context.helper().raw()), "-0.02", "outside of range [0.0:1.0]");
        context.helper().assertTrue(json.has("yields"),
                "Invalid chance should not abort construction of the recipe yields");
        CodecHelper.assertFloat(context.helper().raw(),
                json.getAsJsonArray("yields").get(0).getAsJsonObject().get("chance").getAsFloat(), -0.02f,
                "invalid chance value");
        assertSourceLine(context.helper(), json, expectedSource, "invalid chance");
        assertRecipeFails(context.helper(), json, "outside of range [0.0:1.0]");
    }

    @RNSKubeJSBuilderTest
    private static void invalidItemWeightProducesOneAttributedRecipeError(RNSKubeJSBuilderTestContext context) {
        var expectedSource = SourceLine.of("server_scripts/main.js", 63);
        var json = serialize(context.miningRecipe("invalid_item_weight_source")
                .block(VALID_DEPOSIT_BLOCK)
                .yield(yield -> yield.item(List.of("diamond"), -5, false, expectedSource)));

        CodecHelper.assertFails(context.helper().raw(), MiningRecipeKubeSchema.WEIGHT_COMPONENT.codec(),
                CodecHelper.registries(context.helper().raw()), "-5", "outside of range [1:2147483647]");
        context.helper().assertTrue(json.has("yields"),
                "Invalid item weight should not abort construction of the recipe yields");
        context.helper().assertValueEqual(yieldItem(json.getAsJsonArray("yields"), 0).get("weight").getAsInt(), -5,
                "invalid item weight value");
        assertSourceLine(context.helper(), json, expectedSource, "invalid item weight");
        assertRecipeFails(context.helper(), json, "outside of range [1:2147483647]");
    }

    @RNSKubeJSBuilderTest
    private static void durabilityBoundariesAndInvalidComponentValuesSerializeAsExpected(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();

        var valid = context.miningRecipe("durability_boundary")
                .block(VALID_DEPOSIT_BLOCK)
                .durability(8, 3, 1f)
                .yield(yield -> yield.item("diamond"));
        var validJson = serialize(valid);
        var durability = validJson.getAsJsonObject("durability");
        helper.assertValueEqual(durability.get("core").getAsLong(), 8L, "boundary durability core");
        helper.assertValueEqual(durability.get("edge").getAsLong(), 3L, "boundary durability edge");
        CodecHelper.assertFloat(helper.raw(), durability.get("random_spread").getAsFloat(), 1f,
                "boundary durability spread");

        assertDroppedDurability(helper, context.miningRecipe("invalid_edge")
                .block(VALID_DEPOSIT_BLOCK)
                .durability(8, 0, 0.25f)
                .yield(yield -> yield.item("diamond")));
        assertDroppedDurability(helper, context.miningRecipe("invalid_spread_negative")
                .block(VALID_DEPOSIT_BLOCK)
                .durability(8, 3, -0.01f)
                .yield(yield -> yield.item("diamond")));
        assertDroppedDurability(helper, context.miningRecipe("invalid_spread_above_one")
                .block(VALID_DEPOSIT_BLOCK)
                .durability(8, 3, 1.01f)
                .yield(yield -> yield.item("diamond")));
    }

    @RNSKubeJSBuilderTest
    private static void serializesAdvancedYieldFieldsAndPreservesOrder(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("advanced_yields");

        recipe.block(VALID_DEPOSIT_BLOCK)
                .yield(yield -> yield.chance(0.25f)
                        .item("diamond")
                        .compatItem(List.of("compatmod:raw_ore", "#planks"), 2)
                        .catalyst("create_rns:alpha")
                        .catalyst("create_rns:beta")
                        .jeiSlotColor("#123abc"))
                .yield(yield -> yield.item("emerald").jeiSlotColor(EXPLICIT_SLOT_COLOR));

        var json = serialize(recipe);
        var yields = json.getAsJsonArray("yields");
        helper.assertValueEqual(yields.size(), 2, "yield count");

        var firstYield = yields.get(0).getAsJsonObject();
        helper.assertTrue(firstYield.has("chance"), "Expected explicit chance to serialize");
        CodecHelper.assertFloat(helper.raw(), firstYield.get("chance").getAsFloat(), 0.25f, "serialized yield chance");
        helper.assertValueEqual(firstYield.get("jei_slot_color").getAsInt(), STRING_SLOT_COLOR,
                "string slot color should serialize as opaque ARGB");

        var catalysts = firstYield.getAsJsonArray("catalysts");
        helper.assertValueEqual(catalysts.size(), 2, "catalyst count");
        helper.assertValueEqual(catalysts.get(0).getAsString(), "create_rns:alpha", "first catalyst id");
        helper.assertValueEqual(catalysts.get(1).getAsString(), "create_rns:beta", "second catalyst id");

        var firstItems = firstYield.getAsJsonArray("items");
        helper.assertValueEqual(firstItems.size(), 2, "first yield item entry count");
        helper.assertValueEqual(firstItems.get(0).getAsJsonObject().get("item").getAsString(),
                "minecraft:diamond", "first weighted item");

        var compatItem = firstItems.get(1).getAsJsonObject();
        helper.assertTrue(compatItem.get("compat").getAsBoolean(), "Compat entry should serialize compat flag");
        helper.assertValueEqual(compatItem.get("weight").getAsInt(), 2, "compat item weight");
        assertStringArray(helper, compatItem.getAsJsonArray("item"),
                "compat item candidates", "compatmod:raw_ore", "#minecraft:planks");

        var secondYield = yields.get(1).getAsJsonObject();
        helper.assertValueEqual(secondYield.get("jei_slot_color").getAsInt(), EXPLICIT_SLOT_COLOR,
                "explicit ARGB slot color");
    }

    @RNSKubeJSBuilderTest
    private static void serializesEveryYieldItemOverload(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("yield_item_overloads");

        recipe.block(VALID_DEPOSIT_BLOCK)
                .yield(yield -> yield.item("diamond"))
                .yield(yield -> yield.item("emerald", 2))
                .yield(yield -> yield.item(List.of("diamond", "#planks")))
                .yield(yield -> yield.item(List.of("emerald", "#logs"), 3))
                .yield(yield -> yield.compatItem("compatmod:raw_ore"))
                .yield(yield -> yield.compatItem("compatmod:rich_ore", 4))
                .yield(yield -> yield.compatItem(List.of("compatmod:raw_ore", "#planks")))
                .yield(yield -> yield.compatItem(List.of("compatmod:rich_ore", "#logs"), 5));

        var json = serialize(recipe);
        var yields = json.getAsJsonArray("yields");
        helper.assertValueEqual(yields.size(), 8, "yield overload count");

        assertWeightedItemObject(helper, yieldItem(yields, 0), "item(String)", "minecraft:diamond", false, null);
        assertWeightedItemObject(helper, yieldItem(yields, 1), "item(String, int)", "minecraft:emerald", false, 2);
        assertWeightedItemArray(helper, yieldItem(yields, 2), "item(List<String>)",
                List.of("minecraft:diamond", "#minecraft:planks"), false, null);
        assertWeightedItemArray(helper, yieldItem(yields, 3), "item(List<String>, int)",
                List.of("minecraft:emerald", "#minecraft:logs"), false, 3);
        assertWeightedItemObject(helper, yieldItem(yields, 4), "compatItem(String)", "compatmod:raw_ore", true, null);
        assertWeightedItemObject(helper, yieldItem(yields, 5), "compatItem(String, int)",
                "compatmod:rich_ore", true, 4);
        assertWeightedItemArray(helper, yieldItem(yields, 6), "compatItem(List<String>)",
                List.of("compatmod:raw_ore", "#minecraft:planks"), true, null);
        assertWeightedItemArray(helper, yieldItem(yields, 7), "compatItem(List<String>, int)",
                List.of("compatmod:rich_ore", "#minecraft:logs"), true, 5);

        var parsed = parseRecipe(helper, json, "recipe using every yield item overload");
        helper.assertValueEqual(parsed.getYields().size(), 8, "parsed yield count");
    }

    @RNSKubeJSBuilderTest
    private static void serializesExplicitTopLevelConfiguration(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("explicit_top_level");

        recipe.block(VALID_DEPOSIT_BLOCK)
                .overworld()
                .replaceWhenDepleted("cobblestone")
                .durability(8, 3, 0.25f)
                .yield(yield -> yield.item("diamond"));

        var json = serialize(recipe);
        helper.assertValueEqual(json.get("deposit_block").getAsString(), VALID_DEPOSIT_BLOCK, "deposit block");
        helper.assertValueEqual(json.get("dimension").getAsString(), "minecraft:overworld", "dimension");
        helper.assertValueEqual(json.get("replace_when_depleted").getAsString(), "minecraft:cobblestone",
                "replacement block");

        var durability = json.getAsJsonObject("durability");
        helper.assertValueEqual(durability.get("core").getAsLong(), 8L, "durability core");
        helper.assertValueEqual(durability.get("edge").getAsLong(), 3L, "durability edge");
        CodecHelper.assertFloat(helper.raw(), durability.get("random_spread").getAsFloat(), 0.25f,
                "durability random spread");

        var parsed = parseRecipe(helper, json, "explicit top-level mining recipe");
        helper.assertTrue(parsed.getDimension() == Level.OVERWORLD, "Expected explicit overworld dimension");
        helper.assertTrue(parsed.getReplacementBlock() == Blocks.COBBLESTONE,
                "Expected replacement block to parse as cobblestone");
        helper.assertValueEqual(parsed.getDurability().core(), 8L, "parsed durability core");
        helper.assertValueEqual(parsed.getDurability().edge(), 3L, "parsed durability edge");
        CodecHelper.assertFloat(helper.raw(), parsed.getDurability().randomSpread(), 0.25f, "parsed durability spread");
    }

    @RNSKubeJSBuilderTest
    private static void theLastDimensionCallWins(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("last_dimension_wins");

        recipe.block(VALID_DEPOSIT_BLOCK)
                .overworld()
                .nether()
                .yield(yield -> yield.item("diamond"));

        var json = serialize(recipe);
        helper.assertValueEqual(json.get("dimension").getAsString(), "minecraft:the_nether", "last dimension");

        var parsed = parseRecipe(helper, json, "recipe with overwritten dimension");
        helper.assertTrue(parsed.getDimension() == Level.NETHER, "Expected final dimension to be nether");
    }

    @RNSKubeJSBuilderTest
    private static void yieldMethodIgnoresEmptyBuildersAndKeepsLaterValidYields(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("ignores_empty_yields");

        recipe.block(VALID_DEPOSIT_BLOCK)
                .yield(yield -> {
                })
                .yield(yield -> yield.chance(0.5f).catalyst("create_rns:alpha"))
                .yield(yield -> yield.item("diamond"));

        var json = serialize(recipe);
        var yields = json.getAsJsonArray("yields");
        helper.assertValueEqual(yields.size(), 1, "Only non-empty yields should serialize");
        helper.assertValueEqual(yieldItem(yields, 0).get("item").getAsString(), "minecraft:diamond",
                "Valid yield should be retained");
    }

    @RNSKubeJSBuilderTest
    private static void jeiSlotColorRejectsInvalidStrings(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("invalid_yield_arguments");

        assertThrows(helper, IllegalArgumentException.class, "JEI slot color must be in the form #rrggbb",
                () -> recipe.yield(yield -> yield.jeiSlotColor("123456")));
        assertThrows(helper, IllegalArgumentException.class, "JEI slot color must be in the form #rrggbb",
                () -> recipe.yield(yield -> yield.jeiSlotColor("#12zzzz")));
    }

    @RNSKubeJSBuilderTest
    private static void failedYieldCallbacksDoNotLeavePartialYieldState(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("failed_yield_callback");

        assertThrows(helper, IllegalArgumentException.class, "JEI slot color must be in the form #rrggbb",
                () -> recipe.yield(yield -> yield.item("diamond").jeiSlotColor("invalid")));

        recipe.block(VALID_DEPOSIT_BLOCK)
                .yield(yield -> yield.item("emerald"));

        var json = serialize(recipe);
        var yields = json.getAsJsonArray("yields");
        helper.assertValueEqual(yields.size(), 1, "failed yield callbacks should not leave partial yield state");
        helper.assertValueEqual(yieldItem(yields, 0).get("item").getAsString(), "minecraft:emerald",
                "later valid yield should serialize normally");
    }

    private static void assertStringArray(
            RNSKubeJSBuilderTestHelper helper, JsonArray actual, String valueName, String... expected
    ) {
        helper.assertValueEqual(actual.size(), expected.length, valueName + " size");
        for (var i = 0; i < expected.length; i++) {
            helper.assertValueEqual(actual.get(i).getAsString(), expected[i], valueName + " [" + i + "]");
        }
    }

    private static void assertThrows(
            RNSKubeJSBuilderTestHelper helper, Class<? extends Throwable> expectedType, String expectedMessagePart,
            Runnable action
    ) {
        Throwable thrown = null;

        try {
            action.run();
        } catch (Throwable t) {
            thrown = t;
        }

        helper.assertTrue(thrown != null, "Expected exception " + expectedType.getSimpleName());
        helper.assertTrue(expectedType.isInstance(thrown),
                "Expected " + expectedType.getSimpleName() + ", got "
                        + (thrown == null ? "null" : thrown.getClass().getSimpleName()));
        helper.assertTrue(thrown.getMessage() != null && thrown.getMessage().contains(expectedMessagePart),
                "Expected exception containing '" + expectedMessagePart + "', got "
                        + (thrown == null ? "null" : thrown.getMessage()));
    }

    private static void assertWeightedItemArray(
            RNSKubeJSBuilderTestHelper helper, JsonObject item, String valueName, List<String> expectedIds, boolean compat,
            Integer expectedWeight
    ) {
        assertStringArray(helper, item.getAsJsonArray("item"), valueName + " candidates",
                expectedIds.toArray(String[]::new));
        assertWeightedItemFlags(helper, item, valueName, compat, expectedWeight);
    }

    private static void assertWeightedItemFlags(
            RNSKubeJSBuilderTestHelper helper, JsonObject item, String valueName, boolean compat, Integer expectedWeight
    ) {
        helper.assertValueEqual(item.has("compat"), compat, valueName + " compat presence");
        if (compat) {
            helper.assertTrue(item.get("compat").getAsBoolean(), valueName + " compat flag");
        }

        helper.assertValueEqual(item.has("weight"), expectedWeight != null, valueName + " weight presence");
        if (expectedWeight != null) {
            helper.assertValueEqual(item.get("weight").getAsInt(), expectedWeight, valueName + " weight");
        }
    }

    private static void assertWeightedItemObject(
            RNSKubeJSBuilderTestHelper helper, JsonObject item, String valueName, String expectedId, boolean compat,
            Integer expectedWeight
    ) {
        helper.assertValueEqual(item.get("item").getAsString(), expectedId, valueName + " item");
        assertWeightedItemFlags(helper, item, valueName, compat, expectedWeight);
    }

    private static JsonObject codecJson(JsonObject json) {
        var copy = json.deepCopy();
        copy.remove("type");
        copy.remove(KubeRecipe.CHANGED_MARKER);
        return copy;
    }

    private static MiningRecipe parseRecipe(RNSKubeJSBuilderTestHelper helper, JsonObject json, String valueName) {
        return CodecHelper.assertParses(helper.raw(), MiningRecipe.CODEC.codec(),
                CodecHelper.registries(helper.raw()), codecJson(json).toString(), valueName);
    }

    private static void assertRecipeFails(
            RNSKubeJSBuilderTestHelper helper, JsonObject json, String expectedMessagePart
    ) {
        helper.assertTrue(json.has(KubeRecipe.CHANGED_MARKER),
                "Invalid KubeJS recipe should retain its source marker for error attribution");
        CodecHelper.assertFails(helper.raw(), MiningRecipe.CODEC.codec(),
                CodecHelper.registries(helper.raw()), codecJson(json).toString(), expectedMessagePart);
    }

    private static void assertSourceLine(
            RNSKubeJSBuilderTestHelper helper, JsonObject json, SourceLine expected, String valueName
    ) {
        var marker = json.getAsJsonObject(KubeRecipe.CHANGED_MARKER);
        helper.assertValueEqual(marker.get("source").getAsString(), expected.source(), valueName + " error source");
        helper.assertValueEqual(marker.get("line").getAsInt(), expected.line(), valueName + " error line");
    }

    private static void assertSourceLine(
            RNSKubeJSBuilderTestHelper helper, SourceLine actual, SourceLine expected, String valueName
    ) {
        helper.assertValueEqual(actual.source(), expected.source(), valueName + " source");
        helper.assertValueEqual(actual.line(), expected.line(), valueName + " line");
    }

    private static JsonObject serialize(MiningRecipeKubeRecipe recipe) {
        recipe.serializeChanges();
        return recipe.json.deepCopy();
    }

    private static void assertDroppedDurability(RNSKubeJSBuilderTestHelper helper, MiningRecipeKubeRecipe recipe) {
        var json = serialize(recipe);
        helper.assertFalse(json.has("durability"), "Invalid durability values should not serialize");

        var parsed = parseRecipe(helper, json, "recipe with dropped invalid durability");
        helper.assertValueEqual(parsed.getDurability().core(), 0L, "default durability core");
        helper.assertValueEqual(parsed.getDurability().edge(), 0L, "default durability edge");
        CodecHelper.assertFloat(helper.raw(), parsed.getDurability().randomSpread(), 0f, "default durability spread");
    }

    private static JsonObject yieldItem(JsonArray yields, int index) {
        return yields.get(index).getAsJsonObject()
                .getAsJsonArray("items")
                .get(0)
                .getAsJsonObject();
    }
}

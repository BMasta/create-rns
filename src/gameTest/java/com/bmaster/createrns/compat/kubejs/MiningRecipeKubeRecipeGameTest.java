package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.util.CodecHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
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

        recipe.block("stone")
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
    private static void omittedDefaultsProduceMinimalValidRecipeJson(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("minimal_defaults");

        recipe.block("stone")
                .yield(yield -> yield.item("diamond"));

        var json = serialize(recipe);
        helper.assertValueEqual(json.get("type").getAsString(), CreateRNS.asResource("mining").toString(), "recipe type");
        helper.assertValueEqual(json.get("deposit_block").getAsString(), "minecraft:stone", "deposit block");
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
        helper.assertTrue(parsed.getDepositBlock() == Blocks.STONE, "Expected parsed deposit block to be stone");
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

        recipe.block("stone")
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
    private static void serializeChangesRejectsAirDepositBlock(RNSKubeJSBuilderTestContext context) {
        assertThrows(context.helper(), IllegalStateException.class,
                "Mining recipe must specify a deposit block via block(...)",
                () -> context.miningRecipe("air_deposit_block")
                        .block("air")
                        .yield(yield -> yield.item("diamond"))
                        .serializeChanges());
    }

    @RNSKubeJSBuilderTest
    private static void serializeChangesRequiresAtLeastOneNonEmptyYield(RNSKubeJSBuilderTestContext context) {
        assertThrows(context.helper(), IllegalStateException.class,
                "Mining recipe must define at least one yield",
                () -> context.miningRecipe("missing_yields")
                        .block("stone")
                        .yield(yield -> {
                        })
                        .yield(yield -> yield.chance(0.5f))
                        .serializeChanges());
    }

    @RNSKubeJSBuilderTest
    private static void serializeChangesRequiresDepositBlock(RNSKubeJSBuilderTestContext context) {
        assertThrows(context.helper(), IllegalStateException.class,
                "Mining recipe must specify a deposit block via block(...)",
                () -> context.miningRecipe("missing_block")
                        .yield(yield -> yield.item("diamond"))
                        .serializeChanges());
    }

    @RNSKubeJSBuilderTest
    private static void serializeChangesRejectsUnregisteredRequiredItems(RNSKubeJSBuilderTestContext context) {
        assertThrows(context.helper(), IllegalStateException.class,
                "item(...) candidates do not resolve to a registered item: "
                        + "[[create_rns:definitely_missing_item]]",
                () -> context.miningRecipe("missing_required_item")
                        .block("stone")
                        .yield(yield -> yield.item("create_rns:definitely_missing_item"))
                        .serializeChanges());
    }

    @RNSKubeJSBuilderTest
    private static void durabilityBoundariesAndInvalidComponentValuesSerializeAsExpected(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();

        var valid = context.miningRecipe("durability_boundary")
                .block("stone")
                .durability(8, 3, 1f)
                .yield(yield -> yield.item("diamond"));
        var validJson = serialize(valid);
        var durability = validJson.getAsJsonObject("durability");
        helper.assertValueEqual(durability.get("core").getAsLong(), 8L, "boundary durability core");
        helper.assertValueEqual(durability.get("edge").getAsLong(), 3L, "boundary durability edge");
        CodecHelper.assertFloat(helper.raw(), durability.get("random_spread").getAsFloat(), 1f,
                "boundary durability spread");

        assertDroppedDurability(helper, context.miningRecipe("invalid_edge")
                .block("stone")
                .durability(8, 0, 0.25f)
                .yield(yield -> yield.item("diamond")));
        assertDroppedDurability(helper, context.miningRecipe("invalid_spread_negative")
                .block("stone")
                .durability(8, 3, -0.01f)
                .yield(yield -> yield.item("diamond")));
        assertDroppedDurability(helper, context.miningRecipe("invalid_spread_above_one")
                .block("stone")
                .durability(8, 3, 1.01f)
                .yield(yield -> yield.item("diamond")));
    }

    @RNSKubeJSBuilderTest
    private static void serializesAdvancedYieldFieldsAndPreservesOrder(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("advanced_yields");

        recipe.block("stone")
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

        recipe.block("stone")
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

        recipe.block("stone")
                .overworld()
                .replaceWhenDepleted("cobblestone")
                .durability(8, 3, 0.25f)
                .yield(yield -> yield.item("diamond"));

        var json = serialize(recipe);
        helper.assertValueEqual(json.get("deposit_block").getAsString(), "minecraft:stone", "deposit block");
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

        recipe.block("stone")
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

        recipe.block("stone")
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
    private static void yieldBuilderRejectsInvalidArguments(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("invalid_yield_arguments");

        assertThrows(helper, IllegalArgumentException.class, "Yield chance must be between 0 and 1",
                () -> recipe.yield(yield -> yield.chance(-0.01f)));
        assertThrows(helper, IllegalArgumentException.class, "Yield chance must be between 0 and 1",
                () -> recipe.yield(yield -> yield.chance(1.01f)));
        assertThrows(helper, IllegalArgumentException.class, "Yield item weight must be positive",
                () -> recipe.yield(yield -> yield.item("diamond", 0)));
        assertThrows(helper, IllegalArgumentException.class, "Yield item weight must be positive",
                () -> recipe.yield(yield -> yield.compatItem(List.of("compatmod:ore"), 0)));
        assertThrows(helper, IllegalArgumentException.class, "Catalyst id cannot be blank",
                () -> recipe.yield(yield -> yield.catalyst(" ")));
        assertThrows(helper, IllegalArgumentException.class, "Invalid catalyst id: create_rns:bad id",
                () -> recipe.yield(yield -> yield.catalyst("create_rns:bad id")));
        assertThrows(helper, IllegalArgumentException.class, "JEI slot color must be in the form #rrggbb",
                () -> recipe.yield(yield -> yield.jeiSlotColor("123456")));
        assertThrows(helper, IllegalArgumentException.class, "JEI slot color must be in the form #rrggbb",
                () -> recipe.yield(yield -> yield.jeiSlotColor("#12zzzz")));
    }

    @RNSKubeJSBuilderTest
    private static void failedYieldCallbacksDoNotLeavePartialYieldState(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var recipe = context.miningRecipe("failed_yield_callback");

        assertThrows(helper, IllegalArgumentException.class, "Yield item weight must be positive",
                () -> recipe.yield(yield -> yield.item("diamond", 0)));

        recipe.block("stone")
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

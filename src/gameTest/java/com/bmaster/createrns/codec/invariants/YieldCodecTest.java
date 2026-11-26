package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystHandler.CatalystEffects;
import com.bmaster.createrns.util.CodecHelper;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.Collections;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class YieldCodecTest {
    private static final TagKey<Item> PLANKS_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:planks"));

    @GameTest(template = "empty16x16")
    public void weightedItemParsesAndInitializesFromItemCandidate(GameTestHelper helper) {
        var weightedItem = CodecHelper.assertParses(helper, Yield.WeightedItemStack.CODEC,
                CodecHelper.json(), """
                        {
                          "item": "minecraft:diamond",
                          "weight": 5
                        }
                        """, "weighted item");

        helper.assertValueEqual(weightedItem.weight, 5, "weighted item weight");
        CodecHelper.assertSame(helper, Items.AIR, weightedItem.item,
                "Direct weighted items should stay unresolved until initialization");
        helper.assertTrue(weightedItem.initialize(helper.getLevel().registryAccess()),
                "Direct weighted items should initialize against live registry data");
        CodecHelper.assertSame(helper, Items.DIAMOND, weightedItem.item, "resolved weighted item");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void scaledCountRoundsUpOnlyWithinTolerance(GameTestHelper helper) {
        var entry = CodecHelper.assertParses(helper, Yield.WeightedItemStack.CODEC, CodecHelper.json(),
                """
                {"item": "minecraft:diamond", "count": 2}
                """, "two-item yield");
        helper.assertValueEqual(entry.scaledCount(2.4945), 4, "4.989 is outside the tolerance");
        helper.assertValueEqual(entry.scaledCount(2.495), 5, "4.99 is exactly at the threshold");
        helper.assertValueEqual(entry.scaledCount(2.4975), 5, "4.995 is inside the tolerance");
        helper.assertValueEqual(entry.scaledCount(0.495), 1, "0.99 produces one item");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void weightedItemRejectsUnregisteredStrictDirectItemDuringDecoding(GameTestHelper helper) {
        CodecHelper.assertFails(helper, Yield.WeightedItemStack.CODEC, CodecHelper.json(), """
                {
                  "item": "create_rns:definitely_missing_item",
                  "weight": 1
                }
                """, "None of the item resources or tags resolved: [create_rns:definitely_missing_item]");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void weightedItemInitializesFromLiveItemTags(GameTestHelper helper) {
        var weightedItem = CodecHelper.assertParses(helper, Yield.WeightedItemStack.CODEC,
                CodecHelper.json(), """
                        {
                          "item": "#minecraft:planks",
                          "weight": 2
                        }
                        """, "tag-backed weighted item");

        CodecHelper.assertSame(helper, Items.AIR, weightedItem.item,
                "Tag-backed weighted items should stay unresolved until initialization");
        helper.assertTrue(weightedItem.initialize(helper.getLevel().registryAccess()),
                "Tag-backed weighted items should initialize against live registry data");
        helper.assertTrue(BuiltInRegistries.ITEM.wrapAsHolder(weightedItem.item).is(PLANKS_TAG),
                "Resolved weighted item should come from the live planks tag");
        helper.assertTrue(weightedItem.item != Items.AIR,
                "Resolved weighted item from a live tag should not fall back to air");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void compatWeightedItemAllowsMissingCandidates(GameTestHelper helper) {
        var weightedItem = CodecHelper.assertParses(helper, Yield.WeightedItemStack.CODEC,
                CodecHelper.json(), """
                        {
                          "item": "create_rns:definitely_missing_item",
                          "compat": true,
                          "weight": 1
                        }
                        """, "compat weighted item");

        helper.assertTrue(weightedItem.compat, "Compat weighted item should preserve the compat flag");
        CodecHelper.assertSame(helper, Items.AIR, weightedItem.item,
                "Compat weighted item should fall back to air when unresolved");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void yieldParsesWithDefaultsAndInitializesItems(GameTestHelper helper) {
        var yield = CodecHelper.assertParses(helper, Yield.CODEC, CodecHelper.registries(helper), """
                        {
                          "items": [
                            {
                              "item": "minecraft:diamond",
                              "count": 99,
                              "weight": 2
                            }
                          ],
                          "catalysts": ["create_rns:overclock"]
                        }
                        """, "yield");

        CodecHelper.assertFloat(helper, yield.chance, 1.0f, "yield chance");
        helper.assertValueEqual(yield.items.size(), 1, "yield weighted item count");
        helper.assertValueEqual(yield.getCRSIds().size(), 1, "yield catalyst count");
        helper.assertValueEqual(yield.getCRSIds().getFirst(), CreateRNS.asResource("overclock"), "yield catalyst id");
        helper.assertValueEqual(yield.slotColor, 0, "yield slot color");

        var restored = roundTrip(Yield.STREAM_CODEC, yield, helper.getLevel().registryAccess());
        helper.assertValueEqual(restored.items.getFirst().count, 99, "stream codec stack count");
        var encoded = Yield.CODEC.encodeStart(CodecHelper.registries(helper), yield).getOrThrow();
        var entry = encoded.getAsJsonObject().getAsJsonArray("items").get(0).getAsJsonObject();
        helper.assertValueEqual(entry.get("count").getAsInt(), 99, "encoded count is independent of weight");
        helper.assertValueEqual(restored.getCRSIds(), yield.getCRSIds(),
                "stream codec catalyst ids");
        helper.assertTrue(yield.initialize(helper.getLevel().registryAccess()),
                "Yield initialization should succeed when referenced catalysts exist in the live registry");
        CodecHelper.assertSame(helper, Items.DIAMOND, yield.items.getFirst().item,
                "initialized yield item");
        helper.assertValueEqual(yield.roll(helper.getLevel().random, new CatalystEffects(1f, 1d)).getCount(), 99,
                "rolled stack count");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void explicitYieldChanceSurvivesCodecRoundTrip(GameTestHelper helper) {
        var ops = CodecHelper.registries(helper);
        var yield = CodecHelper.assertParses(helper, Yield.CODEC, ops, """
                {"chance": 0.25, "items": [{"item": "minecraft:diamond"}]}
                """, "yield with explicit chance");
        CodecHelper.assertFloat(helper, yield.chance, 0.25f, "decoded yield chance");
        var json = Yield.CODEC.encodeStart(ops, yield).getOrThrow().getAsJsonObject();
        helper.assertTrue(json.has("chance"), "Encoding should preserve the chance field");
        CodecHelper.assertFloat(helper, json.get("chance").getAsFloat(), 0.25f, "encoded yield chance");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void zeroChanceMultiplierSuppressesGuaranteedYield(GameTestHelper helper) {
        var yield = CodecHelper.assertParses(helper, Yield.CODEC, CodecHelper.registries(helper), """
                {"items": [{"item": "minecraft:diamond"}]}
                """, "guaranteed yield");
        helper.assertTrue(yield.initialize(helper.getLevel().registryAccess()), "Diamond yield should initialize");
        helper.assertTrue(yield.roll(RandomSource.create(0), new CatalystEffects(1f, 1d)).is(Items.DIAMOND),
                "Neutral effects should produce a diamond");
        helper.assertTrue(yield.roll(RandomSource.create(0), new CatalystEffects(0f, 1d)).isEmpty(),
                "An active zero chance multiplier should suppress the same roll");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void largeWeightsPreserveTheirCombinedTotal(GameTestHelper helper) {
        var yield = CodecHelper.assertParses(helper, Yield.CODEC, CodecHelper.registries(helper), """
                {"items": [{"item": "stone", "weight": 1500000000}, {"item": "diamond", "weight": 1500000000}]}
                """, "yield with a total weight above the integer limit");
        helper.assertValueEqual(yield.getTotalWeight(), 3_000_000_000L, "Sum of valid item weights");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void guaranteedRollAtUpperBoundaryReturnsLastItem(GameTestHelper helper) {
        // Twelve equal float weights sum to slightly less than the largest possible chance roll.
        var firstEntries = Collections.nCopies(11, "{\"item\":\"stone\"}");
        var json = "{\"items\":[" + String.join(",", firstEntries) + ",{\"item\":\"diamond\",\"count\":3}]}";
        var yield = CodecHelper.assertParses(helper, Yield.CODEC, CodecHelper.registries(helper), json, "equal weights");
        helper.assertTrue(yield.initialize(helper.getLevel().registryAccess()), "Weighted items should resolve");
        var random = new LegacyRandomSource(0L) {
            @Override
            public float nextFloat() {
                return Math.nextDown(1f);
            }
        };
        var stack = yield.roll(random, new CatalystEffects(1f, 2d));
        helper.assertTrue(stack.is(Items.DIAMOND), "The final item covers the rounding gap");
        helper.assertValueEqual(stack.getCount(), 6, "The fallback item receives the count multiplier");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void yieldRetainsResolvableWeightedItemsWhenOthersFail(GameTestHelper helper) {
        var yield = CodecHelper.assertParses(helper, Yield.CODEC, CodecHelper.json(), """
                        {
                          "items": [
                            {
                              "item": "create_rns:definitely_missing_item",
                              "compat": true,
                              "weight": 1
                            },
                            {
                              "item": "#minecraft:planks",
                              "weight": 3
                            }
                          ]
                        }
                        """, "partially resolvable yield");

        helper.assertTrue(yield.initialize(helper.getLevel().registryAccess()),
                "Yield initialization should keep any weighted items that resolve");
        helper.assertValueEqual(yield.items.size(), 1, "remaining weighted item count");
        helper.assertTrue(BuiltInRegistries.ITEM.wrapAsHolder(yield.items.getFirst().item).is(PLANKS_TAG),
                "Remaining weighted item should come from the live planks tag");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void yieldRejectsUnknownCatalystDuringDecoding(GameTestHelper helper) {
        CodecHelper.assertFails(helper, Yield.CODEC, CodecHelper.registries(helper), """
                        {
                          "items": [
                            {
                              "item": "minecraft:diamond",
                              "weight": 1
                            }
                          ],
                          "catalysts": ["create_rns:missing_catalyst"]
                        }
                        """, "Failed to get element create_rns:missing_catalyst");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void yieldOmitsExplicitlyEmptyCatalysts(GameTestHelper helper) {
        var yield = CodecHelper.assertParses(helper, Yield.CODEC, CodecHelper.registries(helper), """
                {
                  "items": [
                    {
                      "item": "minecraft:diamond"
                    }
                  ],
                  "catalysts": []
                }
                """, "yield with empty catalysts");

        var encoded = Yield.CODEC.encodeStart(CodecHelper.registries(helper), yield).result().orElse(null);
        helper.assertTrue(encoded != null, "Yield with empty catalysts should encode");
        helper.assertFalse(encoded.getAsJsonObject().has("catalysts"),
                "Empty catalyst list should be omitted during encoding");

        var restored = roundTrip(Yield.STREAM_CODEC, yield, helper.getLevel().registryAccess());
        helper.assertTrue(restored.getCRSes().isEmpty(), "Stream codec should preserve an empty catalyst list");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void yieldRejectsInvalidWeightedItemDefinition(GameTestHelper helper) {
        CodecHelper.assertFails(helper, Yield.CODEC, CodecHelper.json(), """
                        {
                          "items": [
                            {
                              "item": [],
                              "weight": 1
                            }
                          ]
                        }
                        """, "No item resources or tags specified");
        helper.succeed();
    }

    private static <T> T roundTrip(
            StreamCodec<RegistryFriendlyByteBuf, T> codec, T value, RegistryAccess access
    ) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), access, ConnectionType.NEOFORGE);
        codec.encode(buffer, value);
        return codec.decode(buffer);
    }
}

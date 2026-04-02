package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield;
import com.bmaster.createrns.util.CodecHelper;
import com.bmaster.createrns.util.LogCapture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class YieldCodecTest {
    private static final TagKey<Item> PLANKS_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:planks"));

    @GameTest(template = "empty16x16")
    public void weightedItemParsesAndInitializesFromItemCandidate(GameTestHelper helper) {
        var weightedItem = CodecHelper.assertParses(helper, Yield.WeightedItem.CODEC,
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
    public void weightedItemRejectsUnresolvableStrictItem(GameTestHelper helper) {
        var weightedItem = CodecHelper.assertParses(helper, Yield.WeightedItem.CODEC,
                CodecHelper.json(), """
                        {
                          "item": "create_rns:definitely_missing_item",
                          "weight": 1
                        }
                        """, "strict weighted item");

        try (var logs = LogCapture.capture(CreateRNS.LOGGER.getName())) {
            helper.assertFalse(weightedItem.initialize(helper.getLevel().registryAccess()),
                    "Strict weighted items should fail initialization when no candidates resolve");
            helper.assertTrue(logs.contains("Could not resolve item \"create_rns:definitely_missing_item\""),
                    "Missing strict weighted items should log the unresolved candidate");
        }
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void weightedItemInitializesFromLiveItemTags(GameTestHelper helper) {
        var weightedItem = CodecHelper.assertParses(helper, Yield.WeightedItem.CODEC,
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
        var weightedItem = CodecHelper.assertParses(helper, Yield.WeightedItem.CODEC,
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
        var yield = CodecHelper.assertParses(helper, Yield.CODEC, CodecHelper.json(), """
                        {
                          "items": [
                            {
                              "item": "minecraft:diamond",
                              "weight": 2
                            }
                          ],
                          "catalysts": ["overclock"]
                        }
                        """, "yield");

        CodecHelper.assertFloat(helper, yield.chance, 1.0f, "yield chance");
        helper.assertValueEqual(yield.items.size(), 1, "yield weighted item count");
        helper.assertTrue(yield.crsNames != null, "Yield catalyst list should be initialized");
        helper.assertValueEqual(yield.crsNames.size(), 1, "yield catalyst count");
        helper.assertValueEqual(yield.crsNames.getFirst(), "overclock", "yield catalyst name");
        helper.assertValueEqual(yield.slotColor, 0, "yield slot color");
        helper.assertTrue(yield.initialize(helper.getLevel().registryAccess()),
                "Yield initialization should succeed when referenced catalysts exist in the live registry");
        CodecHelper.assertSame(helper, Items.DIAMOND, yield.items.getFirst().item,
                "initialized yield item");
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
    public void yieldLogsErrorAndFailsWhenCatalystNameIsUnknown(GameTestHelper helper) {
        var yield = CodecHelper.assertParses(helper, Yield.CODEC, CodecHelper.json(), """
                        {
                          "items": [
                            {
                              "item": "minecraft:diamond",
                              "weight": 1
                            }
                          ],
                          "catalysts": ["missing_catalyst"]
                        }
                        """, "yield with missing catalyst");

        try (var logs = LogCapture.capture(CreateRNS.LOGGER.getName())) {
            helper.assertFalse(yield.initialize(helper.getLevel().registryAccess()),
                    "Yield initialization should fail when a referenced catalyst is missing");
            helper.assertTrue(logs.contains("unknown catalyst requirement set"),
                    "Missing catalysts should produce the expected error log");
            helper.assertTrue(logs.contains("missing_catalyst"),
                    "Missing catalyst logs should include the missing catalyst name");
        }
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
                        """, "No items or item tags specified");
        helper.succeed();
    }
}

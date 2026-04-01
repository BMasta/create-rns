package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield;
import com.bmaster.createrns.util.CodecHelper;
import com.bmaster.createrns.util.LogCapture;
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
                          "item_candidates": ["minecraft:diamond"],
                          "weight": 5
                        }
                        """, "weighted item");

        helper.assertValueEqual(weightedItem.weight, 5, "weighted item weight");
        helper.assertTrue(weightedItem.initialize(helper.getLevel().registryAccess()),
                "Weighted item should initialize from an item candidate");
        CodecHelper.assertSame(helper, Items.DIAMOND, weightedItem.getItem(), "resolved weighted item");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void weightedItemRejectsMissingCandidates(GameTestHelper helper) {
        CodecHelper.assertFails(helper, Yield.WeightedItem.CODEC, CodecHelper.json(), """
                        {
                          "item_candidates": [],
                          "tag_candidates": [],
                          "weight": 1
                        }
                        """, "Weighted item must define at least an item or a tag");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void weightedItemInitializesFromLiveItemTags(GameTestHelper helper) {
        var weightedItem = CodecHelper.assertParses(helper, Yield.WeightedItem.CODEC,
                CodecHelper.json(), """
                        {
                          "tag_candidates": ["minecraft:planks"],
                          "weight": 2
                        }
                        """, "tag-backed weighted item");

        helper.assertTrue(weightedItem.initialize(helper.getLevel().registryAccess()),
                "Weighted item should initialize from live item tag contents");
        helper.assertTrue(weightedItem.getItem().builtInRegistryHolder().is(PLANKS_TAG),
                "Resolved weighted item should come from the live planks tag");
        helper.assertTrue(weightedItem.getItem() != Items.AIR,
                "Resolved weighted item from a live tag should not fall back to air");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void weightedItemLogsErrorWhenCandidatesDoNotResolve(GameTestHelper helper) {
        var weightedItem = CodecHelper.assertParses(helper, Yield.WeightedItem.CODEC,
                CodecHelper.json(), """
                        {
                          "item_candidates": ["create_rns:definitely_missing_item"],
                          "weight": 1
                        }
                        """, "unresolvable weighted item");

        try (var logs = LogCapture.capture(CreateRNS.LOGGER.getName())) {
            helper.assertFalse(weightedItem.initialize(helper.getLevel().registryAccess()),
                    "Weighted item initialization should fail when no candidates resolve");
            helper.assertTrue(logs.contains("Failed to resolve weighted item"),
                    "Unresolvable weighted items should emit a stable error log");
            helper.assertTrue(logs.contains("create_rns:definitely_missing_item"),
                    "Weighted item failure logs should include the missing candidate id");
        }
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void yieldParsesWithDefaultsAndInitializesItems(GameTestHelper helper) {
        var yield = CodecHelper.assertParses(helper, Yield.CODEC, CodecHelper.json(), """
                        {
                          "items": [
                            {
                              "item_candidates": ["minecraft:diamond"],
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
        CodecHelper.assertSame(helper, Items.DIAMOND, yield.items.getFirst().getItem(),
                "initialized yield item");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void yieldRetainsResolvableWeightedItemsWhenOthersFail(GameTestHelper helper) {
        var yield = CodecHelper.assertParses(helper, Yield.CODEC, CodecHelper.json(), """
                        {
                          "items": [
                            {
                              "item_candidates": ["create_rns:definitely_missing_item"],
                              "weight": 1
                            },
                            {
                              "tag_candidates": ["minecraft:planks"],
                              "weight": 3
                            }
                          ]
                        }
                        """, "partially resolvable yield");

        helper.assertTrue(yield.initialize(helper.getLevel().registryAccess()),
                "Yield initialization should keep any weighted items that resolve");
        helper.assertValueEqual(yield.items.size(), 1, "remaining weighted item count");
        helper.assertTrue(yield.items.getFirst().getItem().builtInRegistryHolder().is(PLANKS_TAG),
                "Remaining weighted item should come from the live planks tag");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void yieldLogsErrorAndFailsWhenCatalystNameIsUnknown(GameTestHelper helper) {
        var yield = CodecHelper.assertParses(helper, Yield.CODEC, CodecHelper.json(), """
                        {
                          "items": [
                            {
                              "item_candidates": ["minecraft:diamond"],
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
                              "weight": 1
                            }
                          ]
                        }
                        """, "Weighted item must define at least an item or a tag");
        helper.succeed();
    }
}

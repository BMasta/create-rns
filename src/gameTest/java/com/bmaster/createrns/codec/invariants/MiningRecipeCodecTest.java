package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.util.CodecHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MiningRecipeCodecTest {
    private static final TagKey<Item> PLANKS_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:planks"));

    @GameTest(template = "empty16x16")
    public void parsesRecipeWithReplacementAndDurabilityDefaults(GameTestHelper helper) {
        var recipe = CodecHelper.assertParses(helper, MiningRecipe.CODEC.codec(),
                CodecHelper.registries(helper), """
                        {
                          "deposit_block": "minecraft:stone",
                          "yields": [
                            {
                              "items": [
                                {
                                  "item_candidates": ["minecraft:diamond"],
                                  "weight": 1
                                }
                              ]
                            }
                          ]
                        }
                        """, "mining recipe");

        CodecHelper.assertSame(helper, Blocks.STONE, recipe.getDepositBlock(), "deposit block");
        CodecHelper.assertSame(helper, Blocks.AIR, recipe.getReplacementBlock(), "replacement block");
        helper.assertValueEqual(recipe.getDurability().core(), 0L, "durability core");
        helper.assertValueEqual(recipe.getDurability().edge(), 0L, "durability edge");
        CodecHelper.assertFloat(helper, recipe.getDurability().randomSpread(), 0.0f, "durability spread");
        helper.assertTrue(recipe.initialize(helper.getLevel().registryAccess()),
                "Recipe initialization should succeed when all yields resolve");
        CodecHelper.assertSame(helper, Items.DIAMOND, recipe.getYields().getFirst().items.getFirst().getItem(),
                "resolved yield item");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void initializeFiltersInvalidYieldsButKeepsResolvableTagBackedYields(GameTestHelper helper) {
        var recipe = CodecHelper.assertParses(helper, MiningRecipe.CODEC.codec(),
                CodecHelper.registries(helper), """
                        {
                          "deposit_block": "minecraft:stone",
                          "yields": [
                            {
                              "items": [
                                {
                                  "item_candidates": ["create_rns:definitely_missing_item"],
                                  "weight": 1
                                }
                              ]
                            },
                            {
                              "items": [
                                {
                                  "tag_candidates": ["minecraft:planks"],
                                  "weight": 2
                                }
                              ]
                            }
                          ]
                        }
                        """, "mixed-validity mining recipe");

        helper.assertTrue(recipe.initialize(helper.getLevel().registryAccess()),
                "Recipe initialization should keep any yields that resolve against live game data");
        helper.assertValueEqual(recipe.getYields().size(), 1, "remaining initialized yield count");
        var resolvedItem = recipe.getYields().getFirst().items.getFirst().getItem();
        helper.assertTrue(resolvedItem.builtInRegistryHolder().is(PLANKS_TAG),
                "Tag-backed yields should resolve to an item from the live planks tag");
        helper.assertTrue(resolvedItem != Items.AIR, "Tag-backed yields should resolve to a non-air item");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void rejectsRecipeWithOutOfRangeDurability(GameTestHelper helper) {
        CodecHelper.assertFails(helper, MiningRecipe.CODEC.codec(), CodecHelper.registries(helper), """
                        {
                          "deposit_block": "minecraft:stone",
                          "durability": {
                            "core": 0,
                            "edge": 4,
                            "random_spread": 0.25
                          },
                          "yields": [
                            {
                              "items": [
                                {
                                  "item_candidates": ["minecraft:diamond"],
                                  "weight": 1
                                }
                              ]
                            }
                          ]
                        }
                        """, "Value 0 outside of range [1:9223372036854775807]");
        helper.succeed();
    }
}

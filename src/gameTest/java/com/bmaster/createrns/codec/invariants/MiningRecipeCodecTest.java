package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.util.CodecHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

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
                                  "item": "minecraft:diamond",
                                  "weight": 1
                                }
                              ]
                            }
                          ]
                        }
                        """, "mining recipe");
        var runtimeRecipe = recipe.toRecipe(CreateRNS.asResource("test_recipe"));

        CodecHelper.assertSame(helper, Blocks.STONE, runtimeRecipe.getDepositBlock(), "deposit block");
        CodecHelper.assertSame(helper, Blocks.AIR, runtimeRecipe.getReplacementBlock(), "replacement block");
        CodecHelper.assertValueEqual(helper, runtimeRecipe.getDurability().core(), 0L, "durability core");
        CodecHelper.assertValueEqual(helper, runtimeRecipe.getDurability().edge(), 0L, "durability edge");
        CodecHelper.assertFloat(helper, runtimeRecipe.getDurability().randomSpread(), 0.0f, "durability spread");
        helper.assertTrue(runtimeRecipe.initialize(helper.getLevel().registryAccess()),
                "Recipe initialization should succeed when all yields resolve");
        CodecHelper.assertSame(helper, Items.DIAMOND, runtimeRecipe.getYields().get(0).items.get(0).item,
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
                                  "item": "create_rns:definitely_missing_item",
                                  "compat": true,
                                  "weight": 1
                                }
                              ]
                            },
                            {
                              "items": [
                                {
                                  "item": "#minecraft:planks",
                                  "weight": 2
                                }
                              ]
                            }
                          ]
                        }
                        """, "mixed-validity mining recipe");
        var runtimeRecipe = recipe.toRecipe(CreateRNS.asResource("test_recipe"));

        helper.assertTrue(runtimeRecipe.initialize(helper.getLevel().registryAccess()),
                "Recipe initialization should keep any yields that resolve against live game data");
        CodecHelper.assertValueEqual(helper, runtimeRecipe.getYields().size(), 1, "remaining initialized yield count");
        var resolvedItem = runtimeRecipe.getYields().get(0).items.get(0).item;
        helper.assertTrue(isInTag(helper.getLevel().registryAccess(), resolvedItem, PLANKS_TAG),
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
                          "item": "minecraft:diamond",
                          "weight": 1
                        }
                      ]
                    }
                  ]
                }
                """, "Value 0 outside of range [1:9223372036854775807]");
        helper.succeed();
    }

    private static boolean isInTag(RegistryAccess access, Item item, TagKey<Item> tag) {
        var values = access.lookupOrThrow(Registries.ITEM).get(tag).orElse(null);
        if (values == null) return false;
        return values.stream().anyMatch(holder -> holder.value() == item);
    }
}

package com.bmaster.createrns.unittest.codec;

import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.testutil.CodecAssertions;
import com.bmaster.createrns.testutil.TestRegistryContexts;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningRecipeCodecTest {
    @Test
    void parsesRecipeWithReplacementAndDurabilityDefaults() {
        var recipe = CodecAssertions.assertParses(MiningRecipe.CODEC.codec(), TestRegistryContexts.builtinRegistries(), """
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
                """);

        assertSame(Blocks.STONE, recipe.getDepositBlock());
        assertSame(Blocks.AIR, recipe.getReplacementBlock());
        assertEquals(0, recipe.getDurability().core());
        assertEquals(0, recipe.getDurability().edge());
        assertEquals(0.0f, recipe.getDurability().randomSpread());
        assertTrue(recipe.initialize(TestRegistryContexts.builtinAccess()));
        assertSame(Items.DIAMOND, recipe.getYields().getFirst().items.getFirst().getItem());
    }

    @Test
    void rejectsRecipeWithOutOfRangeDurability() {
        CodecAssertions.assertFails(MiningRecipe.CODEC.codec(), TestRegistryContexts.builtinRegistries(), """
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
    }
}

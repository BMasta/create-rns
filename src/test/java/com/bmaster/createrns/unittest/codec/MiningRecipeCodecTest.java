package com.bmaster.createrns.unittest.codec;

import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.testutil.CodecAssertions;
import com.bmaster.createrns.testutil.TestRegistryContexts;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MiningRecipeCodecTest extends CodecTestBase {
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

        assertSame(Blocks.STONE, recipe.depositBlock());
        assertSame(Blocks.AIR, recipe.replacementBlock());
        assertEquals(0, recipe.dur().core());
        assertEquals(0, recipe.dur().edge());
        assertEquals(0.0f, recipe.dur().randomSpread());
        assertEquals(1, recipe.yields().size());
    }
}

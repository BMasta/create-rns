package com.bmaster.createrns.unittest.codec;

import com.bmaster.createrns.content.deposit.mining.recipe.Yield;
import com.bmaster.createrns.testutil.CodecAssertions;
import com.bmaster.createrns.testutil.TestRegistryContexts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YieldCodecTest extends CodecTestBase {
    @Test
    void weightedItemParsesFromItemCandidate() {
        var weightedItem = CodecAssertions.assertParses(Yield.WeightedItem.CODEC, TestRegistryContexts.json(), """
                {
                  "item_candidates": ["minecraft:diamond"],
                  "weight": 5
                }
                """);

        assertEquals(5, weightedItem.weight);
    }

    @Test
    void weightedItemRejectsMissingCandidates() {
        CodecAssertions.assertFails(Yield.WeightedItem.CODEC, TestRegistryContexts.json(), """
                {
                  "item_candidates": [],
                  "tag_candidates": [],
                  "weight": 1
                }
                """, "Weighted item must define at least an item or a tag");
    }

    @Test
    void yieldParsesWithDefaults() {
        var yield = CodecAssertions.assertParses(Yield.CODEC, TestRegistryContexts.json(), """
                {
                  "items": [
                    {
                      "item_candidates": ["minecraft:diamond"],
                      "weight": 2
                    }
                  ],
                  "catalysts": ["overclock"]
                }
                """);

        assertEquals(1.0f, yield.chance);
        assertEquals(1, yield.items.size());
        Assertions.assertNotNull(yield.crsNames);
        assertEquals(1, yield.crsNames.size());
        assertEquals("overclock", yield.crsNames.get(0));
        assertEquals(0, yield.slotColor);
    }

    @Test
    void yieldRejectsInvalidWeightedItemDefinition() {
        CodecAssertions.assertFails(Yield.CODEC, TestRegistryContexts.json(), """
                {
                  "items": [
                    {
                      "weight": 1
                    }
                  ]
                }
                """, "Weighted item must define at least an item or a tag");
    }
}

package com.bmaster.createrns.unittest.codec;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.Yield;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.FluidCatalystRequirement;
import com.bmaster.createrns.testutil.CodecAssertions;
import com.bmaster.createrns.testutil.LogCapture;
import com.bmaster.createrns.testutil.TestRegistryContexts;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YieldCodecTest {
    @Test
    void weightedItemParsesAndInitializesFromItemCandidate() {
        var weightedItem = CodecAssertions.assertParses(Yield.WeightedItem.CODEC, TestRegistryContexts.json(), """
                {
                  "item_candidates": ["minecraft:diamond"],
                  "weight": 5
                }
                """);

        assertEquals(5, weightedItem.weight);
        assertTrue(weightedItem.initialize(TestRegistryContexts.builtinAccess()));
        assertSame(Items.DIAMOND, weightedItem.getItem());
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
    void weightedItemLogsErrorWhenCandidatesDoNotResolve() {
        var weightedItem = CodecAssertions.assertParses(Yield.WeightedItem.CODEC, TestRegistryContexts.json(), """
                {
                  "item_candidates": ["create_rns:definitely_missing_item"],
                  "weight": 1
                }
                """);

        try (var logs = LogCapture.capture(CreateRNS.LOGGER.getName())) {
            assertFalse(weightedItem.initialize(TestRegistryContexts.builtinAccess()));
            assertTrue(logs.contains("Failed to resolve weighted item"));
            assertTrue(logs.contains("create_rns:definitely_missing_item"));
        }
    }

    @Test
    void yieldParsesWithDefaultsAndInitializesItems() {
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
        assertEquals("overclock", yield.crsNames.getFirst());
        assertEquals(0, yield.slotColor);
        var overclock = new CatalystRequirementSet(
                "overclock",
                1f,
                false,
                0,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(new FluidCatalystRequirement(new FluidStack(Fluids.LAVA, 10)))
        );
        assertTrue(yield.initialize(TestRegistryContexts.catalystRegistryAccess(overclock)));
        assertSame(Items.DIAMOND, yield.items.getFirst().getItem());
    }

    @Test
    void yieldValidatesReferencedCatalystNamesDuringInitialization() {
        var yield = CodecAssertions.assertParses(Yield.CODEC, TestRegistryContexts.json(), """
                {
                  "items": [
                    {
                      "item_candidates": ["minecraft:diamond"],
                      "weight": 1
                    }
                  ],
                  "catalysts": ["known_catalyst"]
                }
                """);

        var knownCatalyst = new CatalystRequirementSet(
                "known_catalyst",
                1f,
                false,
                0,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(new FluidCatalystRequirement(new FluidStack(Fluids.LAVA, 10)))
        );

        assertTrue(yield.initialize(TestRegistryContexts.catalystRegistryAccess(knownCatalyst)));
    }

    @Test
    void yieldLogsErrorAndFailsWhenCatalystNameIsUnknown() {
        var yield = CodecAssertions.assertParses(Yield.CODEC, TestRegistryContexts.json(), """
                {
                  "items": [
                    {
                      "item_candidates": ["minecraft:diamond"],
                      "weight": 1
                    }
                  ],
                  "catalysts": ["missing_catalyst"]
                }
                """);

        var knownCatalyst = new CatalystRequirementSet(
                "known_catalyst",
                1f,
                false,
                0,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(new FluidCatalystRequirement(new FluidStack(Fluids.LAVA, 10)))
        );

        try (var logs = LogCapture.capture(CreateRNS.LOGGER.getName())) {
            assertFalse(yield.initialize(TestRegistryContexts.catalystRegistryAccess(knownCatalyst)));
            assertTrue(logs.contains("unknown catalyst requirement set"));
            assertTrue(logs.contains("missing_catalyst"));
        }
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

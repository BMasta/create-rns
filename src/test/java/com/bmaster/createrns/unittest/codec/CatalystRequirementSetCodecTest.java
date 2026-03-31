package com.bmaster.createrns.unittest.codec;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.FluidCatalystRequirement;
import com.bmaster.createrns.testutil.CodecAssertions;
import com.bmaster.createrns.testutil.TestRegistryContexts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalystRequirementSetCodecTest extends CodecTestBase {
    @Test
    void parsesRequirementSetWithRepresentativeItems() {
        var requirementSet = CodecAssertions.assertParses(CatalystRequirementSet.CODEC, TestRegistryContexts.builtinRegistries(), """
                {
                  "name": "overclock",
                  "chance_multiplier": 2.0,
                  "optional": true,
                  "display_priority": 1004,
                  "hide_if_present": ["resonance"],
                  "requirements": [
                    {
                      "type": "fluid",
                      "consume": {
                        "FluidName": "minecraft:lava",
                        "Amount": 20
                      }
                    }
                  ]
                }
                """);

        assertEquals("overclock", requirementSet.name);
        assertEquals(2.0f, requirementSet.chanceMult);
        assertTrue(requirementSet.optional);
        assertEquals(1004, requirementSet.displayPriority);
        assertEquals(1, requirementSet.hideIfPresent.size());
        assertEquals("resonance", requirementSet.hideIfPresent.get(0));
        assertEquals(1, requirementSet.requirements.size());
        assertInstanceOf(FluidCatalystRequirement.class, requirementSet.requirements.get(0));
    }

    @Test
    void rejectsRequirementSetWithoutRequirements() {
        CodecAssertions.assertFails(CatalystRequirementSet.CODEC, TestRegistryContexts.builtinRegistries(), """
                {
                  "name": "invalid",
                  "requirements": []
                }
                """, "Catalyst must have at least one requirement");
    }
}

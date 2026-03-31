package com.bmaster.createrns.unittest.codec;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.AttachmentCatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.FluidCatalystRequirement;
import com.bmaster.createrns.testutil.CodecAssertions;
import com.bmaster.createrns.testutil.TestRegistryContexts;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class CatalystRequirementCodecTest {
    @Test
    void attachmentRequirementParsesWithBuiltinRegistryContext() {
        var requirement = CodecAssertions.assertParses(CatalystRequirement.CODEC, TestRegistryContexts.builtinRegistries(), """
                {
                  "type": "attachment",
                  "attachment": ["minecraft:stone", "minecraft:dirt"],
                  "count": 3
                }
                """);

        var attachmentRequirement = assertInstanceOf(AttachmentCatalystRequirement.class, requirement);
        assertEquals(3, attachmentRequirement.count);
    }

    @Test
    void unknownRequirementTypeReportsStableError() {
        CodecAssertions.assertFails(CatalystRequirement.CODEC, TestRegistryContexts.json(), """
                {
                  "type": "definitely_missing"
                }
                """, "Unknown catalyst requirement type: definitely_missing");
    }

    @Test
    void fluidRequirementParsesWithFluidStackPayload() {
        var requirement = CodecAssertions.assertParses(CatalystRequirement.CODEC, TestRegistryContexts.builtinRegistries(), """
                {
                  "type": "fluid",
                  "consume": {
                    "id": "minecraft:lava",
                    "amount": 20
                  }
                }
                """);

        var fluidRequirement = assertInstanceOf(FluidCatalystRequirement.class, requirement);
        assertSame(Fluids.LAVA, fluidRequirement.fluidStack.getFluid());
        assertEquals(20, fluidRequirement.fluidStack.getAmount());
    }
}

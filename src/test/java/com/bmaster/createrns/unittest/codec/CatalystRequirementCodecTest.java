package com.bmaster.createrns.unittest.codec;

import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.AttachmentCatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.FluidCatalystRequirement;
import com.bmaster.createrns.testutil.CodecAssertions;
import com.bmaster.createrns.testutil.TestRegistryContexts;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

class CatalystRequirementCodecTest extends CodecTestBase {
    @Test
    void attachmentRequirementParsesSingleBlock() {
        var requirement = CodecAssertions.assertParses(CatalystRequirement.CODEC, TestRegistryContexts.builtinRegistries(), """
                {
                  "type": "attachment",
                  "attachment": "minecraft:stone",
                  "count": 3
                }
                """);

        var attachmentRequirement = assertInstanceOf(AttachmentCatalystRequirement.class, requirement);
        assertEquals(3, attachmentRequirement.count);
        assertEquals(1, attachmentRequirement.attachment.size());
        assertSame(Blocks.STONE, attachmentRequirement.attachment.get(0).value());
    }

    @Test
    void attachmentRequirementParsesBlockList() {
        var requirement = CodecAssertions.assertParses(CatalystRequirement.CODEC, TestRegistryContexts.builtinRegistries(), """
                {
                  "type": "attachment",
                  "attachment": ["minecraft:stone", "minecraft:dirt"],
                  "count": 3
                }
                """);

        var attachmentRequirement = assertInstanceOf(AttachmentCatalystRequirement.class, requirement);
        assertEquals(3, attachmentRequirement.count);
        assertEquals(2, attachmentRequirement.attachment.size());
        assertSame(Blocks.STONE, attachmentRequirement.attachment.get(0).value());
        assertSame(Blocks.DIRT, attachmentRequirement.attachment.get(1).value());
    }

    @Test
    void attachmentRequirementParsesBlockTag() {
        var requirement = CodecAssertions.assertParses(CatalystRequirement.CODEC, TestRegistryContexts.builtinRegistries(), """
                {
                  "type": "attachment",
                  "attachment": "#minecraft:planks",
                  "count": 2
                }
                """);

        var attachmentRequirement = assertInstanceOf(AttachmentCatalystRequirement.class, requirement);
        assertEquals(2, attachmentRequirement.count);
        assertTrue(attachmentRequirement.attachment.unwrapKey().isPresent());
        assertEquals(BlockTags.PLANKS.location(), attachmentRequirement.attachment.unwrapKey().orElseThrow().location());
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
                    "FluidName": "minecraft:lava",
                    "Amount": 20
                  }
                }
                """);

        var fluidRequirement = assertInstanceOf(FluidCatalystRequirement.class, requirement);
        assertSame(Fluids.LAVA, fluidRequirement.fluidStack.getFluid());
        assertEquals(20, fluidRequirement.fluidStack.getAmount());
    }
}

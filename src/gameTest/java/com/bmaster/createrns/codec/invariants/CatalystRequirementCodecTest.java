package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.*;
import com.bmaster.createrns.util.CodecHelper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class CatalystRequirementCodecTest {
    @GameTest(template = "empty16x16")
    public void attachmentRequirementParsesSingleBlock(GameTestHelper helper) {
        var requirement = CodecHelper.assertParses(helper, CatalystRequirement.CODEC,
                CodecHelper.registries(helper), """
                        {
                          "type": "attachment",
                          "attachment": "minecraft:stone",
                          "count": 3
                        }
                        """, "single-block attachment requirement");

        var attachmentRequirement = CodecHelper.assertInstanceOf(helper, AttachmentCatalystRequirement.class,
                requirement, "parsed catalyst requirement");
        CodecHelper.assertValueEqual(helper, attachmentRequirement.count, 3, "attachment count");
        CodecHelper.assertValueEqual(helper, attachmentRequirement.attachment.size(), 1, "attachment block count");
        CodecHelper.assertSame(helper, Blocks.STONE, attachmentRequirement.attachment.get(0).value(),
                "single attachment block");
        helper.assertTrue(attachmentRequirement.isSatisfiedBy(List.of(new AttachmentCatalyst(Blocks.STONE, 3))),
                "Stone attachments with the required count should satisfy the requirement");
        helper.assertFalse(attachmentRequirement.isSatisfiedBy(List.of(new AttachmentCatalyst(Blocks.STONE, 2))),
                "Too few matching attachments should not satisfy the requirement");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void attachmentRequirementParsesBlockList(GameTestHelper helper) {
        var requirement = CodecHelper.assertParses(helper, CatalystRequirement.CODEC,
                CodecHelper.registries(helper), """
                        {
                          "type": "attachment",
                          "attachment": ["minecraft:stone", "minecraft:dirt"],
                          "count": 3
                        }
                        """, "multi-block attachment requirement");

        var attachmentRequirement = CodecHelper.assertInstanceOf(helper, AttachmentCatalystRequirement.class,
                requirement, "parsed catalyst requirement");
        CodecHelper.assertValueEqual(helper, attachmentRequirement.count, 3, "attachment count");
        CodecHelper.assertValueEqual(helper, attachmentRequirement.attachment.size(), 2, "attachment block count");
        CodecHelper.assertSame(helper, Blocks.STONE, attachmentRequirement.attachment.get(0).value(),
                "first attachment block");
        CodecHelper.assertSame(helper, Blocks.DIRT, attachmentRequirement.attachment.get(1).value(),
                "second attachment block");
        helper.assertTrue(attachmentRequirement.isSatisfiedBy(List.of(new AttachmentCatalyst(Blocks.DIRT, 3))),
                "Any listed attachment block should satisfy the requirement");
        helper.assertFalse(attachmentRequirement.isSatisfiedBy(List.of(new AttachmentCatalyst(Blocks.GRASS_BLOCK, 3))),
                "Blocks outside the attachment list should not satisfy the requirement");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void attachmentRequirementParsesBlockTag(GameTestHelper helper) {
        var requirement = CodecHelper.assertParses(helper, CatalystRequirement.CODEC,
                CodecHelper.registries(helper), """
                        {
                          "type": "attachment",
                          "attachment": "#minecraft:planks",
                          "count": 2
                        }
                        """, "block-tag attachment requirement");

        var attachmentRequirement = CodecHelper.assertInstanceOf(helper, AttachmentCatalystRequirement.class,
                requirement, "parsed catalyst requirement");
        CodecHelper.assertValueEqual(helper, attachmentRequirement.count, 2, "attachment count");
        var tagKey = attachmentRequirement.attachment.unwrapKey().orElse(null);
        helper.assertTrue(tagKey != null, "Tag-backed attachment requirement should retain its block tag key");
        CodecHelper.assertValueEqual(helper, tagKey.location(), BlockTags.PLANKS.location(), "attachment tag key");
        helper.assertTrue(Blocks.OAK_PLANKS.defaultBlockState().is(attachmentRequirement.attachment),
                "Oak planks should resolve through the live planks block tag");
        helper.assertTrue(Blocks.BIRCH_PLANKS.defaultBlockState().is(attachmentRequirement.attachment),
                "Birch planks should resolve through the live planks block tag");
        helper.assertFalse(Blocks.STONE.defaultBlockState().is(attachmentRequirement.attachment),
                "Stone should not resolve through the planks block tag");
        helper.assertTrue(attachmentRequirement.isSatisfiedBy(List.of(new AttachmentCatalyst(Blocks.OAK_PLANKS, 2))),
                "Tag-backed attachment requirements should accept matching tagged blocks");
        helper.assertFalse(attachmentRequirement.isSatisfiedBy(List.of(new AttachmentCatalyst(Blocks.STONE, 2))),
                "Tag-backed attachment requirements should reject non-tagged blocks");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void unknownRequirementTypeReportsStableError(GameTestHelper helper) {
        CodecHelper.assertFails(helper, CatalystRequirement.CODEC, CodecHelper.json(), """
                        {
                          "type": "definitely_missing"
                        }
                        """, "Unknown catalyst requirement type: definitely_missing");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void fluidRequirementParsesWithFluidStackPayload(GameTestHelper helper) {
        var requirement = CodecHelper.assertParses(helper, CatalystRequirement.CODEC,
                CodecHelper.registries(helper), """
                        {
                          "type": "fluid",
                          "consume": {
                            "FluidName": "minecraft:lava",
                            "Amount": 20
                          }
                        }
                        """, "fluid catalyst requirement");

        var fluidRequirement = CodecHelper.assertInstanceOf(helper, FluidCatalystRequirement.class, requirement,
                "parsed catalyst requirement");
        CodecHelper.assertSame(helper, Fluids.LAVA, fluidRequirement.fluidStack.getFluid(),
                "required catalyst fluid");
        CodecHelper.assertValueEqual(helper, fluidRequirement.fluidStack.getAmount(), 20, "required fluid amount");

        var tank = new FluidTank(1000);
        tank.fill(new FluidStack(Fluids.LAVA, 40), IFluidHandler.FluidAction.EXECUTE);
        var catalyst = new FluidCatalyst(tank);

        helper.assertTrue(fluidRequirement.isSatisfiedBy(List.of(catalyst)),
                "A fluid catalyst should satisfy the requirement type check");
        helper.assertTrue(fluidRequirement.useCatalysts(List.of(catalyst), true),
                "Simulated catalyst usage should succeed when enough fluid is present");
        CodecHelper.assertValueEqual(helper, tank.getFluidAmount(), 40, "fluid amount after simulated catalyst usage");
        helper.assertTrue(fluidRequirement.useCatalysts(List.of(catalyst), false),
                "Executing catalyst usage should succeed when enough fluid is present");
        CodecHelper.assertValueEqual(helper, tank.getFluidAmount(), 20, "fluid amount after executing catalyst usage");

        var insufficientTank = new FluidTank(1000);
        insufficientTank.fill(new FluidStack(Fluids.LAVA, 10), IFluidHandler.FluidAction.EXECUTE);
        helper.assertFalse(fluidRequirement.useCatalysts(List.of(new FluidCatalyst(insufficientTank)), false),
                "Catalyst usage should fail when the tank contains too little fluid");
        helper.succeed();
    }
}

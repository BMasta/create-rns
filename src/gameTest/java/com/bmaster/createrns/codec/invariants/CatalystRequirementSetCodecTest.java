package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.AttachmentCatalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.AttachmentCatalystRequirement;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.FluidCatalystRequirement;
import com.bmaster.createrns.util.CodecHelper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class CatalystRequirementSetCodecTest {
    @GameTest(template = "empty16x16")
    public void parsesRequirementSetWithRepresentativeItems(GameTestHelper helper) {
        var requirementSet = CodecHelper.assertParses(helper, CatalystRequirementSet.CODEC,
                CodecHelper.registries(helper), """
                        {
                          "name": "overclock",
                          "chance_multiplier": 2.0,
                          "optional": true,
                          "display_priority": 1004,
                          "representative_items": ["minecraft:lava_bucket"],
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
                        """, "catalyst requirement set");

        CodecHelper.assertValueEqual(helper, requirementSet.name, "overclock", "catalyst requirement set name");
        CodecHelper.assertFloat(helper, requirementSet.chanceMult, 2.0f, "chance multiplier");
        helper.assertTrue(requirementSet.optional, "Requirement set should preserve the optional flag");
        CodecHelper.assertValueEqual(helper, requirementSet.displayPriority, 1004, "display priority");
        CodecHelper.assertValueEqual(helper, requirementSet.representativeItems.size(), 1, "representative item count");
        CodecHelper.assertSame(helper, Items.LAVA_BUCKET, requirementSet.representativeItems.get(0),
                "representative item");
        CodecHelper.assertValueEqual(helper, requirementSet.hideIfPresent.size(), 1, "hidden catalyst count");
        CodecHelper.assertValueEqual(helper, requirementSet.hideIfPresent.get(0), "resonance", "hidden catalyst name");
        CodecHelper.assertValueEqual(helper, requirementSet.requirements.size(), 1, "requirement count");

        var fluidRequirement = CodecHelper.assertInstanceOf(helper, FluidCatalystRequirement.class,
                requirementSet.requirements.get(0), "parsed catalyst requirement");
        CodecHelper.assertSame(helper, Fluids.LAVA, fluidRequirement.fluidStack.getFluid(),
                "representative fluid requirement fluid");
        CodecHelper.assertValueEqual(helper, fluidRequirement.fluidStack.getAmount(), 20,
                "representative fluid requirement amount");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void networkCodecFlattensAttachmentTags(GameTestHelper helper) {
        var requirementSet = CodecHelper.assertParses(helper, CatalystRequirementSet.CODEC,
                CodecHelper.registries(helper), """
                        {
                          "name": "resonance",
                          "requirements": [
                            {
                              "type": "attachment",
                              "attachment": "#minecraft:planks",
                              "count": 2
                            }
                          ]
                        }
                        """, "tag-backed catalyst requirement set");

        var encodedResult = CatalystRequirementSet.STREAM_CODEC.encodeStart(CodecHelper.registries(helper), requirementSet);
        var error = encodedResult.error().orElse(null);
        helper.assertTrue(error == null,
                "Expected catalyst requirement set network encode success, got: "
                        + ((error != null) ? error.message() : "unknown codec error"));
        var encoded = encodedResult.result().orElse(null);
        helper.assertTrue(encoded != null, "Expected catalyst requirement set network encode result");

        var attachment = encoded.getAsJsonObject()
                .getAsJsonArray("requirements").get(0).getAsJsonObject()
                .get("attachment");
        helper.assertTrue(attachment.isJsonArray(),
                "Network-encoded attachment requirement should flatten tags into direct block ids");

        var restoredResult = CatalystRequirementSet.STREAM_CODEC.parse(CodecHelper.registries(helper), encoded);
        var restoredError = restoredResult.error().orElse(null);
        helper.assertTrue(restoredError == null,
                "Expected catalyst requirement set network decode success, got: "
                        + ((restoredError != null) ? restoredError.message() : "unknown codec error"));
        var restored = restoredResult.result().orElse(null);
        helper.assertTrue(restored != null, "Expected restored catalyst requirement set");
        CodecHelper.assertValueEqual(helper, restored.requirements.size(), 1, "restored requirement count");

        var attachmentRequirement = CodecHelper.assertInstanceOf(helper, AttachmentCatalystRequirement.class,
                restored.requirements.get(0), "restored network catalyst requirement");
        helper.assertTrue(attachmentRequirement.attachment.unwrapKey().isEmpty(),
                "Network-decoded attachment requirement should not require a live tag binding");
        helper.assertTrue(attachmentRequirement.isSatisfiedBy(List.of(new AttachmentCatalyst(Blocks.OAK_PLANKS, 2))),
                "Flattened network attachment requirement should still match tag members");
        helper.assertFalse(attachmentRequirement.isSatisfiedBy(List.of(new AttachmentCatalyst(Blocks.STONE, 2))),
                "Flattened network attachment requirement should still reject non-members");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void rejectsRequirementSetWithoutRequirements(GameTestHelper helper) {
        CodecHelper.assertFails(helper, CatalystRequirementSet.CODEC, CodecHelper.registries(helper),
                """
                        {
                          "name": "invalid",
                          "requirements": []
                        }
                        """, "Catalyst must have at least one requirement");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void builtInOverclockRegistryEntryMatchesExpectedFields(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(CatalystRequirementSet.REGISTRY_KEY);
        var overclock = registry.get(CreateRNS.asResource("overclock"));

        helper.assertTrue(overclock != null, "Game bootstrap should load the built-in overclock catalyst entry");
        CodecHelper.assertValueEqual(helper, overclock.name, "overclock", "built-in catalyst name");
        CodecHelper.assertFloat(helper, overclock.chanceMult, 2.0f, "built-in overclock chance multiplier");
        helper.assertTrue(overclock.optional, "Built-in overclock should be optional");
        CodecHelper.assertValueEqual(helper, overclock.displayPriority, 1004, "built-in overclock display priority");
        CodecHelper.assertValueEqual(helper, overclock.representativeItems.size(), 1, "built-in representative item count");
        CodecHelper.assertSame(helper, Items.LAVA_BUCKET, overclock.representativeItems.get(0),
                "built-in overclock representative item");
        helper.assertTrue(overclock.hideIfPresent.isEmpty(),
                "Built-in overclock should not hide itself when other catalysts are present");
        CodecHelper.assertValueEqual(helper, overclock.requirements.size(), 1, "built-in requirement count");

        var fluidRequirement = CodecHelper.assertInstanceOf(helper, FluidCatalystRequirement.class,
                overclock.requirements.get(0), "built-in overclock requirement");
        CodecHelper.assertSame(helper, Fluids.LAVA, fluidRequirement.fluidStack.getFluid(),
                "built-in overclock fluid");
        CodecHelper.assertValueEqual(helper, fluidRequirement.fluidStack.getAmount(), 20, "built-in overclock fluid amount");
        helper.succeed();
    }
}

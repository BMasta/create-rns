package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.FluidCatalystRequirement;
import com.bmaster.createrns.util.CodecHelper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
                                "id": "minecraft:lava",
                                "amount": 20
                              }
                            }
                          ]
                        }
                        """, "catalyst requirement set");

        helper.assertValueEqual(requirementSet.name, "overclock", "catalyst requirement set name");
        CodecHelper.assertFloat(helper, requirementSet.chanceMult, 2.0f, "chance multiplier");
        helper.assertTrue(requirementSet.optional, "Requirement set should preserve the optional flag");
        helper.assertValueEqual(requirementSet.displayPriority, 1004, "display priority");
        helper.assertValueEqual(requirementSet.representativeItems.size(), 1, "representative item count");
        CodecHelper.assertSame(helper, Items.LAVA_BUCKET, requirementSet.representativeItems.getFirst(),
                "representative item");
        helper.assertValueEqual(requirementSet.hideIfPresent.size(), 1, "hidden catalyst count");
        helper.assertValueEqual(requirementSet.hideIfPresent.getFirst(), "resonance", "hidden catalyst name");
        helper.assertValueEqual(requirementSet.requirements.size(), 1, "requirement count");

        var fluidRequirement = CodecHelper.assertInstanceOf(helper, FluidCatalystRequirement.class,
                requirementSet.requirements.getFirst(), "parsed catalyst requirement");
        CodecHelper.assertSame(helper, Fluids.LAVA, fluidRequirement.fluidStack.getFluid(),
                "representative fluid requirement fluid");
        helper.assertValueEqual(fluidRequirement.fluidStack.getAmount(), 20,
                "representative fluid requirement amount");
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
        helper.assertValueEqual(overclock.name, "overclock", "built-in catalyst name");
        CodecHelper.assertFloat(helper, overclock.chanceMult, 2.0f, "built-in overclock chance multiplier");
        helper.assertTrue(overclock.optional, "Built-in overclock should be optional");
        helper.assertValueEqual(overclock.displayPriority, 1004, "built-in overclock display priority");
        helper.assertValueEqual(overclock.representativeItems.size(), 1, "built-in representative item count");
        CodecHelper.assertSame(helper, Items.LAVA_BUCKET, overclock.representativeItems.getFirst(),
                "built-in overclock representative item");
        helper.assertTrue(overclock.hideIfPresent.isEmpty(),
                "Built-in overclock should not hide itself when other catalysts are present");
        helper.assertValueEqual(overclock.requirements.size(), 1, "built-in requirement count");

        var fluidRequirement = CodecHelper.assertInstanceOf(helper, FluidCatalystRequirement.class,
                overclock.requirements.getFirst(), "built-in overclock requirement");
        CodecHelper.assertSame(helper, Fluids.LAVA, fluidRequirement.fluidStack.getFluid(),
                "built-in overclock fluid");
        helper.assertValueEqual(fluidRequirement.fluidStack.getAmount(), 20, "built-in overclock fluid amount");
        helper.succeed();
    }
}

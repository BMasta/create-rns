package com.bmaster.createrns.codec.invariants;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystHandler;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystReservations;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.FluidCatalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.FluidCatalystRequirement;
import com.bmaster.createrns.util.CodecHelper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class CatalystRequirementSetCodecTest {
    @GameTest(template = "empty16x16")
    public void parsesRequirementSetWithRepresentativeItems(GameTestHelper helper) {
        var requirementSet = CodecHelper.assertParses(helper, CatalystRequirementSet.CODEC,
                CodecHelper.registries(helper), """
                        {
                          "chance_multiplier": 2.0,
                          "count_multiplier": 1.5,
                          "optional": true,
                          "display_priority": 1004,
                          "representative_items": ["minecraft:lava_bucket"],
                          "hide_if_present": ["create_rns:resonance"],
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

        CodecHelper.assertFloat(helper, requirementSet.chanceMult, 2.0f, "chance multiplier");
        helper.assertValueEqual(requirementSet.countMult, 1.5, "count multiplier");
        helper.assertTrue(requirementSet.optional, "Requirement set should preserve the optional flag");
        helper.assertValueEqual(requirementSet.displayPriority, 1004, "display priority");
        helper.assertValueEqual(requirementSet.representativeItems.size(), 1, "representative item count");
        CodecHelper.assertSame(helper, Items.LAVA_BUCKET, requirementSet.representativeItems.getFirst(),
                "representative item");
        helper.assertValueEqual(requirementSet.hideIfPresent.size(), 1, "hidden catalyst count");
        helper.assertValueEqual(CatalystRequirementSet.id(requirementSet.hideIfPresent.getFirst()),
                CreateRNS.asResource("resonance"),
                "hidden catalyst id");
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
    public void countMultipliersApplyOnlyToMatchingActiveYields(GameTestHelper helper) {
        var recipe = CodecHelper.assertParses(helper, MiningRecipe.CODEC.codec(), CodecHelper.registries(helper), """
                {
                  "deposit_block": "minecraft:stone",
                  "yields": [
                    {"items": [{"item": "diamond", "count": 3}], "catalysts": ["create_rns_test:water_count", "create_rns_test:lava_count"]},
                    {"items": [{"item": "diamond", "count": 50}], "catalysts": ["create_rns_test:water_count"]},
                    {"items": [{"item": "diamond"}], "catalysts": ["create_rns_test:water_count"]},
                    {"items": [{"item": "diamond", "count": 50}], "catalysts": ["create_rns_test:lava_count"]},
                    {"items": [{"item": "diamond", "count": 3}]}
                  ]
                }
                """, "recipe with count multipliers");
        recipe.initialize(helper.getLevel().registryAccess());
        var water = new FluidTank(1);
        var lava = new FluidTank(1);
        var handler = new CatalystHandler(recipe, Set.of(new FluidCatalyst(water), new FluidCatalyst(lava)));
        var combined = recipe.getYields().getFirst();
        var random = helper.getLevel().random;

        water.fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE);
        var preview = handler.reserve(new CatalystReservations());
        helper.assertValueEqual(water.getFluidAmount(), 1, "Preview leaves water available");
        helper.assertValueEqual(handler.getCurrentActiveCRSes(), Set.of(ResourceLocation.parse("create_rns_test:water_count")),
                "Only the available optional catalyst should be active");
        helper.assertValueEqual(combined.roll(random, preview.get(0)).getCount(), 1,
                "An inactive optional catalyst contributes no multiplier");

        lava.fill(new FluidStack(Fluids.LAVA, 1), IFluidHandler.FluidAction.EXECUTE);
        var reservations = new CatalystReservations();
        handler.reserve(reservations);
        helper.assertTrue(handler.consume(reservations), "Shared catalysts should be consumed once per operation");
        helper.assertValueEqual(water.getFluidAmount(), 0, "Consumed water");
        helper.assertValueEqual(lava.getFluidAmount(), 0, "Consumed lava");
        var restored = new CatalystHandler(recipe, Set.of());
        var provider = helper.getLevel().registryAccess();
        restored.deserializeNBT(provider, handler.serializeNBT(provider));
        helper.assertValueEqual(restored.getCurrentCatalystEffects(1).countMult(), 0.58d, "saved count multiplier precision");
        // Round once, preserve exact decimal boundaries, allow zero, cap at 99 and leave unrelated yields alone.
        var expected = List.of(5, 29, 0, 99, 3);
        for (int i = 0; i < expected.size(); i++) {
            var yield = recipe.getYields().get(i);
            helper.assertValueEqual(yield.roll(random, restored.getCurrentCatalystEffects(i)).getCount(),
                    expected.get(i), "rolled count for yield " + i);
        }
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void rejectsRequirementSetWithoutRequirements(GameTestHelper helper) {
        CodecHelper.assertFails(helper, CatalystRequirementSet.CODEC, CodecHelper.registries(helper),
                """
                        {
                          "requirements": []
                        }
                        """, "Catalyst must have at least one requirement");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void rejectsMissingHideIfPresentCatalyst(GameTestHelper helper) {
        CodecHelper.assertFails(helper, CatalystRequirementSet.CODEC, CodecHelper.registries(helper),
                """
                        {
                          "hide_if_present": ["create_rns:missing"],
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
                        """, "Failed to get element create_rns:missing");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void builtInOverclockRegistryEntryMatchesExpectedFields(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(CatalystRequirementSet.REGISTRY_KEY);
        var overclock = registry.get(CreateRNS.asResource("overclock"));

        helper.assertTrue(overclock != null, "Game bootstrap should load the built-in overclock catalyst entry");
        CodecHelper.assertFloat(helper, overclock.chanceMult, 2.0f, "built-in overclock chance multiplier");
        helper.assertValueEqual(overclock.countMult, 1d, "default count multiplier");
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

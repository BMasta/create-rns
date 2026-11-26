package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipe;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystHandler;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystHandler.CatalystEffects;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystReservations;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.FluidCatalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.FluidCatalystRequirement;
import com.bmaster.createrns.util.CodecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class CatalystReservationGameTest {
    @GameTest(template = "empty16x16")
    public void emptyRollLeavesReservedFluidInTank(GameTestHelper helper) {
        var recipe = CodecHelper.assertParses(helper, MiningRecipe.CODEC.codec(), CodecHelper.registries(helper), """
                {
                  "deposit_block": "create_rns:iron_deposit_block",
                  "yields": [{"items": [{"item": "diamond"}], "catalysts": ["create_rns_test:water_count"]}]
                }
                """, "one item scaled below one by the active water catalyst");
        var level = helper.getLevel();
        helper.assertTrue(recipe.initialize(level.registryAccess()), "Diamond yield resolves");
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, RNSDeposits.IRON_DEPOSIT.get());
        var tank = new FluidTank(1);
        tank.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE);
        var process = new InnerProcess(level, List.of(helper.absolutePos(pos)), recipe, 1, Set.of(new FluidCatalyst(tank)));
        process.advance(1);

        helper.assertTrue(process.collect(new MiningStats()).isEmpty(), "A scaled count of zero produces no items");
        helper.assertValueEqual(process.catalystHandler.getCurrentCatalystEffects(0).countMult(), 0.58d,
                "The water catalyst applied to the roll");
        helper.assertValueEqual(tank.getFluidAmount(), 1, "An empty mining operation leaves its reserved water available");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void reservationsDistinguishFluidComponentsAndCombineMatchingTanks(GameTestHelper helper) {
        var plain = new FluidStack(Fluids.WATER, 1);
        var named = plain.copy();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Test water"));
        var first = new FluidTank(1);
        var second = new FluidTank(1);
        var third = new FluidTank(1);
        first.fill(named.copy(), FluidAction.EXECUTE);
        second.fill(plain.copy(), FluidAction.EXECUTE);
        third.fill(named.copy(), FluidAction.EXECUTE);
        var catalysts = List.<Catalyst>of(new FluidCatalyst(first), new FluidCatalyst(second), new FluidCatalyst(third));

        var reservations = new CatalystReservations();
        var namedRequirement = new FluidCatalystRequirement(named.copyWithAmount(2));
        helper.assertTrue(namedRequirement.reserve(catalysts, reservations), "Matching water spans two tanks");
        helper.assertFalse(new FluidCatalystRequirement(named).reserve(catalysts, reservations),
                "Plain water cannot pay for another unit of named water");
        helper.assertTrue(new FluidCatalystRequirement(plain).reserve(catalysts, reservations),
                "Plain water has its own reservation");
        helper.assertValueEqual(first.getFluidAmount() + second.getFluidAmount() + third.getFluidAmount(), 3,
                "Planning leaves all three units in their tanks");
        helper.assertTrue(reservations.consume(catalysts), "Both fluid variants can be consumed together");
        helper.assertTrue(first.isEmpty() && second.isEmpty() && third.isEmpty(), "Every reserved unit was drained");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void earlierOptionalCatalystTakesPriorityAndSharedIdsArePaidOnce(GameTestHelper helper) {
        var recipe = CodecHelper.assertParses(helper, MiningRecipe.CODEC.codec(), CodecHelper.registries(helper), """
                {
                  "deposit_block": "minecraft:stone",
                  "yields": [
                    {"items": [{"item": "diamond"}], "catalysts": ["create_rns_test:water_count", "create_rns_test:water_count"]},
                    {"items": [{"item": "iron_ingot"}], "catalysts": ["create_rns_test:required_water"]},
                    {"items": [{"item": "emerald"}], "catalysts": ["create_rns_test:water_count"]}
                  ]
                }
                """, "yields competing for the last water");
        var tank = new FluidTank(1);
        tank.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE);
        var handler = new CatalystHandler(recipe, Set.of(new FluidCatalyst(tank)));
        var reservations = new CatalystReservations();
        var effects = handler.reserve(reservations);
        helper.assertValueEqual(effects.get(0).countMult(), 0.58d, "Duplicate ID applies the multiplier once");
        helper.assertValueEqual(effects.get(1), CatalystEffects.DISABLE, "Later required water is unavailable");
        helper.assertValueEqual(effects.get(2), effects.get(0), "Later yield reuses the active catalyst");
        helper.assertValueEqual(handler.getCurrentActiveCRSes(), Set.of(ResourceLocation.parse("create_rns_test:water_count")),
                "Only the earlier optional catalyst is active");
        helper.assertValueEqual(tank.getFluidAmount(), 1, "Planning preserves the last unit");
        helper.assertTrue(handler.consume(reservations), "One unit pays for the shared catalyst");
        helper.assertTrue(tank.isEmpty(), "Exactly the reserved water was consumed");

        var next = handler.reserve(new CatalystReservations());
        helper.assertValueEqual(next.get(0), new CatalystEffects(1f, 1d), "Optional yield still works with an empty tank");
        helper.assertTrue(handler.getCurrentActiveCRSes().isEmpty(), "New operation clears earlier active catalysts");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void failedYieldReleasesPartialFluidReservations(GameTestHelper helper) {
        var recipe = CodecHelper.assertParses(helper, MiningRecipe.CODEC.codec(), CodecHelper.registries(helper), """
                {
                  "deposit_block": "minecraft:stone",
                  "yields": [
                    {"items": [{"item": "diamond"}], "catalysts": ["create_rns_test:required_water", "create_rns_test:required_water_and_lava"]},
                    {"items": [{"item": "emerald"}], "catalysts": ["create_rns_test:required_water", "create_rns_test:water_count"]}
                  ]
                }
                """, "unavailable lava after reserving water");
        var tank = new FluidTank(2);
        tank.fill(new FluidStack(Fluids.WATER, 2), FluidAction.EXECUTE);
        var handler = new CatalystHandler(recipe, Set.of(new FluidCatalyst(tank)));
        var reservations = new CatalystReservations();
        var effects = handler.reserve(reservations);
        helper.assertValueEqual(effects.get(0), CatalystEffects.DISABLE, "Missing required lava disables the first yield");
        helper.assertValueEqual(effects.get(1), new CatalystEffects(1f, 0.58d),
                "Both units of water remain available for the second yield");
        helper.assertValueEqual(tank.getFluidAmount(), 2, "Failed and successful reservations leave the tank intact");
        helper.assertTrue(handler.consume(reservations), "Distinct catalysts can spend the exact combined balance");
        helper.assertTrue(tank.isEmpty(), "Two distinct water reservations are both paid");
        helper.succeed();
    }
}

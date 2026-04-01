package com.bmaster.createrns.comprehensive;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator.ShatteringResonatorBlock;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystUsageStats;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.util.MinerSetup;
import com.bmaster.createrns.util.MinerSetupBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.stream.Collectors;

@SuppressWarnings("DataFlowIssue")
@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MinerMiningBehaviourGameTest {
    @GameTest(template = "empty16x16", batch = "1", timeoutTicks = 140)
    public void minerMinesAndDropsItem(GameTestHelper h) {
        int miningSpeed = 90;
        int rpm = 256;
        int miningRadius = 1;
        int expectedClaimedBlockCount = (int) Math.pow(miningRadius * 2 + 1, 2);

        setServerConfig(miningSpeed, miningRadius, 256);

        var miner = miner(h, 3, 5, 3)
                .deposit(0, 2, 0, 7, 2, 7)
                .place();
        miner.assemble(rpm);

        int ticksToMine = miner.ticksToMine(expectedClaimedBlockCount).orElse(-1);
        h.assertTrue(ticksToMine > 0, "Failed to compute # of ticks to mine an item");
        h.assertTrue(ticksToMine + 1 < 140, "Miner requires more time than the test allows");

        // Assembly takes one tick
        h.runAtTickTime(1, () -> commonPostAssemblyChecks(h, miner, miningRadius, rpm));

        h.runAtTickTime(ticksToMine, () -> {
            var nCobbleOnGround = countGroundItems(h, miner.headPos(), Items.COBBLESTONE);
            h.assertTrue(nCobbleOnGround == 0,
                    "Expected to find no cobblestone on the ground yet, but found " + nCobbleOnGround);
        });

        h.runAtTickTime(ticksToMine + 1, () -> {
            var nCobbleOnGround = countGroundItems(h, miner.headPos(), Items.COBBLESTONE);
            h.assertTrue(nCobbleOnGround == 1,
                    "Expected to find 1 cobblestone on the ground, but found " + nCobbleOnGround);
            h.succeed();
        });
    }

    @GameTest(template = "empty16x16", batch = "2", timeoutTicks = 140)
    public void minerMinesAndCollectsItem(GameTestHelper h) {
        int miningSpeed = 60;
        int rpm = 128;
        int miningRadius = 3;
        int expectedClaimedBlockCount = (int) Math.pow(miningRadius * 2 + 1, 2);

        setServerConfig(miningSpeed, miningRadius, 256);

        var miner = minerWithStorage(h, 3, 5, 3)
                .deposit(0, 2, 0, 7, 2, 7)
                .place();
        miner.assemble(rpm);

        int ticksToMine = miner.ticksToMine(expectedClaimedBlockCount).orElse(-1);
        h.assertTrue(ticksToMine > 0, "Failed to compute # of ticks to mine an item");
        h.assertTrue(ticksToMine + 1 < 140, "Miner requires more time than the test allows");

        h.runAtTickTime(1, () -> commonPostAssemblyChecks(h, miner, miningRadius, rpm));

        h.runAtTickTime(ticksToMine, () -> {
            h.assertTrue(miner.findInStorage(ItemStack.EMPTY, true), "Miner storage is expected to be empty");
        });

        h.runAtTickTime(ticksToMine + 1, () -> {
            var nCobbleOnGround = countGroundItems(h, miner.headPos(), Items.COBBLESTONE);
            h.assertTrue(nCobbleOnGround == 0,
                    "Expected to find 0 cobblestone on the ground, but found " + nCobbleOnGround);
            h.assertTrue(miner.findInStorage(new ItemStack(Items.COBBLESTONE), true),
                    "Expected to find exactly 1 cobblestone in miner storage");
            h.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 300)
    public void faintResonance(GameTestHelper h) {
        int rpm = 128;
        int miningRadius = ServerConfig.MINING_RADIUS.get();
        int expectedClaimedBlockCount = (int) Math.pow(miningRadius * 2 + 1, 2);

        var miner = minerWithStorage(h, 3, 5, 3)
                .part(RNSBlocks.SHATTERING_RESONATOR.getDefaultState()
                        .setValue(ShatteringResonatorBlock.FACE, AttachFace.WALL)
                        .setValue(ShatteringResonatorBlock.FACING, Direction.WEST), 2, 4, 3)
                .part(RNSBlocks.SHATTERING_RESONATOR.getDefaultState()
                        .setValue(ShatteringResonatorBlock.FACE, AttachFace.WALL)
                        .setValue(ShatteringResonatorBlock.FACING, Direction.NORTH), 3, 4, 2)
                .deposit(0, 2, 0, 7, 2, 7, RNSDeposits.IRON_DEPOSIT.get())
                .place();
        miner.assemble(rpm);

        int ticksToMine = miner.ticksToMine(expectedClaimedBlockCount).orElse(-1);
        h.assertTrue(ticksToMine > 0, "Failed to compute # of ticks to mine an item");
        h.assertTrue(ticksToMine + 1 < 300, "Miner requires more time than the test allows");

        h.runAtTickTime(1, () -> {
            commonPostAssemblyChecks(h, miner, miningRadius, rpm);
        });

        h.runAtTickTime(ticksToMine + 1, () -> {
            var process = miner.process();
            var aggStats = process.innerProcesses.stream().map(p -> p.catStats).collect(Collectors.toSet());
            var activeCRSes = CatalystUsageStats.getLastSatisfiedCRSes(aggStats);
            h.assertTrue(activeCRSes.size() == 2, "Unexpected number of active catalysts: " +
                    activeCRSes.size() + "(2 expected)");
            h.assertTrue(activeCRSes.stream().anyMatch(crs -> crs.name.equals("faint_resonance")),
                    "Faint resonance is not active");
            h.assertTrue(activeCRSes.stream().anyMatch(crs -> crs.name.equals("faint_shattering_resonance")),
                    "Faint shattering resonance is not active");
            h.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 300)
    public void resonance(GameTestHelper h) {
        int rpm = 128;
        int miningRadius = ServerConfig.MINING_RADIUS.get();
        int expectedClaimedBlockCount = (int) Math.pow(miningRadius * 2 + 1, 2);

        var miner = minerWithStorage(h, 3, 5, 3)
                .part(RNSBlocks.SHATTERING_RESONATOR.getDefaultState()
                        .setValue(ShatteringResonatorBlock.FACE, AttachFace.WALL)
                        .setValue(ShatteringResonatorBlock.FACING, Direction.WEST), 2, 4, 3)
                .part(RNSBlocks.SHATTERING_RESONATOR.getDefaultState()
                        .setValue(ShatteringResonatorBlock.FACE, AttachFace.WALL)
                        .setValue(ShatteringResonatorBlock.FACING, Direction.NORTH), 3, 4, 2)
                .part(RNSBlocks.SHATTERING_RESONATOR.getDefaultState()
                        .setValue(ShatteringResonatorBlock.FACE, AttachFace.WALL)
                        .setValue(ShatteringResonatorBlock.FACING, Direction.EAST), 4, 4, 3)
                .part(RNSBlocks.SHATTERING_RESONATOR.getDefaultState()
                        .setValue(ShatteringResonatorBlock.FACE, AttachFace.WALL)
                        .setValue(ShatteringResonatorBlock.FACING, Direction.SOUTH), 3, 4, 4)
                .deposit(0, 2, 0, 7, 2, 7, RNSDeposits.IRON_DEPOSIT.get())
                .place();
        miner.assemble(rpm);

        int ticksToMine = miner.ticksToMine(expectedClaimedBlockCount).orElse(-1);
        h.assertTrue(ticksToMine > 0, "Failed to compute # of ticks to mine an item");
        h.assertTrue(ticksToMine + 1 < 300, "Miner requires more time than the test allows");

        h.runAtTickTime(1, () -> {
            commonPostAssemblyChecks(h, miner, miningRadius, rpm);
        });

        h.runAtTickTime(ticksToMine + 1, () -> {
            var process = miner.process();
            var aggStats = process.innerProcesses.stream().map(p -> p.catStats).collect(Collectors.toSet());
            var activeCRSes = CatalystUsageStats.getLastSatisfiedCRSes(aggStats);
            h.assertTrue(activeCRSes.size() == 4, "Unexpected number of active catalysts: " +
                    activeCRSes.size() + "(4 expected)");
            h.assertTrue(activeCRSes.stream().anyMatch(crs -> crs.name.equals("faint_resonance")),
                    "Faint resonance is not active");
            h.assertTrue(activeCRSes.stream().anyMatch(crs -> crs.name.equals("faint_shattering_resonance")),
                    "Faint shattering resonance is not active");
            h.assertTrue(activeCRSes.stream().anyMatch(crs -> crs.name.equals("resonance")),
                    "Resonance is not active");
            h.assertTrue(activeCRSes.stream().anyMatch(crs -> crs.name.equals("shattering_resonance")),
                    "Shattering resonance is not active");
            h.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 300)
    public void overclock(GameTestHelper h) {
        int rpm = 128;
        int miningRadius = ServerConfig.MINING_RADIUS.get();
        int expectedClaimedBlockCount = (int) Math.pow(miningRadius * 2 + 1, 2);
        int lavaAmount = 1000;
        int lavaPerMine = 20;

        var miner = minerWithStorageAndTank(h, 3, 5, 3)
                .deposit(0, 2, 0, 7, 2, 7, RNSDeposits.IRON_DEPOSIT.get())
                .place();
        miner.assemble(rpm);

        int ticksToMine = miner.ticksToMine(expectedClaimedBlockCount).orElse(-1);
        h.assertTrue(ticksToMine > 0, "Failed to compute # of ticks to mine an item");
        h.assertTrue(ticksToMine + 1 < 300, "Miner requires more time than the test allows");

        h.runAtTickTime(1, () -> {
            commonPostAssemblyChecks(h, miner, miningRadius, rpm);
            miner.storage().getFluids().fill(new FluidStack(Fluids.LAVA, lavaAmount), IFluidHandler.FluidAction.EXECUTE);
        });

        h.runAtTickTime(ticksToMine + 1, () -> {
            var process = miner.process();
            var aggStats = process.innerProcesses.stream().map(p -> p.catStats).collect(Collectors.toSet());
            var activeCRSes = CatalystUsageStats.getLastSatisfiedCRSes(aggStats);
            h.assertTrue(activeCRSes.size() == 1, "Unexpected number of active catalysts: " +
                    activeCRSes.size() + "(1 expected)");
            h.assertTrue(activeCRSes.stream().anyMatch(crs -> crs.name.equals("overclock")),
                    "Overclock is not active");
            h.assertTrue(miner.findInStorage(new ItemStack(Items.COBBLESTONE), false),
                    "Expected to find at least 1 cobblestone in miner storage");
            h.assertTrue(miner.findInStorage(new ItemStack(Items.IRON_NUGGET), false),
                    "Expected to find at least 1 iron nugget in miner storage");
            h.assertTrue(miner.findInStorage(new FluidStack(Fluids.LAVA, lavaAmount - lavaPerMine), true),
                    "Expected to find exactly " + (lavaAmount - lavaPerMine) + " of lava in miner storage");
            h.succeed();
        });
    }

    private static void setServerConfig(int miningSpeed, int miningRadius, int maxRpm) {
        ServerConfig.MINING_SPEED.set(miningSpeed);
        ServerConfig.MINING_RADIUS.set(miningRadius);
        AllConfigs.server().kinetics.maxRotationSpeed.set(maxRpm);
    }

    private static MinerSetupBuilder miner(GameTestHelper helper, int x, int y, int z) {
        return MinerSetupBuilder.create(helper)
                .bearing(x, y, z)
                .part(AllBlocks.ANDESITE_CASING.getDefaultState(), x, y - 1, z)
                .head(x, y - 2, z);
    }

    private static MinerSetupBuilder minerWithStorage(GameTestHelper helper, int x, int y, int z) {
        return MinerSetupBuilder.create(helper)
                .bearing(x, y, z)
                .part(Blocks.BARREL.defaultBlockState(), x, y - 1, z)
                .head(x, y - 2, z);
    }

    private static MinerSetupBuilder minerWithStorageAndTank(GameTestHelper helper, int x, int y, int z) {
        return MinerSetupBuilder.create(helper)
                .bearing(x, y, z)
                .part(Blocks.BARREL.defaultBlockState(), x, y - 1, z)
                .part(AllBlocks.FLUID_TANK.getDefaultState(), x - 1, y - 1, z)
                .head(x, y - 2, z);
    }

    private static int countGroundItems(GameTestHelper helper, BlockPos relativeMineHeadPos, Item item) {
        var center = helper.absolutePos(relativeMineHeadPos);
        var searchBox = new AABB(center).inflate(2);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, searchBox, entity -> entity.getItem().is(item))
                .stream()
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }

    private static void commonPostAssemblyChecks(
            GameTestHelper h, MinerSetup miner, int miningRadius, int rpm
    ) {
        int expectedClaimedBlockCount = (int) Math.pow(miningRadius * 2 + 1, 2);

        h.assertTrue(miner.isRunning(), "Miner is not running");
        h.assertTrue(miner.bearing().getSpeed() == rpm, "Unexpected miner speed: " +
                miner.bearing().getSpeed());
        h.assertTrue(miner.process() != null, "Mining process is null");
        var claimedBlocks = miner.behavior().getClaimedDepositBlocks();
        h.assertTrue(claimedBlocks != null,
                "Miner did not claim deposit blocks");
        h.assertTrue(claimedBlocks.size() == expectedClaimedBlockCount,
                "Unexpected claimed deposit count: " + claimedBlocks.size() +
                        " (" + expectedClaimedBlockCount + " expected)");
        h.assertTrue(miner.process().innerProcesses.size() == 1,
                "Unexpected number of inner processes: " + miner.process().innerProcesses.size() +
                        " (1 expected)");
    }
}


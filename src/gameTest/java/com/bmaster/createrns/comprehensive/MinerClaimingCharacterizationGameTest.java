package com.bmaster.createrns.comprehensive;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapter.OperatingSublevel;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapterHolder;
import com.bmaster.createrns.content.deposit.operating.sublevel.VanillaOperatingSublevelAdapter;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.util.MinerSetup;
import com.bmaster.createrns.util.MinerSetupBuilder;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SuppressWarnings("DataFlowIssue")
@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MinerClaimingCharacterizationGameTest {
    private static final int TEST_RPM = 128;

    @GameTest(template = "empty16x16", timeoutTicks = 20)
    public void exclusiveClaimsDoNotOverlapAndReleasedClaimsAreReclaimed(GameTestHelper helper) {
        var first = miner(helper, 3, 5, 5)
                .deposit(0, 2, 0, 10, 2, 10)
                .place();
        var second = miner(helper, 5, 5, 5)
                .deposit(0, 2, 0, 10, 2, 10)
                .place();
        var releasedOverlap = new AtomicReference<Set<BlockPos>>(Set.of());
        var initialSecondClaimSize = new AtomicInteger();

        first.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var firstClaim = getClaimedBlocks(helper, first);
            helper.assertFalse(firstClaim.isEmpty(), "First miner should claim deposit blocks");
            assertProcessTargetsMatchClaim(helper, first, firstClaim);
            second.assemble(TEST_RPM);
        });

        helper.runAtTickTime(4, () -> {
            var firstClaim = getClaimedBlocks(helper, first);
            var secondClaim = getClaimedBlocks(helper, second);
            helper.assertTrue(disjoint(firstClaim, secondClaim), "Exclusive miner claims must not overlap");
            assertProcessTargetsMatchClaim(helper, first, firstClaim);
            assertProcessTargetsMatchClaim(helper, second, secondClaim);

            var secondArea = second.behavior().getOperatingBoundingBox();
            helper.assertTrue(secondArea != null, "Second miner should have a claiming area");
            var overlap = firstClaim.stream()
                    .filter(secondArea::isInside)
                    .collect(Collectors.toUnmodifiableSet());
            helper.assertFalse(overlap.isEmpty(), "Miner claiming areas should overlap for this test");

            releasedOverlap.set(overlap);
            initialSecondClaimSize.set(secondClaim.size());
            helper.setBlock(first.bearingPos(), Blocks.AIR.defaultBlockState());
        });

        helper.runAtTickTime(6, () -> {
            var secondClaim = getClaimedBlocks(helper, second);
            helper.assertTrue(secondClaim.containsAll(releasedOverlap.get()),
                    "Remaining miner should reclaim blocks released by the removed bearing");
            helper.assertTrue(secondClaim.size() > initialSecondClaimSize.get(),
                    "Remaining miner's claim should grow after the competing bearing is removed");
            assertProcessTargetsMatchClaim(helper, second, secondClaim);
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void depositChangesRefreshClaimAndProcessTargets(GameTestHelper helper) {
        var rootPos = new BlockPos(5, 2, 5);
        var addedPos = rootPos.below();
        var miner = miner(helper, 5, 5, 5)
                .deposit(rootPos.getX(), rootPos.getY(), rootPos.getZ())
                .place();

        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var initialClaim = getClaimedBlocks(helper, miner);
            helper.assertValueEqual(initialClaim, absolutePositions(helper, rootPos), "initial miner claim");
            assertProcessTargetsMatchClaim(helper, miner, initialClaim);
            helper.setBlock(addedPos, RNSDeposits.DEPLETED_DEPOSIT.getDefaultState());
        });

        helper.runAtTickTime(3, () -> {
            var expandedClaim = getClaimedBlocks(helper, miner);
            helper.assertValueEqual(expandedClaim, absolutePositions(helper, rootPos, addedPos),
                    "claim after placing a connected deposit");
            assertProcessTargetsMatchClaim(helper, miner, expandedClaim);
            helper.setBlock(addedPos, Blocks.AIR.defaultBlockState());
        });

        helper.runAtTickTime(4, () -> {
            var reducedClaim = getClaimedBlocks(helper, miner);
            helper.assertValueEqual(reducedClaim, absolutePositions(helper, rootPos),
                    "claim after removing a connected deposit");
            assertProcessTargetsMatchClaim(helper, miner, reducedClaim);
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16")
    public void confinedVeinRequiresRootAndRespectsAreaBounds(GameTestHelper helper) {
        var anchorPos = new BlockPos(5, 6, 5);
        var rootPos = anchorPos.below();
        var insideDepthPos = rootPos.below();
        var outsideDepthPos = insideDepthPos.below();
        var insideRadiusPos = rootPos.east();
        var outsideRadiusPos = insideRadiusPos.east();
        var depositState = RNSDeposits.DEPLETED_DEPOSIT.getDefaultState();

        helper.setBlock(rootPos, depositState);
        helper.setBlock(insideDepthPos, depositState);
        helper.setBlock(outsideDepthPos, depositState);
        helper.setBlock(insideRadiusPos, depositState);
        helper.setBlock(outsideRadiusPos, depositState);

        var claimer = new FixedAreaClaimer(helper.getLevel(), helper.absolutePos(anchorPos));
        var expected = absolutePositions(helper, rootPos, insideDepthPos, insideRadiusPos);
        helper.assertValueEqual(claimer.getConfinedDepositVein(), expected,
                "deposit vein confined to the configured radius and depth");

        helper.setBlock(rootPos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(claimer.getConfinedDepositVein().isEmpty(),
                "Deposits inside the area should not be found when the root deposit is absent");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void finiteDurabilityDepletesAndNormalRemovalClearsStoredValue(GameTestHelper helper) {
        boolean previousInfiniteDeposits = ServerConfig.INFINITE_DEPOSITS.get();
        ServerConfig.INFINITE_DEPOSITS.set(false);
        try {
            var relativePos = new BlockPos(5, 2, 5);
            var absolutePos = helper.absolutePos(relativePos);
            var level = helper.getLevel();
            var depositState = RNSDeposits.IRON_DEPOSIT.getDefaultState();
            var replacementState = RNSDeposits.DEPLETED_DEPOSIT.getDefaultState();

            helper.setBlock(relativePos, depositState);
            helper.assertTrue(DepositDurabilityManager.set(level, absolutePos, 2),
                    "Finite durability should be stored");

            DepositDurabilityManager.useDepositBlock(level, absolutePos, replacementState);
            helper.assertValueEqual(level.getBlockState(absolutePos), depositState,
                    "deposit state after its first finite use");
            helper.assertValueEqual(DepositDurabilityManager.get(level, absolutePos, false), 1L,
                    "durability after the first finite use");

            DepositDurabilityManager.useDepositBlock(level, absolutePos, replacementState);
            helper.assertValueEqual(level.getBlockState(absolutePos), replacementState,
                    "deposit state after its final finite use");
            helper.assertValueEqual(DepositDurabilityManager.get(level, absolutePos, false), -1L,
                    "durability entry after depletion");

            helper.setBlock(relativePos, depositState);
            helper.assertTrue(DepositDurabilityManager.set(level, absolutePos, 3),
                    "Durability should be stored before normal removal");
            helper.setBlock(relativePos, Blocks.AIR.defaultBlockState());
            helper.assertValueEqual(DepositDurabilityManager.get(level, absolutePos, false), -1L,
                    "durability entry after normal block removal");
        } finally {
            ServerConfig.INFINITE_DEPOSITS.set(previousInfiniteDeposits);
        }
        helper.succeed();
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void disassemblyClearsClaimAndProcess(GameTestHelper helper) {
        var miner = miner(helper, 5, 5, 5)
                .deposit(5, 2, 5)
                .place();

        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var claim = getClaimedBlocks(helper, miner);
            assertProcessTargetsMatchClaim(helper, miner, claim);
            miner.disassemble();
        });

        helper.runAtTickTime(4, () -> {
            helper.assertTrue(miner.behavior().getClaimedDepositBlocks() != null,
                    "Disassembled miner should have a finalized claim");
            helper.assertTrue(miner.behavior().getClaimedDepositBlocks().positions().isEmpty(),
                    "Disassembled miner should release its exclusive claim");
            helper.assertTrue(miner.process() == null, "Disassembled miner should clear its mining process");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void localClaimUsesMineHeadSpaceAndMinerLevel(GameTestHelper helper) {
        var miner = miner(helper, 5, 5, 5)
                .deposit(5, 2, 5)
                .place();

        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var level = helper.getLevel();
            var anchor = behavior.getOperatingAnchor();
            var bounds = behavior.getOperatingBoundingBox();
            var detectionDimensions = behavior.getCrossSublevelOperatingDimensions();
            var claimedBlocks = behavior.getClaimedDepositBlocks();
            helper.assertTrue(anchor != null, "Assembled miner should have an effective mine-head tip");
            helper.assertTrue(bounds != null, "Assembled miner should have mining bounds");
            helper.assertTrue(detectionDimensions != null,
                    "Assembled miner should have deposit detection dimensions");
            helper.assertTrue(claimedBlocks != null, "Assembled miner should initialize its local claim");
            helper.assertFalse(claimedBlocks.crossSublevel(), "Local claims should not be marked cross-sublevel");

            var adapter = OperatingSublevelAdapterHolder.getAdapter();
            var operatingSublevel = adapter.getOperatingSublevel(level, anchor);
            helper.assertValueEqual(operatingSublevel.identity(), OperatingSublevel.MAIN_ID,
                    "positions outside a Sable sublevel should use the main-sublevel identity");
            helper.assertTrue(miner.process().level == level, "Mining process should use the miner's level");

            helper.assertTrue(adapter.getCrossSublevelDepositBlocks(
                    level, operatingSublevel, anchor, behavior.getOperatingDirection(), detectionDimensions).isEmpty(),
                    "Stage 4 operating-sublevel adapter must not expose cross-sublevel targets");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16")
    public void crossSublevelOperatingAreaUsesRadiusLengthAndOffset(GameTestHelper helper) {
        var dimensions = new IDepositBlockOperator.CrossSublevelOperatingDimensions(2, 6, 4);
        var area = IDepositBlockOperator.createCrossSublevelOperatingArea(
                new BlockPos(10, 20, 30), Direction.DOWN, dimensions);

        helper.assertValueEqual(area.minX, 8.5, "detection area minimum X");
        helper.assertValueEqual(area.minY, 13.5, "detection area minimum Y");
        helper.assertValueEqual(area.minZ, 28.5, "detection area minimum Z");
        helper.assertValueEqual(area.maxX, 12.5, "detection area maximum X");
        helper.assertValueEqual(area.maxY, 19.5, "detection area maximum Y");
        helper.assertValueEqual(area.maxZ, 32.5, "detection area maximum Z");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void vanillaOperatingSublevelAdapterPreservesBlockPosDistances(GameTestHelper helper) {
        var adapter = new VanillaOperatingSublevelAdapter();
        var level = helper.getLevel();
        var firstPos = new BlockPos(2, 3, 4);
        var secondPos = new BlockPos(-3, 8, 1);

        helper.assertValueEqual(adapter.getOperatingSublevel(level, firstPos),
                adapter.getOperatingSublevel(level, secondPos), "vanilla operating sublevel");
        helper.assertValueEqual(adapter.distManhattan(level, firstPos, secondPos),
                (double) firstPos.distManhattan(secondPos), "vanilla Manhattan distance");
        helper.assertValueEqual(adapter.distSqr(level, firstPos, secondPos),
                firstPos.distSqr(secondPos), "vanilla squared distance");
        helper.succeed();
    }

    private static MinerSetupBuilder miner(GameTestHelper helper, int x, int y, int z) {
        return MinerSetupBuilder.create(helper)
                .bearing(x, y, z)
                .part(AllBlocks.ANDESITE_CASING.getDefaultState(), x, y - 1, z)
                .head(x, y - 2, z);
    }

    private static Set<BlockPos> getClaimedBlocks(GameTestHelper helper, MinerSetup miner) {
        var claimedBlocks = miner.behavior().getClaimedDepositBlocks();
        helper.assertTrue(claimedBlocks != null, "Assembled miner should have initialized its claim");
        return claimedBlocks.positions();
    }

    private static void assertProcessTargetsMatchClaim(
            GameTestHelper helper, MinerSetup miner, Set<BlockPos> expectedClaim
    ) {
        var claimedBlocks = miner.behavior().getClaimedDepositBlocks();
        helper.assertTrue(claimedBlocks != null, "Assembled miner should have initialized its claim");
        helper.assertValueEqual(claimedBlocks.positions(), expectedClaim, "miner claim");

        var processTargets = getProcessTargets(helper, miner);
        helper.assertTrue(processTargets.equals(expectedClaim),
                "Process targets should match the claim (expected %s blocks, got %s)"
                        .formatted(expectedClaim.size(), processTargets.size()));
    }

    private static Set<BlockPos> getProcessTargets(GameTestHelper helper, MinerSetup miner) {
        var process = miner.process();
        helper.assertTrue(process != null, "Assembled miner should have initialized its mining process");
        return process.innerProcesses.stream()
                .flatMap(innerProcess -> innerProcess.depositPositions.stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<BlockPos> absolutePositions(GameTestHelper helper, BlockPos... relativePositions) {
        var positions = new HashSet<BlockPos>();
        for (var pos : relativePositions) {
            positions.add(helper.absolutePos(pos));
        }
        return Set.copyOf(positions);
    }

    private static boolean disjoint(Set<BlockPos> first, Set<BlockPos> second) {
        return first.stream().noneMatch(second::contains);
    }

    private static class FixedAreaClaimer implements IDepositBlockClaimer {
        private static final OperatingDimensions CLAIMING_AREA = new OperatingDimensions(1, 2);

        private final Level level;
        private final BlockPos anchor;
        private @Nullable ClaimedDepositBlocks claimedBlocks;

        private FixedAreaClaimer(Level level, BlockPos anchor) {
            this.level = level;
            this.anchor = anchor;
        }

        @Override
        public BlockPos getBlockPos() {
            return anchor;
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public OperatingSublevel getSublevel() {
            return OperatingSublevelAdapterHolder.getAdapter().getOperatingSublevel(level, anchor);
        }

        @Override
        public OperatingDimensions getOperatingDimensions() {
            return CLAIMING_AREA;
        }

        @Override
        public BlockPos getOperatingAnchor() {
            return anchor;
        }

        @Override
        public Direction getOperatingDirection() {
            return Direction.DOWN;
        }

        @Override
        public @Nullable ClaimedDepositBlocks getClaimedDepositBlocks() {
            return claimedBlocks;
        }

        @Override
        public void setClaimedDepositBlocks(@Nullable ClaimedDepositBlocks claimedBlocks) {
            this.claimedBlocks = claimedBlocks;
        }

        @Override
        public void claimDepositBlocks() {}
    }
}

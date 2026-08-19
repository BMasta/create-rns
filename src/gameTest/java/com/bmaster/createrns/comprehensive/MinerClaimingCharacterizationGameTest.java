package com.bmaster.createrns.comprehensive;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.content.deposit.claiming.DepositClaimerInstanceHolder;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.bmaster.createrns.content.deposit.info.DepositIndex;
import com.bmaster.createrns.content.deposit.info.IDepositIndex;
import com.bmaster.createrns.content.deposit.mining.MiningProcess;
import com.bmaster.createrns.content.deposit.mining.recipe.DepositDurability;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipeLookup;
import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.util.CodecHelper;
import com.bmaster.createrns.util.MinerSetup;
import com.bmaster.createrns.util.MinerSetupBuilder;
import com.simibubi.create.AllBlocks;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SuppressWarnings("DataFlowIssue")
@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MinerClaimingCharacterizationGameTest {
    private static final Field DEPOSIT_DURABILITIES_FIELD = getDepositIndexField("depositDurabilities");

    private static final int TEST_RPM = 128;
    private static final int LEGACY_MINING_PROGRESS = MiningProcess.BASE_PROGRESS - 1;

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
    public void destroyingSmallerClaimerReleasesEntireClaimToRemainingMiner(GameTestHelper helper) {
        int miningRadius = ServerConfig.MINING_RADIUS.get();
        helper.assertTrue(miningRadius > 0 && miningRadius <= 7,
                "Test requires a mining radius between 1 and 7 blocks");

        var firstBuilder = miner(helper, 7, 5, 7);
        var secondBuilder = miner(helper, 8, 5, 7);
        var expectedVeinBuilder = new HashSet<BlockPos>();

        for (int x = 7; x <= 8 + miningRadius; x++) {
            firstBuilder.deposit(x, 2, 7);
            secondBuilder.deposit(x, 2, 7);
            expectedVeinBuilder.add(helper.absolutePos(new BlockPos(x, 2, 7)));
        }
        for (int z = 7 - miningRadius; z <= 7 + miningRadius; z++) {
            firstBuilder.deposit(8 + miningRadius, 2, z);
            secondBuilder.deposit(8 + miningRadius, 2, z);
            expectedVeinBuilder.add(helper.absolutePos(new BlockPos(8 + miningRadius, 2, z)));
        }

        var first = firstBuilder.place();
        var second = secondBuilder.place();
        var expectedVein = Set.copyOf(expectedVeinBuilder);
        var destroyedMiner = new AtomicReference<MinerSetup>();
        var remainingMiner = new AtomicReference<MinerSetup>();
        var releasedClaim = new AtomicReference<Set<BlockPos>>();
        var initialRemainingClaimSize = new AtomicInteger();

        first.assemble(TEST_RPM);
        helper.runAtTickTime(2, () -> second.assemble(TEST_RPM));

        helper.runAtTickTime(4, () -> {
            var firstClaim = getClaimedBlocks(helper, first);
            var secondClaim = getClaimedBlocks(helper, second);
            CodecHelper.assertValueEqual(helper, unionClaims(helper, first, second), expectedVein,
                    "deposit vein claimed by both miners");
            helper.assertTrue(disjoint(firstClaim, secondClaim), "Exclusive miner claims must not overlap");
            helper.assertTrue(getOperatingArea(helper, first).intersects(getOperatingArea(helper, second)),
                    "Miner operating areas should intersect");
            assertRegisteredClaimers(helper, second, first, second);

            helper.assertFalse(firstClaim.isEmpty(), "First miner should have a non-empty claim");
            helper.assertFalse(secondClaim.isEmpty(), "Second miner should have a non-empty claim");
            helper.assertTrue(firstClaim.size() != secondClaim.size(),
                    "One miner should have a strictly smaller claim");

            var smaller = firstClaim.size() < secondClaim.size() ? first : second;
            var remaining = smaller == first ? second : first;
            destroyedMiner.set(smaller);
            remainingMiner.set(remaining);
            releasedClaim.set(Set.copyOf(getClaimedBlocks(helper, smaller)));
            initialRemainingClaimSize.set(getClaimedBlocks(helper, remaining).size());

            helper.destroyBlock(smaller.bearingPos());
        });

        helper.runAtTickTime(6, () -> {
            var destroyed = destroyedMiner.get();
            var remaining = remainingMiner.get();
            var remainingClaim = getClaimedBlocks(helper, remaining);

            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(destroyed.bearingPos())) == null,
                    "Destroyed miner bearing should no longer exist");
            CodecHelper.assertValueEqual(helper, remainingClaim, expectedVein,
                    "remaining miner claim after destroying the smaller claimer");
            helper.assertTrue(remainingClaim.containsAll(releasedClaim.get()),
                    "Remaining miner should reclaim every block released by the destroyed miner");
            helper.assertTrue(remainingClaim.size() > initialRemainingClaimSize.get(),
                    "Remaining miner's claim should grow after the smaller claimer is destroyed");
            assertRegisteredClaimers(helper, remaining, remaining);
            assertProcessTargetsMatchClaim(helper, remaining, expectedVein);
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 20)
    public void threeOverlappingMinersPreserveAndRedistributeExclusiveClaims(GameTestHelper helper) {
        var minerA = miner(helper, 3, 5, 5)
                .deposit(1, 2, 5, 9, 2, 5)
                .place();
        var minerB = miner(helper, 5, 5, 5)
                .deposit(1, 2, 5, 9, 2, 5)
                .place();
        var minerC = miner(helper, 7, 5, 5)
                .deposit(1, 2, 5, 9, 2, 5)
                .place();
        var expectedVeinBuilder = new HashSet<BlockPos>();
        for (int x = 1; x <= 9; x++) {
            expectedVeinBuilder.add(helper.absolutePos(new BlockPos(x, 2, 5)));
        }
        var expectedVein = Set.copyOf(expectedVeinBuilder);
        var claimAfterAAssembly = new AtomicReference<Set<BlockPos>>();
        var claimAfterBAssembly = new AtomicReference<Set<BlockPos>>();

        minerA.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            claimAfterAAssembly.set(Set.copyOf(getClaimedBlocks(helper, minerA)));
            minerB.assemble(TEST_RPM);
        });

        helper.runAtTickTime(4, () -> {
            CodecHelper.assertValueEqual(helper, getClaimedBlocks(helper, minerA), claimAfterAAssembly.get(),
                    "miner A claim after assembling miner B");
            claimAfterBAssembly.set(Set.copyOf(getClaimedBlocks(helper, minerB)));
            minerC.assemble(TEST_RPM);
        });

        helper.runAtTickTime(6, () -> {
            CodecHelper.assertValueEqual(helper, getClaimedBlocks(helper, minerA), claimAfterAAssembly.get(),
                    "miner A claim after assembling miner C");
            CodecHelper.assertValueEqual(helper, getClaimedBlocks(helper, minerB), claimAfterBAssembly.get(),
                    "miner B claim after assembling miner C");
            CodecHelper.assertValueEqual(helper, unionClaims(helper, minerA, minerB, minerC), expectedVein,
                    "deposit vein claimed by miners A, B, and C");
            assertClaimsAreDisjoint(helper, minerA, minerB, minerC);

            var areaA = getOperatingArea(helper, minerA);
            var areaB = getOperatingArea(helper, minerB);
            var areaC = getOperatingArea(helper, minerC);
            helper.assertTrue(areaA.intersects(areaB) && areaA.intersects(areaC) && areaB.intersects(areaC),
                    "All three miner operating areas should intersect");
            assertRegisteredClaimers(helper, minerB, minerA, minerB, minerC);

            minerA.disassemble();
        });

        helper.runAtTickTime(8, () -> {
            CodecHelper.assertValueEqual(helper, minerA.behavior().getClaimedDepositBlocks(), Set.of(),
                    "miner A claim after disassembly");
            assertRegisteredClaimers(helper, minerB, minerB, minerC);

            var areaB = getOperatingArea(helper, minerB);
            var areaC = getOperatingArea(helper, minerC);
            var expectedRemainingCoverage = expectedVein.stream()
                    .filter(pos -> areaB.isInside(pos) || areaC.isInside(pos))
                    .collect(Collectors.toUnmodifiableSet());
            var releasedWithinRemainingAreas = claimAfterAAssembly.get().stream()
                    .filter(pos -> areaB.isInside(pos) || areaC.isInside(pos))
                    .collect(Collectors.toUnmodifiableSet());
            var remainingClaims = unionClaims(helper, minerB, minerC);

            helper.assertFalse(releasedWithinRemainingAreas.isEmpty(),
                    "Miner A should release blocks that miners B or C can reclaim");
            helper.assertTrue(remainingClaims.containsAll(releasedWithinRemainingAreas),
                    "Miners B and C should reclaim every eligible block released by miner A");
            CodecHelper.assertValueEqual(helper, remainingClaims, expectedRemainingCoverage,
                    "deposit blocks covered by miners B and C after disassembling miner A");
            assertClaimsAreDisjoint(helper, minerB, minerC);

            minerC.disassemble();
        });

        helper.runAtTickTime(10, () -> {
            var areaB = getOperatingArea(helper, minerB);
            var expectedMinerBClaim = expectedVein.stream()
                    .filter(areaB::isInside)
                    .collect(Collectors.toUnmodifiableSet());

            CodecHelper.assertValueEqual(helper, getClaimedBlocks(helper, minerB), expectedMinerBClaim,
                    "miner B claim after disassembling miner C");
            CodecHelper.assertValueEqual(helper, minerC.behavior().getClaimedDepositBlocks(), Set.of(),
                    "miner C claim after disassembly");
            assertRegisteredClaimers(helper, minerB, minerB);
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
            CodecHelper.assertValueEqual(helper, initialClaim, absolutePositions(helper, rootPos),
                    "initial miner claim");
            assertProcessTargetsMatchClaim(helper, miner, initialClaim);
            helper.setBlock(addedPos, RNSDeposits.DEPLETED_DEPOSIT.getDefaultState());
        });

        helper.runAtTickTime(3, () -> {
            var expandedClaim = getClaimedBlocks(helper, miner);
            CodecHelper.assertValueEqual(helper, expandedClaim, absolutePositions(helper, rootPos, addedPos),
                    "claim after placing a connected deposit");
            assertProcessTargetsMatchClaim(helper, miner, expandedClaim);
            helper.setBlock(addedPos, Blocks.AIR.defaultBlockState());
        });

        helper.runAtTickTime(4, () -> {
            var reducedClaim = getClaimedBlocks(helper, miner);
            CodecHelper.assertValueEqual(helper, reducedClaim, absolutePositions(helper, rootPos),
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
        var start = claimer.getOperatingStart();
        var area = claimer.getOperatingBoundingBox();
        var expected = absolutePositions(helper, rootPos, insideDepthPos, insideRadiusPos);
        CodecHelper.assertValueEqual(helper,
                IDepositBlockOperator.getConfinedDepositVein(helper.getLevel(), start, area), expected,
                "deposit vein confined to the configured radius and depth");

        helper.setBlock(rootPos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(IDepositBlockOperator.getConfinedDepositVein(helper.getLevel(), start, area).isEmpty(),
                "Deposits inside the area should not be found when the root deposit is absent");
        helper.succeed();
    }

    @GameTest(template = "empty16x16", batch = "durability_infinite", timeoutTicks = 15)
    public void infiniteDepositsDoNotInitializeDurabilityWhenClaimed(GameTestHelper helper) {
        boolean previousInfiniteDeposits = ServerConfig.INFINITE_DEPOSITS.get();
        ServerConfig.INFINITE_DEPOSITS.set(true);
        try {
            var centerPos = new BlockPos(5, 2, 5);
            var expectedVein = absolutePlane(helper, 4, 2, 4, 6, 6);
            var miner = miner(helper, 5, 5, 5)
                    .deposit(4, 2, 4, 6, 2, 6, RNSDeposits.IRON_DEPOSIT.get())
                    .place();

            miner.assemble(TEST_RPM);

            helper.runAtTickTime(2, () -> {
                try {
                    helper.assertFalse(getClaimedBlocks(helper, miner).isEmpty(),
                            "Miner should claim at least one infinite deposit block");
                    for (var pos : expectedVein) {
                        CodecHelper.assertValueEqual(helper, getRawDepositDurability(helper, pos), OptionalLong.empty(),
                                "raw durability for an infinite deposit block");
                    }

                    var absoluteCenterPos = helper.absolutePos(centerPos);
                    CodecHelper.assertValueEqual(helper, DepositDurabilityManager.getDepositBlockDurability(
                                    helper.getLevel(), absoluteCenterPos),
                            0L, "public durability for an infinite deposit block");
                    CodecHelper.assertValueEqual(helper, getRawDepositDurability(helper, absoluteCenterPos),
                            OptionalLong.empty(), "raw durability after reading an infinite deposit block");
                } finally {
                    ServerConfig.INFINITE_DEPOSITS.set(previousInfiniteDeposits);
                }
                helper.succeed();
            });
        } catch (RuntimeException | Error e) {
            ServerConfig.INFINITE_DEPOSITS.set(previousInfiniteDeposits);
            throw e;
        }
    }

    @GameTest(template = "empty16x16", batch = "durability_finite", timeoutTicks = 15)
    public void finiteClaimInitializesEntireVeinAndInfiniteModePreservesRawDurability(GameTestHelper helper) {
        boolean previousInfiniteDeposits = ServerConfig.INFINITE_DEPOSITS.get();
        ServerConfig.INFINITE_DEPOSITS.set(false);
        try {
            int miningRadius = ServerConfig.MINING_RADIUS.get();
            helper.assertTrue(miningRadius <= 7, "Test requires a mining radius of at most 7 blocks");

            var centerPos = new BlockPos(7, 2, 7);
            var edgePos = new BlockPos(4, 2, 7);
            var unclaimedPos = new BlockPos(15, 2, 7);
            var expectedVeinBuilder = new HashSet<BlockPos>();
            for (int x = 4; x <= 10; x++) {
                for (int z = 4; z <= 10; z++) {
                    expectedVeinBuilder.add(helper.absolutePos(new BlockPos(x, 2, z)));
                }
            }
            for (int x = 11; x <= 15; x++) {
                expectedVeinBuilder.add(helper.absolutePos(new BlockPos(x, 2, 7)));
            }
            var expectedVein = Set.copyOf(expectedVeinBuilder);
            var miner = miner(helper, 7, 5, 7)
                    .deposit(4, 2, 4, 10, 2, 10, RNSDeposits.IRON_DEPOSIT.get())
                    .deposit(11, 2, 7, 15, 2, 7, RNSDeposits.IRON_DEPOSIT.get())
                    .place();

            for (var pos : expectedVein) {
                CodecHelper.assertValueEqual(helper, getRawDepositDurability(helper, pos), OptionalLong.empty(),
                        "raw durability immediately after deposit placement");
            }

            miner.assemble(TEST_RPM);

            helper.runAtTickTime(2, () -> {
                try {
                    var level = helper.getLevel();
                    var absoluteCenterPos = helper.absolutePos(centerPos);
                    var absoluteEdgePos = helper.absolutePos(edgePos);
                    var absoluteUnclaimedPos = helper.absolutePos(unclaimedPos);
                    var claimedBlocks = getClaimedBlocks(helper, miner);
                    helper.assertFalse(claimedBlocks.contains(absoluteUnclaimedPos),
                            "Tail endpoint should be outside the miner's claiming area");

                    var rawBeforeInfinite = new HashMap<BlockPos, Long>();
                    for (var pos : expectedVein) {
                        var raw = getRawDepositDurability(helper, pos);
                        helper.assertTrue(raw.isPresent(),
                                "Every block in the connected vein should have initialized durability");
                        rawBeforeInfinite.put(pos, raw.getAsLong());
                    }

                    var recipe = MiningRecipeLookup.find(level, RNSDeposits.IRON_DEPOSIT.get());
                    helper.assertTrue(recipe != null, "Iron deposit should have a mining recipe");
                    var durability = recipe.getDurability();
                    assertDurabilityWithinRecipeSpread(helper, rawBeforeInfinite.get(absoluteCenterPos),
                            durability.core(), durability, "core deposit durability");
                    assertDurabilityWithinRecipeSpread(helper, rawBeforeInfinite.get(absoluteEdgePos),
                            durability.edge(), durability, "edge deposit durability");

                    helper.destroyBlock(unclaimedPos);
                    CodecHelper.assertValueEqual(helper, getRawDepositDurability(helper, absoluteUnclaimedPos),
                            OptionalLong.empty(), "raw durability after destroying a deposit block");
                    rawBeforeInfinite.remove(absoluteUnclaimedPos);

                    ServerConfig.INFINITE_DEPOSITS.set(true);
                    for (var entry : rawBeforeInfinite.entrySet()) {
                        CodecHelper.assertValueEqual(helper, getRawDepositDurability(helper, entry.getKey()),
                                OptionalLong.of(entry.getValue()),
                                "raw durability retained after enabling infinite deposits");
                    }

                    long originalCenterDurability = rawBeforeInfinite.get(absoluteCenterPos);
                    long replacementCenterDurability = originalCenterDurability + 1;
                    CodecHelper.assertValueEqual(helper, DepositDurabilityManager.getDepositBlockDurability(
                            level, absoluteCenterPos), 0L, "public durability after enabling infinite deposits");
                    helper.assertFalse(DepositDurabilityManager.setDepositBlockDurability(
                                    level, absoluteCenterPos, replacementCenterDurability),
                            "Policy-aware durability setter should reject changes while deposits are infinite");
                    CodecHelper.assertValueEqual(helper, getRawDepositDurability(helper, absoluteCenterPos),
                            OptionalLong.of(originalCenterDurability),
                            "raw durability after rejected policy-aware change");
                } finally {
                    ServerConfig.INFINITE_DEPOSITS.set(previousInfiniteDeposits);
                }
                helper.succeed();
            });
        } catch (RuntimeException | Error e) {
            ServerConfig.INFINITE_DEPOSITS.set(previousInfiniteDeposits);
            throw e;
        }
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
            helper.assertTrue(DepositDurabilityManager.setDepositBlockDurability(level, absolutePos, 2),
                    "Finite durability should be stored");

            DepositDurabilityManager.useDepositBlock(level, absolutePos, replacementState);
            CodecHelper.assertValueEqual(helper, level.getBlockState(absolutePos), depositState,
                    "deposit state after its first finite use");
            CodecHelper.assertValueEqual(helper,
                    DepositDurabilityManager.getDepositBlockDurability(level, absolutePos, false), 1L,
                    "durability after the first finite use");

            DepositDurabilityManager.useDepositBlock(level, absolutePos, replacementState);
            CodecHelper.assertValueEqual(helper, level.getBlockState(absolutePos), replacementState,
                    "deposit state after its final finite use");
            CodecHelper.assertValueEqual(helper,
                    DepositDurabilityManager.getDepositBlockDurability(level, absolutePos, false), -1L,
                    "durability entry after depletion");

            helper.setBlock(relativePos, depositState);
            helper.assertTrue(DepositDurabilityManager.setDepositBlockDurability(level, absolutePos, 3),
                    "Durability should be stored before normal removal");
            helper.setBlock(relativePos, Blocks.AIR.defaultBlockState());
            CodecHelper.assertValueEqual(helper,
                    DepositDurabilityManager.getDepositBlockDurability(level, absolutePos, false), -1L,
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
            helper.assertTrue(miner.behavior().getClaimedDepositBlocks().isEmpty(),
                    "Disassembled miner should release its exclusive claim");
            helper.assertTrue(miner.process() == null, "Disassembled miner should clear its mining process");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 10)
    public void legacySerializedMinerReclaimsAndContinuesMining(GameTestHelper helper) {
        var depositPos = new BlockPos(5, 2, 5);
        var absoluteDepositPos = helper.absolutePos(depositPos);
        var miner = MinerSetupBuilder.create(helper)
                .bearing(5, 5, 5)
                .part(Blocks.BARREL.defaultBlockState(), 5, 4, 5)
                .head(5, 3, 5)
                .deposit(depositPos.getX(), depositPos.getY(), depositPos.getZ())
                .place();
        var cobblestoneBeforeLoad = new AtomicInteger();

        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var legacyTag = createLegacyMiningBehaviourTag(absoluteDepositPos);
            cobblestoneBeforeLoad.set(miner.contraptionItemCount(Items.COBBLESTONE));

            helper.assertTrue(legacyTag.getCompound("claimer").get("claimed_blocks") instanceof ListTag,
                    "The legacy claim should use a list rather than the current long-array representation");

            behavior.setClaimedDepositBlocks(null);
            behavior.read(legacyTag, false);

            helper.assertTrue(behavior.getClaimedDepositBlocks() == null,
                    "Legacy disk deserialization should remain deferred until the next behavior tick");
            helper.assertTrue(miner.process() == null,
                    "Mining process reconstruction should wait until the legacy miner has reclaimed blocks");
        });

        helper.runAtTickTime(4, () -> {
            var expectedClaim = Set.of(absoluteDepositPos);
            CodecHelper.assertValueEqual(helper, getClaimedBlocks(helper, miner), expectedClaim,
                    "claim reconstructed after loading legacy miner data");
            assertProcessTargetsMatchClaim(helper, miner, expectedClaim);
            helper.assertTrue(miner.contraptionItemCount(Items.COBBLESTONE) > cobblestoneBeforeLoad.get(),
                    "Legacy miner should resume mining and store at least one new item");
            helper.succeed();
        });
    }

    @GameTest(template = "empty16x16", timeoutTicks = 15)
    public void localClaimStartsBeyondMineHeadTipAndUsesMinerLevel(GameTestHelper helper) {
        var miner = miner(helper, 5, 5, 5)
                .deposit(5, 2, 5)
                .place();

        miner.assemble(TEST_RPM);

        helper.runAtTickTime(2, () -> {
            var behavior = miner.behavior();
            var expectedStart = helper.absolutePos(miner.headPos()).relative(behavior.getOperatingDirection());
            CodecHelper.assertValueEqual(helper, behavior.getOperatingStart(), expectedStart,
                    "Local operating start should touch the mine-head tip");
            helper.assertTrue(behavior.getOperatingBoundingBox() != null,
                    "Assembled miner should have mining bounds");
            helper.assertTrue(behavior.getClaimedDepositBlocks() != null,
                    "Assembled miner should initialize its local claim");
            helper.assertTrue(miner.process().level == helper.getLevel(),
                    "Mining process should use the miner's level");
            helper.succeed();
        });
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
        return claimedBlocks;
    }

    private static BoundingBox getOperatingArea(GameTestHelper helper, MinerSetup miner) {
        var area = miner.behavior().getOperatingBoundingBox();
        helper.assertTrue(area != null, "Assembled miner should have an operating area");
        return area;
    }

    private static Set<BlockPos> unionClaims(GameTestHelper helper, MinerSetup... miners) {
        var union = new HashSet<BlockPos>();
        for (var miner : miners) {
            union.addAll(getClaimedBlocks(helper, miner));
        }
        return Set.copyOf(union);
    }

    private static void assertClaimsAreDisjoint(GameTestHelper helper, MinerSetup... miners) {
        for (int i = 0; i < miners.length; i++) {
            var firstClaim = getClaimedBlocks(helper, miners[i]);
            for (int j = i + 1; j < miners.length; j++) {
                helper.assertTrue(disjoint(firstClaim, getClaimedBlocks(helper, miners[j])),
                        "Miner claims must be pairwise disjoint");
            }
        }
    }

    private static void assertRegisteredClaimers(
            GameTestHelper helper, MinerSetup referenceMiner, MinerSetup... expectedMiners
    ) {
        var expectedClaimers = new HashSet<IDepositBlockClaimer>();
        for (var miner : expectedMiners) {
            expectedClaimers.add(miner.behavior());
        }

        var actualClaimers = DepositClaimerInstanceHolder.getInstancesWithIntersectingArea(
                helper.getLevel(), getOperatingArea(helper, referenceMiner));
        CodecHelper.assertValueEqual(helper, actualClaimers, Set.copyOf(expectedClaimers),
                "deposit claimers registered in the instance holder");
    }

    private static CompoundTag createLegacyMiningBehaviourTag(BlockPos claimedDepositBlock) {
        var claimedBlocks = new ListTag();
        claimedBlocks.add(LongTag.valueOf(claimedDepositBlock.asLong()));
        var claimer = new CompoundTag();
        claimer.put("claimed_blocks", claimedBlocks);

        var depositBlockId = ForgeRegistries.BLOCKS.getKey(RNSDeposits.DEPLETED_DEPOSIT.get());
        if (depositBlockId == null) throw new IllegalStateException("Depleted deposit block is not registered");
        var innerProcess = new CompoundTag();
        innerProcess.putString("deposit_block", depositBlockId.toString());
        innerProcess.putLong("remaining_uses", 0);
        innerProcess.putInt("progress", LEGACY_MINING_PROGRESS);
        var innerProcesses = new ListTag();
        innerProcesses.add(innerProcess);
        var process = new CompoundTag();
        process.put("inner_processes", innerProcesses);

        var behavior = new CompoundTag();
        behavior.put("claimer", claimer);
        behavior.put("process", process);
        return behavior;
    }

    private static void assertProcessTargetsMatchClaim(
            GameTestHelper helper, MinerSetup miner, Set<BlockPos> expectedClaim
    ) {
        var claimedBlocks = miner.behavior().getClaimedDepositBlocks();
        helper.assertTrue(claimedBlocks != null, "Assembled miner should have initialized its claim");
        CodecHelper.assertValueEqual(helper, claimedBlocks, expectedClaim, "miner claim");

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

    private static Set<BlockPos> absolutePlane(
            GameTestHelper helper, int minX, int y, int minZ, int maxX, int maxZ
    ) {
        var positions = new HashSet<BlockPos>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                positions.add(helper.absolutePos(new BlockPos(x, y, z)));
            }
        }
        return Set.copyOf(positions);
    }

    private static void assertDurabilityWithinRecipeSpread(
            GameTestHelper helper, long actual, long base, DepositDurability durability, String description
    ) {
        long spread = (long) (durability.randomSpread() * base);
        long minimum = Math.max(durability.edge(), base - spread);
        long maximum = Math.min(durability.core(), base + spread);
        helper.assertTrue(actual >= minimum && actual <= maximum,
                "%s should be between %s and %s, but was %s".formatted(description, minimum, maximum, actual));
    }

    private static Field getDepositIndexField(String name) {
        try {
            var field = DepositIndex.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access DepositIndex field: " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static OptionalLong getRawDepositDurability(GameTestHelper helper, BlockPos pos) {
        try {
            var index = IDepositIndex.get(helper.getLevel());
            helper.assertTrue(index != null, "Level should expose a deposit index");
            var durabilities = (Object2LongOpenHashMap<BlockPos>) DEPOSIT_DURABILITIES_FIELD.get(index);
            return durabilities.containsKey(pos) ? OptionalLong.of(durabilities.getLong(pos)) : OptionalLong.empty();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access deposit durabilities", e);
        }
    }

    private static boolean disjoint(Set<BlockPos> first, Set<BlockPos> second) {
        return first.stream().noneMatch(second::contains);
    }

    private static class FixedAreaClaimer implements IDepositBlockClaimer {
        private static final OperatingDimensions CLAIMING_AREA = new OperatingDimensions(1, 2);

        private final Level level;
        private final BlockPos anchor;
        private @Nullable Set<BlockPos> claimedBlocks;

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
        public OperatingDimensions getOperatingDimensions() {
            return CLAIMING_AREA;
        }

        @Override
        public BlockPos getOperatingStart() {
            return anchor.relative(getOperatingDirection());
        }

        @Override
        public Direction getOperatingDirection() {
            return Direction.DOWN;
        }

        @Override
        public @Nullable Set<BlockPos> getClaimedDepositBlocks() {
            return claimedBlocks;
        }

        @Override
        public void setClaimedDepositBlocks(@Nullable Set<BlockPos> claimedBlocks) {
            this.claimedBlocks = claimedBlocks;
        }

        @Override
        public void claimDepositBlocks() {
        }
    }
}

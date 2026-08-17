package com.bmaster.createrns.compat.aeronautics;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.content.deposit.claiming.DepositClaimerInstanceHolder;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapter.OperatingSublevel;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SuppressWarnings("DataFlowIssue")
@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class MinerClaimingPhysicsGameTest {
    private static final int TEST_RPM = 128;
    private static final int TEST_MINING_RADIUS = 2;
    // GameTest helper coordinates are relative to the structure block one block below the template volume.
    private static final int TEMPLATE_Y_OFFSET = 1;
    private static final int DEPOSIT_MIN_X = 4;
    private static final int ATTACHED_DEPOSIT_MIN_X = 6;
    private static final int DEPOSIT_MAX_X = 8;
    private static final int DEPOSIT_Y = TEMPLATE_Y_OFFSET;
    private static final int DEPOSIT_MIN_Z = 5;
    private static final int DEPOSIT_MAX_Z = 6;
    private static final int ATTACHED_DEPOSIT_COUNT =
            (DEPOSIT_MAX_X - ATTACHED_DEPOSIT_MIN_X + 1) * (DEPOSIT_MAX_Z - DEPOSIT_MIN_Z + 1);
    private static final int CLAIMER_SEARCH_DISTANCE = 8;
    private static final BlockPos FIRST_BEARING = new BlockPos(4, 3 + TEMPLATE_Y_OFFSET, 6);
    private static final BlockPos SECOND_BEARING = new BlockPos(8, 3 + TEMPLATE_Y_OFFSET, 6);
    private static final BlockPos FIRST_ASSEMBLER = new BlockPos(4, 4 + TEMPLATE_Y_OFFSET, 5);
    private static final BlockPos SECOND_ASSEMBLER = new BlockPos(8, 4 + TEMPLATE_Y_OFFSET, 5);
    private static final BlockPos FIRST_GLUE_MIN = new BlockPos(4, 1 + TEMPLATE_Y_OFFSET, 5);
    private static final BlockPos FIRST_GLUE_MAX = new BlockPos(4, 4 + TEMPLATE_Y_OFFSET, 6);
    private static final BlockPos SECOND_GLUE_MIN = new BlockPos(6, TEMPLATE_Y_OFFSET, 5);
    private static final BlockPos SECOND_GLUE_MAX = new BlockPos(8, 3 + TEMPLATE_Y_OFFSET, 6);
    private static final BlockPos SECOND_TOP_GLUE_MIN = new BlockPos(8, 3 + TEMPLATE_Y_OFFSET, 5);
    private static final BlockPos SECOND_TOP_GLUE_MAX = new BlockPos(8, 4 + TEMPLATE_Y_OFFSET, 6);
    private static final String PHYSICS_ASSEMBLER_BLOCK_ENTITY =
            "dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity";
    private static final String HONEY_GLUE_ENTITY =
            "dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity";
    private static final String PHYSICS_CONFIG_BATCH = "miner_claiming_physics_config";

    private static int previousMiningRadius;

    @BeforeBatch(batch = PHYSICS_CONFIG_BATCH)
    public static void configurePhysicsBatch(ServerLevel level) {
        previousMiningRadius = ServerConfig.MINING_RADIUS.get();
        ServerConfig.MINING_RADIUS.set(TEST_MINING_RADIUS);
    }

    @AfterBatch(batch = PHYSICS_CONFIG_BATCH)
    public static void restorePhysicsBatch(ServerLevel level) {
        ServerConfig.MINING_RADIUS.set(previousMiningRadius);
    }

    @GameTest(template = "simulated16x16", batch = PHYSICS_CONFIG_BATCH, timeoutTicks = 100)
    public void minerMovedWithoutDepositsCrossClaimsMainSublevelRemainder(GameTestHelper helper) {
        assembleTemplateMinerWithPhysics(helper, false);
    }

    @GameTest(template = "simulated16x16", batch = PHYSICS_CONFIG_BATCH, timeoutTicks = 100)
    public void minerMovedWithDepositsKeepsLocalPhysicsSublevelClaim(GameTestHelper helper) {
        assembleTemplateMinerWithPhysics(helper, true);
    }

    private static void assembleTemplateMinerWithPhysics(GameTestHelper helper, boolean moveGluedDeposits) {
        helper.assertValueEqual(ServerConfig.MINING_RADIUS.get(), TEST_MINING_RADIUS,
                "mining radius required by the simulated miner layout");

        placeHoneyGlue(helper, FIRST_GLUE_MIN, FIRST_GLUE_MAX);
        placeHoneyGlue(helper, SECOND_GLUE_MIN, SECOND_GLUE_MAX);
        placeHoneyGlue(helper, SECOND_TOP_GLUE_MIN, SECOND_TOP_GLUE_MAX);

        var movedBearingPos = moveGluedDeposits ? SECOND_BEARING : FIRST_BEARING;
        var remainingBearingPos = moveGluedDeposits ? FIRST_BEARING : SECOND_BEARING;
        var assemblerPos = moveGluedDeposits ? SECOND_ASSEMBLER : FIRST_ASSEMBLER;
        var movedMinerName = moveGluedDeposits ? "Miner B" : "Miner A";
        var remainingMinerName = moveGluedDeposits ? "Miner A" : "Miner B";
        var movedInitialClaim = moveGluedDeposits
                ? absolutePlane(helper, ATTACHED_DEPOSIT_MIN_X, DEPOSIT_Y,
                        DEPOSIT_MIN_Z, DEPOSIT_MAX_X, DEPOSIT_MAX_Z)
                : absolutePlane(helper, DEPOSIT_MIN_X, DEPOSIT_Y,
                        DEPOSIT_MIN_Z, ATTACHED_DEPOSIT_MIN_X, DEPOSIT_MAX_Z);
        var remainingInitialClaim = moveGluedDeposits
                ? absolutePlane(helper, DEPOSIT_MIN_X, DEPOSIT_Y,
                        DEPOSIT_MIN_Z, ATTACHED_DEPOSIT_MIN_X - 1, DEPOSIT_MAX_Z)
                : absolutePlane(helper, ATTACHED_DEPOSIT_MIN_X + 1, DEPOSIT_Y,
                        DEPOSIT_MIN_Z, DEPOSIT_MAX_X, DEPOSIT_MAX_Z);
        var allDeposits = absolutePlane(helper, DEPOSIT_MIN_X, DEPOSIT_Y,
                DEPOSIT_MIN_Z, DEPOSIT_MAX_X, DEPOSIT_MAX_Z);
        var movedBearing = startTemplateMiner(helper, movedBearingPos);
        var remainingBearing = new AtomicReference<MinerBearingBlockEntity>();

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(movedBearing.isRunning(), movedMinerName + " should assemble first");
                    assertClaimerClaim(helper, movedBearing.miningBehaviour, movedInitialClaim,
                            movedMinerName + " initial claim");
                })
                .thenExecute(() -> remainingBearing.set(startTemplateMiner(helper, remainingBearingPos)))
                .thenWaitUntil(() -> {
                    var remaining = remainingBearing.get();
                    helper.assertTrue(remaining != null && remaining.isRunning(),
                            remainingMinerName + " should assemble second");
                    var movedClaim = assertClaimerClaim(helper, movedBearing.miningBehaviour, movedInitialClaim,
                            movedMinerName + " claim after both miners assemble");
                    var remainingClaim = assertClaimerClaim(helper, remaining.miningBehaviour, remainingInitialClaim,
                            remainingMinerName + " initial claim");
                    helper.assertTrue(disjoint(movedClaim, remainingClaim),
                            "Initial miner claims should not overlap");
                    helper.assertValueEqual(union(movedClaim, remainingClaim), allDeposits,
                            "initial claims across the entire deposit area");
                })
                .thenExecute(() -> pullPhysicsAssemblerLever(helper, assemblerPos, moveGluedDeposits))
                .thenWaitUntil(() -> assertTemplatePhysicsAssembly(
                        helper, movedBearingPos, remainingBearingPos, assemblerPos, moveGluedDeposits))
                .thenWaitUntil(() -> assertTemplateClaimsAfterPhysicsAssembly(
                        helper, remainingBearing.get(), movedBearingPos, moveGluedDeposits))
                .thenSucceed();
    }

    private static void placeHoneyGlue(GameTestHelper helper, BlockPos min, BlockPos max) {
        var absoluteMin = helper.absolutePos(min);
        var absoluteMax = helper.absolutePos(max);
        var bounds = AABB.encapsulatingFullBlocks(absoluteMin, absoluteMax);

        try {
            var glueType = Class.forName(HONEY_GLUE_ENTITY).asSubclass(Entity.class);
            var glue = glueType.getConstructor(Level.class, AABB.class).newInstance(helper.getLevel(), bounds);
            helper.assertTrue(helper.getLevel().addFreshEntity(glue),
                    "Could not add honey glue spanning " + absoluteMin + " to " + absoluteMax);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not create honey glue spanning " + absoluteMin + " to " + absoluteMax,
                    exception);
        }
    }

    private static MinerBearingBlockEntity startTemplateMiner(GameTestHelper helper, BlockPos bearingPos) {
        var absoluteBearingPos = helper.absolutePos(bearingPos);
        var blockEntity = helper.getLevel().getBlockEntity(absoluteBearingPos);
        helper.assertTrue(blockEntity instanceof MinerBearingBlockEntity,
                "Expected miner bearing at " + absoluteBearingPos);

        var motorPos = bearingPos.above();
        helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.DOWN));
        var motorBlockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(motorPos));
        helper.assertTrue(motorBlockEntity instanceof CreativeMotorBlockEntity,
                "Expected creative motor above miner bearing at " + absoluteBearingPos);
        ((CreativeMotorBlockEntity) motorBlockEntity).generatedSpeed.setValue(-TEST_RPM);

        return (MinerBearingBlockEntity) blockEntity;
    }

    private static void pullPhysicsAssemblerLever(
            GameTestHelper helper, BlockPos assemblerPos, boolean allowDepositMovement
    ) {
        var absoluteAssemblerPos = helper.absolutePos(assemblerPos);
        var blockEntity = helper.getLevel().getBlockEntity(absoluteAssemblerPos);
        helper.assertTrue(blockEntity != null && blockEntity.getClass().getName().equals(PHYSICS_ASSEMBLER_BLOCK_ENTITY),
                "Expected physics assembler at " + absoluteAssemblerPos);

        boolean previousMovableDeposits = ServerConfig.MOVABLE_DEPOSITS.get();
        if (allowDepositMovement) ServerConfig.MOVABLE_DEPOSITS.set(true);
        try {
            // This is the server entry point called by Simulated's AssemblePacket after the client lever gesture.
            blockEntity.getClass().getMethod("assembleOrDisassemble").invoke(blockEntity);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not operate physics assembler at " + absoluteAssemblerPos, exception);
        } finally {
            ServerConfig.MOVABLE_DEPOSITS.set(previousMovableDeposits);
        }
    }

    private static void assertTemplatePhysicsAssembly(
            GameTestHelper helper, BlockPos movedBearingPos, BlockPos remainingBearingPos,
            BlockPos assemblerPos, boolean movedGluedDeposits
    ) {
        var level = helper.getLevel();
        helper.assertTrue(level.getBlockState(helper.absolutePos(movedBearingPos)).isAir(),
                "Selected miner bearing should move into a physics sublevel");
        helper.assertTrue(level.getBlockState(helper.absolutePos(movedBearingPos.above())).isAir(),
                "Selected miner motor should move into a physics sublevel");
        helper.assertTrue(level.getBlockState(helper.absolutePos(assemblerPos)).isAir(),
                "Selected physics assembler should move into a physics sublevel");

        var remainingBearing = level.getBlockEntity(helper.absolutePos(remainingBearingPos));
        helper.assertTrue(remainingBearing instanceof MinerBearingBlockEntity,
                "The other miner bearing should remain in the main level");
        helper.assertTrue(((MinerBearingBlockEntity) remainingBearing).isRunning(),
                "The other miner should remain running");

        for (int x = DEPOSIT_MIN_X; x <= DEPOSIT_MAX_X; x++) {
            for (int z = DEPOSIT_MIN_Z; z <= DEPOSIT_MAX_Z; z++) {
                var depositPos = helper.absolutePos(new BlockPos(x, DEPOSIT_Y, z));
                boolean shouldMove = movedGluedDeposits && x >= ATTACHED_DEPOSIT_MIN_X;
                if (shouldMove) {
                    helper.assertTrue(level.getBlockState(depositPos).isAir(),
                            "Glued deposit should move into the physics sublevel at " + depositPos);
                } else {
                    helper.assertTrue(level.getBlockState(depositPos).is(RNSDeposits.IRON_DEPOSIT.get()),
                            "Unglued deposit should remain in the main level at " + depositPos);
                }
            }
        }
    }

    private static void assertTemplateClaimsAfterPhysicsAssembly(
            GameTestHelper helper, MinerBearingBlockEntity remainingBearing,
            BlockPos movedBearingPos, boolean movedGluedDeposits
    ) {
        helper.assertTrue(remainingBearing != null && remainingBearing.isRunning(),
                "The miner remaining in the main level should still be running");

        var level = helper.getLevel();
        var movedClaimer = getPhysicsSublevelClaimer(helper, movedBearingPos);
        var movedSublevel = OperatingSublevel.of(level, movedClaimer.getBlockPos());
        helper.assertFalse(movedSublevel.identity().equals(OperatingSublevel.MAIN_ID),
                "The moved miner should belong to a physics sublevel");

        if (!movedGluedDeposits) {
            var expectedMainClaim = absolutePlane(helper, ATTACHED_DEPOSIT_MIN_X, DEPOSIT_Y,
                    DEPOSIT_MIN_Z, DEPOSIT_MAX_X, DEPOSIT_MAX_Z);
            var expectedCrossSublevelClaim = absolutePlane(helper, DEPOSIT_MIN_X, DEPOSIT_Y,
                    DEPOSIT_MIN_Z, ATTACHED_DEPOSIT_MIN_X - 1, DEPOSIT_MAX_Z);
            var expectedVein = absolutePlane(helper, DEPOSIT_MIN_X, DEPOSIT_Y,
                    DEPOSIT_MIN_Z, DEPOSIT_MAX_X, DEPOSIT_MAX_Z);

            var mainClaim = assertClaimerClaim(helper, remainingBearing.miningBehaviour,
                    expectedMainClaim, "Miner B main-sublevel claim");
            var crossSublevelClaim = assertClaimerClaim(helper, movedClaimer,
                    expectedCrossSublevelClaim, "Miner A cross-sublevel claim");
            helper.assertFalse(remainingBearing.miningBehaviour.isOperatingCrossSublevel(),
                    "Miner B should operate locally in the main sublevel");
            helper.assertTrue(movedClaimer.isOperatingCrossSublevel(),
                    "Miner A should operate across sublevels");
            helper.assertTrue(disjoint(mainClaim, crossSublevelClaim),
                    "Final miner claims should not overlap");
            helper.assertValueEqual(union(mainClaim, crossSublevelClaim), expectedVein,
                    "final claims across the main-level deposit vein");
            return;
        }

        var expectedMainClaim = absolutePlane(helper, DEPOSIT_MIN_X, DEPOSIT_Y,
                DEPOSIT_MIN_Z, ATTACHED_DEPOSIT_MIN_X - 1, DEPOSIT_MAX_Z);
        var mainClaim = assertClaimerClaim(helper, remainingBearing.miningBehaviour,
                expectedMainClaim, "Miner A main-sublevel claim");
        var movedClaim = movedClaimer.getClaimedDepositBlocks();
        helper.assertTrue(movedClaim != null, "Miner B should initialize its physics-sublevel claim");
        helper.assertValueEqual(movedClaim.size(), ATTACHED_DEPOSIT_COUNT,
                "number of deposits claimed by Miner B in the physics sublevel");
        helper.assertFalse(remainingBearing.miningBehaviour.isOperatingCrossSublevel(),
                "Miner A should operate locally in the main sublevel");
        helper.assertFalse(movedClaimer.isOperatingCrossSublevel(),
                "Miner B should operate locally in its physics sublevel");
        helper.assertTrue(disjoint(mainClaim, movedClaim), "Final miner claims should not overlap");

        for (var claimedPos : movedClaim) {
            helper.assertTrue(level.getBlockState(claimedPos).is(RNSDeposits.IRON_DEPOSIT.get()),
                    "Miner B should only claim moved deposits, but claimed " + claimedPos);
            helper.assertValueEqual(OperatingSublevel.of(level, claimedPos), movedSublevel,
                    "sublevel of deposit claimed by Miner B");
        }
    }

    private static IDepositBlockClaimer getPhysicsSublevelClaimer(
            GameTestHelper helper, BlockPos originalBearingPos
    ) {
        var level = helper.getLevel();
        var referencePos = helper.absolutePos(originalBearingPos);
        var claimers = DepositClaimerInstanceHolder.getInstancesWithinManhattanDistance(
                        level, referencePos, CLAIMER_SEARCH_DISTANCE).stream()
                .filter(claimer -> {
                    var sublevel = claimer.getSublevel();
                    return sublevel != null && !sublevel.identity().equals(OperatingSublevel.MAIN_ID);
                })
                .collect(Collectors.toUnmodifiableSet());
        helper.assertValueEqual(claimers.size(), 1, "number of nearby miners in a physics sublevel");
        return claimers.iterator().next();
    }

    private static Set<BlockPos> assertClaimerClaim(
            GameTestHelper helper, IDepositBlockClaimer claimer, Set<BlockPos> expected, String description
    ) {
        var claimedBlocks = claimer.getClaimedDepositBlocks();
        helper.assertTrue(claimedBlocks != null, description + " should be initialized");
        helper.assertValueEqual(claimedBlocks, expected, description);
        return claimedBlocks;
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

    @SafeVarargs
    private static Set<BlockPos> union(Set<BlockPos>... sets) {
        var union = new HashSet<BlockPos>();
        for (var set : sets) union.addAll(set);
        return Set.copyOf(union);
    }

    private static boolean disjoint(Set<BlockPos> first, Set<BlockPos> second) {
        return first.stream().noneMatch(second::contains);
    }
}

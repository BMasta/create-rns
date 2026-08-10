package com.bmaster.createrns.compat.sable;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSDeposits;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.bmaster.createrns.infrastructure.ServerConfig;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;
import java.util.OptionalLong;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class DepositDurabilityMovementGameTest {
    private static final BlockPos SOURCE_POS = new BlockPos(4, 2, 4);
    private static final BlockPos DESTINATION_POS = new BlockPos(8, 2, 8);
    private static final BlockPos OTHER_SOURCE_POS = new BlockPos(4, 2, 8);
    private static final BlockPos OTHER_DESTINATION_POS = new BlockPos(8, 2, 4);
    private static final long SOURCE_DURABILITY = 37;
    private static final long DESTINATION_DURABILITY = 91;
    private static final long OTHER_SOURCE_DURABILITY = 53;

    @GameTest(template = "empty16x16")
    public void finiteDurabilityMovesExactlyWithinLevel(GameTestHelper helper) {
        withFiniteDeposits(helper, () -> {
            var level = helper.getLevel();
            var sourcePos = helper.absolutePos(SOURCE_POS);
            var destinationPos = helper.absolutePos(DESTINATION_POS);
            var state = RNSDeposits.IRON_DEPOSIT.getDefaultState();
            var listener = listener();

            helper.setBlock(SOURCE_POS, state);
            helper.assertTrue(DepositDurabilityManager.set(level, sourcePos, SOURCE_DURABILITY),
                    "Source durability should be stored");

            listener.beforeMove(level, level, state, sourcePos, destinationPos);
            helper.setBlock(DESTINATION_POS, state);
            listener.afterMove(level, level, state, sourcePos, destinationPos);
            helper.setBlock(SOURCE_POS, Blocks.AIR.defaultBlockState());

            helper.assertValueEqual(stored(level, sourcePos), OptionalLong.empty(),
                    "source durability after movement");
            helper.assertValueEqual(stored(level, destinationPos), OptionalLong.of(SOURCE_DURABILITY),
                    "destination durability after movement");
        });
    }

    @GameTest(template = "empty16x16")
    public void sourceAbsenceReplacesConflictingDestinationDurability(GameTestHelper helper) {
        withFiniteDeposits(helper, () -> {
            var level = helper.getLevel();
            var sourcePos = helper.absolutePos(SOURCE_POS);
            var destinationPos = helper.absolutePos(DESTINATION_POS);
            var state = RNSDeposits.IRON_DEPOSIT.getDefaultState();
            var listener = listener();

            helper.setBlock(SOURCE_POS, state);
            DepositDurabilityManager.setRaw(
                    level, destinationPos, OptionalLong.of(DESTINATION_DURABILITY));

            listener.beforeMove(level, level, state, sourcePos, destinationPos);
            helper.setBlock(DESTINATION_POS, state);
            listener.afterMove(level, level, state, sourcePos, destinationPos);
            helper.setBlock(SOURCE_POS, Blocks.AIR.defaultBlockState());

            helper.assertValueEqual(stored(level, destinationPos), OptionalLong.empty(),
                    "An uninitialized source should remove conflicting destination durability");
        });
    }

    @GameTest(template = "empty16x16")
    public void finiteDurabilityMovesBetweenLevelData(GameTestHelper helper) {
        withFiniteDeposits(helper, () -> {
            var sourceLevel = helper.getLevel();
            var destinationLevel = Objects.requireNonNull(sourceLevel.getServer().getLevel(Level.NETHER));
            var sourcePos = helper.absolutePos(SOURCE_POS);
            var destinationPos = helper.absolutePos(DESTINATION_POS);
            var state = RNSDeposits.IRON_DEPOSIT.getDefaultState();
            var listener = listener();

            helper.setBlock(SOURCE_POS, state);
            helper.assertTrue(DepositDurabilityManager.set(
                    sourceLevel, sourcePos, SOURCE_DURABILITY), "Source durability should be stored");

            listener.beforeMove(sourceLevel, destinationLevel, state, sourcePos, destinationPos);
            listener.afterMove(sourceLevel, destinationLevel, state, sourcePos, destinationPos);
            helper.setBlock(SOURCE_POS, Blocks.AIR.defaultBlockState());

            helper.assertValueEqual(stored(sourceLevel, sourcePos), OptionalLong.empty(),
                    "source durability after cross-level movement");
            helper.assertValueEqual(stored(destinationLevel, destinationPos), OptionalLong.of(SOURCE_DURABILITY),
                    "destination durability after cross-level movement");
            DepositDurabilityManager.remove(destinationLevel, destinationPos);
        });
    }

    @GameTest(template = "empty16x16")
    public void unrelatedAfterMoveCannotConsumeNewCapture(GameTestHelper helper) {
        withFiniteDeposits(helper, () -> {
            var level = helper.getLevel();
            var sourcePos = helper.absolutePos(SOURCE_POS);
            var destinationPos = helper.absolutePos(DESTINATION_POS);
            var otherSourcePos = helper.absolutePos(OTHER_SOURCE_POS);
            var otherDestinationPos = helper.absolutePos(OTHER_DESTINATION_POS);
            var state = RNSDeposits.IRON_DEPOSIT.getDefaultState();
            var listener = listener();

            helper.setBlock(SOURCE_POS, state);
            helper.setBlock(OTHER_SOURCE_POS, state);
            helper.assertTrue(DepositDurabilityManager.set(level, sourcePos, SOURCE_DURABILITY),
                    "First source durability should be stored");
            helper.assertTrue(DepositDurabilityManager.set(
                    level, otherSourcePos, OTHER_SOURCE_DURABILITY), "Second source durability should be stored");

            listener.beforeMove(level, level, state, sourcePos, destinationPos);
            listener.beforeMove(level, level, state, otherSourcePos, otherDestinationPos);
            listener.afterMove(level, level, state, sourcePos, destinationPos);
            listener.afterMove(level, level, state, otherSourcePos, otherDestinationPos);
            helper.setBlock(SOURCE_POS, Blocks.AIR.defaultBlockState());
            helper.setBlock(OTHER_SOURCE_POS, Blocks.AIR.defaultBlockState());

            helper.assertValueEqual(stored(level, destinationPos), OptionalLong.empty(),
                    "An unrelated afterMove must not receive captured durability");
            helper.assertValueEqual(stored(level, otherDestinationPos), OptionalLong.of(OTHER_SOURCE_DURABILITY),
                    "The matching afterMove should still receive captured durability");
        });
    }

    private static BlockSubLevelAssemblyListener listener() {
        return (BlockSubLevelAssemblyListener) RNSDeposits.IRON_DEPOSIT.get();
    }

    private static OptionalLong stored(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        return DepositDurabilityManager.getRaw(level, pos);
    }

    private static void withFiniteDeposits(GameTestHelper helper, Runnable test) {
        boolean previousInfiniteDeposits = ServerConfig.INFINITE_DEPOSITS.get();
        ServerConfig.INFINITE_DEPOSITS.set(false);
        try {
            test.run();
        } finally {
            ServerConfig.INFINITE_DEPOSITS.set(previousInfiniteDeposits);
        }
        helper.succeed();
    }
}

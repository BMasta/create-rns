package com.bmaster.createrns.infrastructure.gametest;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.mining.MiningEntityItemHandler;
import com.bmaster.createrns.mining.miner.impl.MinerMk2BlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.items.IItemHandler;

@GameTestHolder(CreateRNS.MOD_ID)
public final class MinerTest {

    @GameTest(template = "double")
    public static void test1(GameTestHelper helper) {
        var l = helper.getLevel();
        var access = l.registryAccess();
        var motorPos = new BlockPos(2, 7, 1);
        var miner1Pos = new BlockPos(1, 6, 1);
        var miner1AbovePos = new BlockPos(1, 7, 1);

        CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) helper.getBlockEntity(motorPos);
        helper.setBlock(miner1Pos, RNSContent.MINER_MK2_BLOCK.get());

        // Validate nbt
        helper.runAtTickTime(1, () -> {
            MinerMk2BlockEntity miner1 = (MinerMk2BlockEntity) helper.getBlockEntity(miner1Pos);
            assert miner1 != null;
            var tagBefore = miner1.saveWithFullMetadata();
            miner1.writeSafe(tagBefore);
            var tagAfter = miner1.saveWithFullMetadata();
            helper.assertTrue(tagBefore.equals(tagAfter),
                    "Serialize->deserialize->serialize of mining BE changed the nbt after 1 tick");
            helper.assertTrue(tagAfter.contains("Inventory"), "Inventory assert failed");
            helper.assertTrue(tagAfter.contains("ReservedDepositBlocks"), "ReservedDepositBlocks assert failed");
            helper.assertTrue(tagAfter.contains("MiningProgress"), "MiningProgress assert failed");
            var mpTag = tagAfter.getCompound("MiningProgress");
            helper.assertTrue(mpTag.contains("PerYieldProgress"),
                    "MiningProgress.PerYieldProgress assert failed");
            var pyTag = mpTag.getList("PerYieldProgress", Tag.TAG_COMPOUND);
            helper.assertTrue(pyTag.size() == 1,
                    "MiningProgress.PerYieldProgress does not have exactly one element");
            CompoundTag pyFirst = pyTag.getCompound(0);
            helper.assertTrue(pyFirst.contains("MaxProgress"),
                    "MiningProgress.PerYieldProgress.MaxProgress assert failed");
            helper.assertTrue(pyFirst.contains("Progress"),
                    "MiningProgress.PerYieldProgress.Progress assert failed");
            helper.assertTrue(pyFirst.contains("Yield"),
                    "MiningProgress.PerYieldProgress.Yield assert failed");
        });

        // Validate nbt and game state after item is mined
        helper.runAtTickTime(36, () -> {
            MinerMk2BlockEntity miner1 = (MinerMk2BlockEntity) helper.getBlockEntity(miner1Pos);
            assert miner1 != null;
            var tagBefore = miner1.saveWithFullMetadata();
            miner1.writeSafe(tagBefore);
            var tagAfter = miner1.saveWithFullMetadata();
            helper.assertTrue(tagBefore.equals(tagAfter),
                    "Serialize->deserialize->serialize of mining BE changed the nbt after 36 ticks");

            for (var d : Direction.values()) {
                helper.assertTrue(miner1.getCapability(ForgeCapabilities.ITEM_HANDLER, d).resolve().isPresent(),
                    "Could not get mining BE inventory");
            }

            var inv = (MiningEntityItemHandler) miner1.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
            helper.assertTrue(inv != null, "Could not get mining BE inventory without direction");
            assert inv != null; // IDE complains otherwise

            var extractedItem = inv.extractFirstAvailableItem(true);
            helper.assertTrue(extractedItem.is(RNSContent.IMPURE_IRON_ORE.get()),
                    "Mining BE expected to mine %s, but mined %s instead"
                            .formatted(RNSContent.IMPURE_IRON_ORE.get(), extractedItem));
            helper.assertTrue(extractedItem.getCount() == 1,
                    "Mining BE expected to mine 1 item, but mined %d instead".formatted(extractedItem.getCount()));

            helper.setBlock(miner1AbovePos, Blocks.BARREL.defaultBlockState());
        });

        // Validate mined item is inserted into a container above
        helper.runAtTickTime(38, () -> {
            MinerMk2BlockEntity miner1 = (MinerMk2BlockEntity) helper.getBlockEntity(miner1Pos);
            var barrel = helper.getBlockEntity(miner1AbovePos);
            assert miner1 != null && barrel != null;
            IItemHandler barrelInv = barrel.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).resolve().orElse(null);
            var inv = (MiningEntityItemHandler) miner1.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
            helper.assertTrue(inv != null, "Could not get mining BE inventory without direction");
            assert ((inv != null) && (barrelInv != null));
            var extractedItem = inv.extractFirstAvailableItem(true);

            helper.assertTrue(extractedItem.is(Items.AIR),
                    "Mining BE expected to eject item to barrel, but still has at least 1 %s"
                            .formatted(RNSContent.IMPURE_IRON_ORE.get()));

            var barrelItem = barrelInv.getStackInSlot(0);
            helper.assertTrue(barrelItem.is(RNSContent.IMPURE_IRON_ORE.get()),
                    "Barrel expected to have %s in first slot, but has %s instead"
                            .formatted(RNSContent.IMPURE_IRON_ORE.get(), barrelItem));
            helper.assertTrue(barrelItem.getCount() == 1,
                    "Barrel expected to have 1 item in first slot, but has %d instead".formatted(barrelItem.getCount()));
            helper.succeed();
        });
    }
}

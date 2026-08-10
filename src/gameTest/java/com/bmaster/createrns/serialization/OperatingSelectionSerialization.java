package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.operating.space.OperatingSpaceAdapter.OperatingSpace;
import com.bmaster.createrns.content.deposit.operating.OperatingSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class OperatingSelectionSerialization {
    @GameTest(template = "empty16x16")
    public void operatingSelectionRoundTripsModeSpaceAndPositions(GameTestHelper helper) {
        var original = new OperatingSelection(true, new OperatingSpace(Level.OVERWORLD, "test_space"),
                Set.of(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6)));
        var restored = OperatingSelection.fromNBT(OperatingSelection.toNBT(original));

        helper.assertTrue(restored != null, "Serialized operating selection should deserialize");
        helper.assertValueEqual(restored.remote, original.remote, "remote operating mode");
        helper.assertValueEqual(restored.space, original.space, "operating space");
        helper.assertValueEqual(restored.positions, original.positions, "active deposit positions");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void incompleteOperatingSelectionDoesNotDeserialize(GameTestHelper helper) {
        var incomplete = new CompoundTag();
        incomplete.putBoolean("remote", false);
        incomplete.putString("space_dim", Level.OVERWORLD.location().toString());
        incomplete.putString("space_id", OperatingSpace.MAIN_SPACE);

        helper.assertTrue(OperatingSelection.fromNBT(incomplete) == null,
                "Operating selection without positions should not deserialize");
        helper.assertTrue(OperatingSelection.fromNBT(new CompoundTag()) == null,
                "Empty operating selection should not deserialize");
        helper.succeed();
    }
}

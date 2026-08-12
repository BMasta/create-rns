package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.operating.sublevel.OperatingSublevelAdapter.OperatingSublevel;
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
        var original = new OperatingSelection(true, new OperatingSublevel(Level.OVERWORLD, "test_space"),
                Set.of(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6)));
        var restored = OperatingSelection.fromNBT(OperatingSelection.toNBT(original));

        helper.assertTrue(restored != null, "Serialized operating selection should deserialize");
        helper.assertValueEqual(restored.crossSublevel, original.crossSublevel, "cross-sublevel operating mode");
        helper.assertValueEqual(restored.sublevel, original.sublevel, "operating sublevel");
        helper.assertValueEqual(restored.positions, original.positions, "active deposit positions");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void incompleteOperatingSelectionDoesNotDeserialize(GameTestHelper helper) {
        var incomplete = new CompoundTag();
        incomplete.putString("dimension", Level.OVERWORLD.location().toString());
        incomplete.putBoolean("cross_sublevel", false);
        incomplete.putString("sublevel_id", OperatingSublevel.MAIN_ID);

        helper.assertTrue(OperatingSelection.fromNBT(incomplete) == null,
                "Operating selection without positions should not deserialize");
        helper.assertTrue(OperatingSelection.fromNBT(new CompoundTag()) == null,
                "Empty operating selection should not deserialize");
        helper.succeed();
    }
}

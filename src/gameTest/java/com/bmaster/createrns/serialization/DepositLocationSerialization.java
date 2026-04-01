package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.info.CustomServerDepositLocation;
import com.bmaster.createrns.content.deposit.info.ServerDepositLocation;
import com.bmaster.createrns.content.deposit.info.StructureServerDepositLocation;
import com.bmaster.createrns.util.CodecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class DepositLocationSerialization {
    private static final ResourceKey<Structure> STRUCTURE_KEY =
            ResourceKey.create(Registries.STRUCTURE, CreateRNS.asResource("deposit_iron"));
    private static final ResourceKey<Structure> CUSTOM_KEY =
            ResourceKey.create(Registries.STRUCTURE, CreateRNS.asResource("deposit_copper"));

    private static final ChunkPos STRUCTURE_ORIGIN = new ChunkPos(4, -3);
    private static final BlockPos CUSTOM_LOCATION = new BlockPos(24, 70, 8);

    @GameTest(template = "empty16x16")
    public void customDepositLocationRoundTrips(GameTestHelper helper) {
        var original = new CustomServerDepositLocation(CUSTOM_KEY, CUSTOM_LOCATION);

        var restored = CustomServerDepositLocation.of(helper.getLevel(), original.serialize());

        helper.assertTrue(restored.equals(original), "Custom deposit should preserve key and origin");
        CodecHelper.assertValueEqual(helper, restored.getLocation(), CUSTOM_LOCATION, "custom deposit location");
        CodecHelper.assertValueEqual(helper, restored.getTypeStr(), "custom", "custom deposit type");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void structureDepositLocationRoundTrips(GameTestHelper helper) {
        var level = helper.getLevel();
        var original = new StructureServerDepositLocation(level, STRUCTURE_KEY, STRUCTURE_ORIGIN);

        var restored = StructureServerDepositLocation.of(level, original.serialize());

        helper.assertTrue(restored.equals(original), "Structure deposit should preserve key and origin");
        CodecHelper.assertValueEqual(helper, restored.getLocation(), STRUCTURE_ORIGIN.getBlockAt(7, 62, 7),
                "fallback structure location");
        CodecHelper.assertValueEqual(helper, restored.getTypeStr(), "ungenerated", "fallback structure deposit type");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void depositLocationDispatchesBySerializedShape(GameTestHelper helper) {
        var level = helper.getLevel();

        var custom = ServerDepositLocation.of(level, new CustomServerDepositLocation(CUSTOM_KEY, CUSTOM_LOCATION).serialize());
        var structure = ServerDepositLocation.of(level, new StructureServerDepositLocation(level, STRUCTURE_KEY, STRUCTURE_ORIGIN).serialize());

        helper.assertTrue(custom instanceof CustomServerDepositLocation, "Location tag should deserialize as custom deposit");
        helper.assertTrue(structure instanceof StructureServerDepositLocation,
                "Start chunk tag should deserialize as structure deposit");
        helper.succeed();
    }
}

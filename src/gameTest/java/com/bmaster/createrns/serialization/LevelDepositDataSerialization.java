package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.info.CustomServerDepositLocation;
import com.bmaster.createrns.content.deposit.info.LevelDepositData;
import com.bmaster.createrns.content.deposit.info.ServerDepositLocation;
import com.bmaster.createrns.content.deposit.info.StructureServerDepositLocation;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class LevelDepositDataSerialization {
    private static final Field FOUND_DEPOSITS_FIELD = getField("foundDeposits");
    private static final Field CUSTOM_DEPOSITS_FIELD = getField("customDeposits");
    private static final Field DEPOSIT_DURABILITIES_FIELD = getField("depositDurabilities");

    private static final ResourceKey<Structure> FOUND_STRUCTURE_KEY =
            ResourceKey.create(Registries.STRUCTURE, CreateRNS.asResource("deposit_iron"));
    private static final ResourceKey<Structure> CUSTOM_STRUCTURE_KEY =
            ResourceKey.create(Registries.STRUCTURE, CreateRNS.asResource("deposit_copper"));

    private static final ChunkPos FOUND_DEPOSIT_CHUNK = new ChunkPos(4, -3);
    private static final BlockPos CUSTOM_DEPOSIT_LOCATION = new BlockPos(24, 70, 8);
    private static final BlockPos DURABILITY_POS_A = new BlockPos(2, 66, 2);
    private static final BlockPos DURABILITY_POS_B = new BlockPos(18, 62, 18);
    private static final long DURABILITY_A = 1234L;
    private static final long DURABILITY_B = 9876L;

    @GameTest(template = "empty16x16")
    public void levelDepositDataRoundTripsSerializableState(GameTestHelper helper) {
        var level = helper.getLevel();
        var provider = level.registryAccess();

        var original = new LevelDepositData(level);
        foundDeposits(original).add(new StructureServerDepositLocation(level, FOUND_STRUCTURE_KEY, FOUND_DEPOSIT_CHUNK));
        var customDeposits = new ObjectOpenHashSet<CustomServerDepositLocation>();
        customDeposits.add(new CustomServerDepositLocation(CUSTOM_STRUCTURE_KEY, CUSTOM_DEPOSIT_LOCATION));
        customDeposits(original).put(CUSTOM_STRUCTURE_KEY.location(), customDeposits);
        depositDurabilities(original).put(DURABILITY_POS_A, DURABILITY_A);
        depositDurabilities(original).put(DURABILITY_POS_B, DURABILITY_B);

        var serialized = original.serializeNBT(provider);
        helper.assertValueEqual(serialized.getList("found", 10).size(), 1, "serialized found deposit count");
        helper.assertValueEqual(serialized.getCompound("custom").getAllKeys().size(), 1, "serialized custom deposit type count");
        helper.assertValueEqual(serialized.getList("durabilities", 10).size(), 2, "serialized durability count");

        var restored = new LevelDepositData(level);
        restored.deserializeNBT(provider, serialized);

        helper.assertTrue(
                restored.getFoundDeposits().contains(new StructureServerDepositLocation(level, FOUND_STRUCTURE_KEY, FOUND_DEPOSIT_CHUNK)),
                "Deserialized found deposits are missing the expected structure deposit"
        );

        var restoredCustomDeposits = customDeposits(restored);
        helper.assertValueEqual(restoredCustomDeposits.size(), 1, "deserialized custom deposit type count");
        helper.assertTrue(
                restoredCustomDeposits.containsKey(CUSTOM_STRUCTURE_KEY.location()),
                "Deserialized custom deposits are missing the expected structure key"
        );
        helper.assertTrue(
                restoredCustomDeposits.get(CUSTOM_STRUCTURE_KEY.location())
                        .contains(new CustomServerDepositLocation(CUSTOM_STRUCTURE_KEY, CUSTOM_DEPOSIT_LOCATION)),
                "Deserialized custom deposits are missing the expected custom deposit"
        );

        var restoredDurabilities = depositDurabilities(restored);
        helper.assertValueEqual(restoredDurabilities.size(), 2, "deserialized durability count");
        helper.assertValueEqual(restoredDurabilities.getLong(DURABILITY_POS_A), DURABILITY_A, "durability at first block");
        helper.assertValueEqual(restoredDurabilities.getLong(DURABILITY_POS_B), DURABILITY_B, "durability at second block");
        helper.succeed();
    }

    private static Field getField(String name) {
        try {
            var field = LevelDepositData.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access LevelDepositData field: " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static ObjectOpenHashSet<ServerDepositLocation> foundDeposits(LevelDepositData data) {
        try {
            return (ObjectOpenHashSet<ServerDepositLocation>) FOUND_DEPOSITS_FIELD.get(data);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access found deposits", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<CustomServerDepositLocation>> customDeposits(
            LevelDepositData data
    ) {
        try {
            return (Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<CustomServerDepositLocation>>)
                    CUSTOM_DEPOSITS_FIELD.get(data);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access custom deposits", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object2LongOpenHashMap<BlockPos> depositDurabilities(LevelDepositData data) {
        try {
            return (Object2LongOpenHashMap<BlockPos>) DEPOSIT_DURABILITIES_FIELD.get(data);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access deposit durabilities", e);
        }
    }
}

package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystUsageStats;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class CatalystUsageStatsSerialization {
    private static final Field LAST_CHANCES_FIELD = getField("lastChances");
    private static final Field LAST_TICKED_CRSES_FIELD = getField("lastTickedCRSes");

    @GameTest(template = "empty16x16")
    public void catalystUsageStatsRoundTrip(GameTestHelper helper) {
        var original = new CatalystUsageStats();
        var lastChances = new Int2FloatOpenHashMap();
        lastChances.put(0, 1.0f);
        lastChances.put(2, 0.35f);
        setLastChances(original, lastChances);

        var lastTickedCrses = new ObjectOpenHashSet<ResourceLocation>();
        lastTickedCrses.add(CreateRNS.asResource("resonance"));
        lastTickedCrses.add(CreateRNS.asResource("overclock"));
        setLastTickedCrses(original, lastTickedCrses);

        var restored = new CatalystUsageStats();
        restored.deserializeNBT(original.serializeNBT());

        helper.assertTrue(restored.isChancesComputed(), "Restored catalyst stats should have computed chances");
        assertFloat(helper, restored.getLastComputedChance(0), 1.0f, "first restored catalyst chance");
        assertFloat(helper, restored.getLastComputedChance(2), 0.35f, "second restored catalyst chance");
        helper.assertTrue(lastTickedCrses(restored).contains(CreateRNS.asResource("resonance")),
                "Restored catalyst stats are missing resonance");
        helper.assertTrue(lastTickedCrses(restored).contains(CreateRNS.asResource("overclock")),
                "Restored catalyst stats are missing overclock");
        helper.succeed();
    }

    @GameTest(template = "empty16x16")
    public void catalystUsageStatsAcceptsLegacyUnqualifiedCrsIds(GameTestHelper helper) {
        var tag = new CompoundTag();
        var lastChances = new CompoundTag();
        lastChances.putFloat("0", 1.0f);
        tag.put("last_chances", lastChances);

        var lastSatisfiedCrses = new ListTag();
        lastSatisfiedCrses.add(StringTag.valueOf("resonance"));
        lastSatisfiedCrses.add(StringTag.valueOf("overclock"));
        tag.put("last_satisfied_crses", lastSatisfiedCrses);

        var restored = new CatalystUsageStats();
        restored.deserializeNBT(tag);

        helper.assertTrue(restored.isChancesComputed(), "Legacy catalyst stats should still have computed chances");
        helper.assertTrue(lastTickedCrses(restored).contains(CreateRNS.asResource("resonance")),
                "Legacy resonance id should map to the create_rns namespace");
        helper.assertTrue(lastTickedCrses(restored).contains(CreateRNS.asResource("overclock")),
                "Legacy overclock id should map to the create_rns namespace");
        helper.succeed();
    }

    private static void assertFloat(GameTestHelper helper, float actual, float expected, String valueName) {
        helper.assertTrue(Math.abs(actual - expected) < 0.0001f,
                "Expected " + valueName + " to be " + expected + ", but was " + actual);
    }
    private static Field getField(String name) {
        try {
            var field = CatalystUsageStats.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access CatalystUsageStats field: " + name, e);
        }
    }

    private static void setLastChances(CatalystUsageStats stats, Int2FloatOpenHashMap chances) {
        try {
            LAST_CHANCES_FIELD.set(stats, chances);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to seed last chances", e);
        }
    }

    private static void setLastTickedCrses(CatalystUsageStats stats, ObjectOpenHashSet<ResourceLocation> crses) {
        try {
            LAST_TICKED_CRSES_FIELD.set(stats, crses);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to seed last satisfied CRS entries", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static ObjectOpenHashSet<ResourceLocation> lastTickedCrses(CatalystUsageStats stats) {
        try {
            return (ObjectOpenHashSet<ResourceLocation>) LAST_TICKED_CRSES_FIELD.get(stats);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access last satisfied CRS entries", e);
        }
    }
}

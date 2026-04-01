package com.bmaster.createrns.serialization;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystUsageStats;
import com.bmaster.createrns.util.CodecHelper;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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

        var lastTickedCrses = new ObjectOpenHashSet<String>();
        lastTickedCrses.add("resonance");
        lastTickedCrses.add("overclock");
        setLastTickedCrses(original, lastTickedCrses);

        var restored = new CatalystUsageStats();
        restored.deserializeNBT(original.serializeNBT());

        helper.assertTrue(restored.isChancesComputed(), "Restored catalyst stats should have computed chances");
        CodecHelper.assertFloat(helper, restored.getLastComputedChance(0), 1.0f, "first restored catalyst chance");
        CodecHelper.assertFloat(helper, restored.getLastComputedChance(2), 0.35f, "second restored catalyst chance");
        helper.assertTrue(lastTickedCrses(restored).contains("resonance"),
                "Restored catalyst stats are missing resonance");
        helper.assertTrue(lastTickedCrses(restored).contains("overclock"),
                "Restored catalyst stats are missing overclock");
        helper.succeed();
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

    private static void setLastTickedCrses(CatalystUsageStats stats, ObjectOpenHashSet<String> crses) {
        try {
            LAST_TICKED_CRSES_FIELD.set(stats, crses);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to seed last satisfied CRS entries", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static ObjectOpenHashSet<String> lastTickedCrses(CatalystUsageStats stats) {
        try {
            return (ObjectOpenHashSet<String>) LAST_TICKED_CRSES_FIELD.get(stats);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access last satisfied CRS entries", e);
        }
    }
}

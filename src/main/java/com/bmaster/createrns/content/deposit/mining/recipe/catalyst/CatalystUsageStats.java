package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CatalystUsageStats implements INBTSerializable<CompoundTag> {
    public static List<CatalystRequirementSet> getLastSatisfiedCRSes(Set<CatalystUsageStats> aggStats) {
        if (aggStats.isEmpty()) return List.of();
        return aggStats.stream()
                .flatMap(s ->
                        (s.lastTickedCRSes != null) ? s.lastTickedCRSes.keySet().stream() : Stream.of())
                .distinct()
                .map(crsName -> CatalystRequirementSetLookup
                        .get(aggStats.stream().findAny().orElseThrow().access, crsName))
                .sorted(Comparator.comparingInt(crs -> crs.displayPriority))
                .toList();
    }

    protected RegistryAccess access = RegistryAccess.EMPTY;
    protected Int2FloatOpenHashMap lastChances = null;
    protected Object2FloatOpenHashMap<String> lastTickedCRSes = null;

    public boolean isChancesComputed() {
        return lastChances != null;
    }

    public float getLastComputedChance(int i) {
        return lastChances.get(i);
    }


    public void clear() {
        lastChances = null;
        lastTickedCRSes = null;
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT() {
        var root = new CompoundTag();
        if (lastChances != null) {
            var lccTag = new CompoundTag();
            for (var e : lastChances.int2FloatEntrySet()) {
                lccTag.putFloat(e.getIntKey() + "", e.getFloatValue());
            }
            root.put("last_chances", lccTag);
        }
        if (lastTickedCRSes != null) {
            var ltcTag = new ListTag();
            // Values (CRS chance multipliers) are currently unused, and are thus not serialized
            for (var crsName : lastTickedCRSes.keySet()) {
                ltcTag.add(StringTag.valueOf(crsName));
            }
            root.put("last_satisfied_crses", ltcTag);
        }
        return root;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("last_chances")) {
            var lccTag = tag.getCompound("last_chances");
            var ltcTag = tag.getList("last_satisfied_crses", Tag.TAG_STRING);

            if (lastChances == null) lastChances = new Int2FloatOpenHashMap();
            else lastChances.clear();
            if (lastTickedCRSes == null) lastTickedCRSes = new Object2FloatOpenHashMap<>();
            else lastTickedCRSes.clear();

            for (var is : lccTag.getAllKeys()) {
                lastChances.put(Integer.parseInt(is), lccTag.getFloat(is));
            }
            // Values (CRS chance multipliers) are currently unused, and are thus not deserialized
            for (int i = 0; i < ltcTag.size(); ++i) {
                lastTickedCRSes.put(ltcTag.getString(i), 0f);
            }
        }
    }

    protected void setRegistryAccess(RegistryAccess access) {
        this.access = access;
    }
}

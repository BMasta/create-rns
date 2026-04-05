package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CatalystUsageStats implements INBTSerializable<CompoundTag> {
    public @Nullable ObjectOpenHashSet<String> lastTickedCRSes = null;
    protected RegistryAccess access = RegistryAccess.EMPTY;
    protected @Nullable Int2FloatOpenHashMap lastChances = null;

    public boolean isChancesComputed() {
        return lastChances != null;
    }

    public float getLastComputedChance(int i) {
        if (lastChances == null) throw new IllegalStateException("Chances were never computed");
        return lastChances.get(i);
    }


    public void clear() {
        lastChances = null;
        lastTickedCRSes = null;
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
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
            for (var crsName : lastTickedCRSes) {
                ltcTag.add(StringTag.valueOf(crsName));
            }
            root.put("last_satisfied_crses", ltcTag);
        }
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains("last_chances")) {
            var lccTag = tag.getCompound("last_chances");
            var ltcTag = tag.getList("last_satisfied_crses", Tag.TAG_STRING);

            if (lastChances == null) lastChances = new Int2FloatOpenHashMap();
            else lastChances.clear();
            if (lastTickedCRSes == null) lastTickedCRSes = new ObjectOpenHashSet<>();
            else lastTickedCRSes.clear();

            for (var is : lccTag.getAllKeys()) {
                lastChances.put(Integer.parseInt(is), lccTag.getFloat(is));
            }
            // Values (CRS chance multipliers) are currently unused, and are thus not deserialized
            for (int i = 0; i < ltcTag.size(); ++i) {
                lastTickedCRSes.add(ltcTag.getString(i));
            }
        }
    }

    protected void setRegistryAccess(RegistryAccess access) {
        this.access = access;
    }
}

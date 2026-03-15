package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.CreateRNS;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LevelDepositData implements INBTSerializable<CompoundTag> {
    protected final ServerLevel level;

    // Serializable
    protected final ObjectOpenHashSet<DepositLocation> foundDeposits = new ObjectOpenHashSet<>();
    protected final Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<CustomDepositLocation>> customDeposits =
            new Object2ObjectOpenHashMap<>();
    protected final Object2LongOpenHashMap<BlockPos> depositDurabilities = new Object2LongOpenHashMap<>();

    // In-memory
    protected final Cache<UUID, DepositLocation.CachedData> perPlayerCache = CacheBuilder.newBuilder()
            .initialCapacity(1)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    public LevelDepositData(ServerLevel level) {
        this.level = level;
    }

    public boolean addCustomDeposit(CustomDepositLocation dep) {
        if (StructureDepositLocation.hasStructureAtChunk(level, dep.key, dep.origin)) return false;
        var depSet = customDeposits.computeIfAbsent(dep.key.location(), k -> new ObjectOpenHashSet<>());
        if (depSet.contains(dep)) return false;
        depSet.add(dep);
        return true;
    }

    public boolean removeCustomDeposit(CustomDepositLocation dep) {
        var set = customDeposits.get(dep.key.location());
        if (set == null) return false;
        boolean result = set.remove(dep);
        if (set.isEmpty()) customDeposits.remove(dep.key.location());
        return result;
    }

    public void forgetFoundDeposits() {
        for (var d : foundDeposits) {
            CreateRNS.LOGGER.debug("Forgot {} at {}", d.getKey().location(), d.getLocationStr());
        }
        foundDeposits.clear();
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var root = new CompoundTag();

        var found = new ListTag();
        for (var dl : foundDeposits) {
            found.add(dl.serialize());
        }

        var custom = new CompoundTag();
        for (var e : customDeposits.object2ObjectEntrySet()) {
            ResourceLocation rl = e.getKey();
            var customPerType = new ListTag();
            for (var dl : e.getValue()) {
                customPerType.add(dl.serialize());
            }
            custom.put(rl.toString(), customPerType);
        }

        var durabilities = new ListTag();
        for (var e : depositDurabilities.object2LongEntrySet()) {
            var d = new CompoundTag();
            d.putLong("pos", e.getKey().asLong());
            d.putLong("durability", e.getLongValue());
            durabilities.add(d);
        }

        root.put("found", found);
        root.put("custom", custom);
        root.put("durabilities", durabilities);

        CreateRNS.LOGGER.trace("Serialized level deposit data with {}", root);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        foundDeposits.clear();
        if (nbt.get("found") instanceof ListTag foundTag) {
            for (var dlTag : foundTag) {
                foundDeposits.add(DepositLocation.of(level, (CompoundTag) dlTag));
            }
            CreateRNS.LOGGER.trace("Deserialized found deposits to {}", foundDeposits);
        } else {
            CreateRNS.LOGGER.error("Failed to deserialize found deposits from nbt");
        }

        customDeposits.clear();
        if (nbt.get("custom") instanceof CompoundTag customTag) {
            for (var id : customTag.getAllKeys()) {
                var rl = ResourceLocation.parse(id);
                var customPerType = new ObjectOpenHashSet<CustomDepositLocation>();
                for (var dlTag : customTag.getList(id, CompoundTag.TAG_COMPOUND)) {
                    customPerType.add(CustomDepositLocation.of(level, (CompoundTag) dlTag));
                }
                customDeposits.put(rl, customPerType);
            }
            CreateRNS.LOGGER.trace("Deserialized custom deposits to {}", customDeposits);
        } else {
            CreateRNS.LOGGER.error("Failed to deserialize custom deposits from nbt");
        }

        depositDurabilities.clear();
        if (!(nbt.get("durabilities") instanceof ListTag durabilities)) {
            CreateRNS.LOGGER.error("Failed to deserialize deposit durabilities from nbt");
            return;
        }
        for (var d : durabilities) {
            if (!(d instanceof CompoundTag dc)) continue;
            depositDurabilities.put(BlockPos.of(dc.getLong("pos")), dc.getLong("durability"));
        }
        CreateRNS.LOGGER.trace("Deserialized durabilities ({} entries)", depositDurabilities.size());
    }
}

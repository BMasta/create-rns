package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.info.sync.FoundDepositsClearS2CPacket;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositIndex implements IDepositIndex {
    protected ServerLevel level;

    // Serializable
    protected final ObjectOpenHashSet<ServerDepositLocation> foundDeposits = new ObjectOpenHashSet<>();
    protected final Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<CustomServerDepositLocation>> customDeposits =
            new Object2ObjectOpenHashMap<>();
    protected final Object2LongOpenHashMap<BlockPos> depositDurabilities = new Object2LongOpenHashMap<>();

    // In-memory
    protected final Cache<UUID, ServerDepositLocation.CachedData> perPlayerCache = CacheBuilder.newBuilder()
            .initialCapacity(1)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    public DepositIndex(ServerLevel sl) {
        this.level = sl;
    }

    public boolean addCustomDeposit(CustomServerDepositLocation dep) {
        if (StructureServerDepositLocation.hasStructureAtChunk(level, dep.key, dep.origin)) return false;
        var depSet = customDeposits.computeIfAbsent(dep.key.location(), k -> new ObjectOpenHashSet<>());
        if (depSet.contains(dep)) return false;
        depSet.add(dep);
        return true;
    }

    public Set<ServerDepositLocation> getFoundDeposits() {
        return foundDeposits;
    }

    public boolean removeCustomDeposit(CustomServerDepositLocation dep) {
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
        FoundDepositsClearS2CPacket.sendToAll(level.getServer(), level.dimension());
    }

    @Override
    public CompoundTag serializeNBT() {
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
            d.putLong("dur", e.getLongValue());
            durabilities.add(d);
        }

        root.put("found", found);
        root.put("custom", custom);
        root.put("durabilities", durabilities);

        CreateRNS.LOGGER.trace("Serialized deposit index with {}", root);
        return root;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        foundDeposits.clear();
        if (nbt.get("found") instanceof ListTag foundTag) {
            for (var dlTag : foundTag) {
                foundDeposits.add(ServerDepositLocation.of(level, (CompoundTag) dlTag));
            }
            CreateRNS.LOGGER.trace("Deserialized found deposits to {}", foundDeposits);
        } else {
            CreateRNS.LOGGER.error("Failed to deserialize found deposits from nbt");
        }

        customDeposits.clear();
        if (nbt.get("custom") instanceof CompoundTag customTag) {
            for (var id : customTag.getAllKeys()) {
                var rl = ResourceLocation.parse(id);
                var customPerType = new ObjectOpenHashSet<CustomServerDepositLocation>();
                for (var dlTag : customTag.getList(id, CompoundTag.TAG_COMPOUND)) {
                    customPerType.add(CustomServerDepositLocation.of(level, (CompoundTag) dlTag));
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
            depositDurabilities.put(BlockPos.of(dc.getLong("pos")), dc.getLong("dur"));
        }
        CreateRNS.LOGGER.trace("Deserialized durabilities ({} entries)", depositDurabilities.size());
    }
}

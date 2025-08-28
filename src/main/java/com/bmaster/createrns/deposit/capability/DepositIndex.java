package com.bmaster.createrns.deposit.capability;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.util.Utils;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.common.util.INBTSerializable;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class DepositIndex implements IDepositIndex, INBTSerializable<CompoundTag> {
    // Generated found deposits are represented as bounding box centers of deposit structures (not bound to res. loc.)
    private final ObjectOpenHashSet<BlockPos> generatedFoundDeposits = new ObjectOpenHashSet<>();

    // Generated deposits are represented as bounding box centers of deposit structures
    private final Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<BlockPos>> generatedDeposits =
            new Object2ObjectOpenHashMap<>();

    // Ungenerated deposits are represented as positions of deposit structure starts since ungenerated deposits
    // do not yet have a bounding box.
    private final Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<BlockPos>> ungeneratedDeposits =
            new Object2ObjectOpenHashMap<>();

    @Override
    public @Nullable BlockPos getNearest(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks,
                                         boolean knownOnly) {
        var playerPos = sp.blockPosition();

        // If unknown deposits are included, scan for any nearby and add to deposit index
        if (!knownOnly) {
            discoverNearest(depositKey, sp, searchRadiusChunks);
        }

        BlockPos closestBP = null;
        double closestDist = Double.MAX_VALUE;
        double dist;

        // Get closest generated deposit
        for (var d : generatedDeposits.getOrDefault(depositKey.location(), new ObjectOpenHashSet<>())) {
            dist = playerPos.distSqr(d);
            if (dist < closestDist) {
                closestDist = dist;
                closestBP = new BlockPos(d);
            }
        }
        if (closestBP != null)
            CreateRNS.LOGGER.info("Closest gen dep {},{},{}", closestBP.getX(), closestBP.getY(), closestBP.getZ());

        // Get closest ungenerated deposit
        for (var d : ungeneratedDeposits.getOrDefault(depositKey.location(), new ObjectOpenHashSet<>())) {
            dist = playerPos.distSqr(d);
            if (dist < closestDist) {
                closestDist = dist;
                closestBP = new BlockPos(d);
            }
        }

        boolean withinMaxDistance = (Math.sqrt(closestDist) <= (searchRadiusChunks << 4));
        return withinMaxDistance ? closestBP : null;
    }

    @Override
    public void add(ResourceKey<Structure> depositKey, StructureStart ss, ServerLevel sl) {
        var startChunk = ss.getChunkPos();
        if (!ss.isValid()) {
            CreateRNS.LOGGER.error("Attempted to add an invalid deposit start to deposit index");
            return;
        }
        var center = ss.getBoundingBox().getCenter();

        // Remove newly-generated deposit start from the list of ungenerated deposits
        for (var t : ungeneratedDeposits.object2ObjectEntrySet()) {
            var dSet = t.getValue();
            dSet.removeIf(d -> Utils.isPosInChunk(d, startChunk));
            CreateRNS.LOGGER.info("Generated deposit start at {},{} removed from ungenerated",
                    startChunk.getBlockX(8), startChunk.getBlockZ(8));
        }

        // Do not add to generated if it already exists in generated+found
        if (generatedFoundDeposits.contains(center)) {
            return;
        }

        // Add to the set of generated deposits
        generatedDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>()).add(center);
    }

    @Override
    public void markAsFound(BlockPos centerPos) {
        Set<BlockPos> foundGenSet = null;

        // Find generated deposit with provided center pos
        for (var e : generatedDeposits.object2ObjectEntrySet()) {
            var genSet = e.getValue();
            if (genSet.contains(centerPos)) foundGenSet = genSet;

        }
        if (foundGenSet == null) {
            CreateRNS.LOGGER.error("Attempted to mark a non-existent deposit");
            return;
        }

        // Remove it from generated, add to generated+found
        foundGenSet.remove(centerPos);
        generatedFoundDeposits.add(centerPos);
        CreateRNS.LOGGER.info("Marking {},{},{} as found", centerPos.getX(), centerPos.getY(), centerPos.getZ());
    }

    @Override
    public CompoundTag serializeNBT() {
        var root = new CompoundTag();
        long[] generatedFound;
        var generated = new CompoundTag();
        var ungenerated = new CompoundTag();

        CreateRNS.LOGGER.info("Serializing GeneratedFound {}", generatedFoundDeposits);
        CreateRNS.LOGGER.info("Serializing Generated {}", generatedDeposits);
        CreateRNS.LOGGER.info("Serializing Ungenerated {}", ungeneratedDeposits);

        generatedFound = generatedFoundDeposits.stream().mapToLong(BlockPos::asLong).toArray();

        for (var e : generatedDeposits.object2ObjectEntrySet()) {
            ResourceLocation rl = e.getKey();
            long[] packed = e.getValue().stream().mapToLong(BlockPos::asLong).toArray();
            generated.putLongArray(rl.toString(), packed);
        }

        for (var e : ungeneratedDeposits.object2ObjectEntrySet()) {
            ResourceLocation rl = e.getKey();
            long[] packed = e.getValue().stream().mapToLong(BlockPos::asLong).toArray();
            ungenerated.putLongArray(rl.toString(), packed);
        }

        root.putLongArray("GeneratedFound", generatedFound);
        root.put("Generated", generated);
        root.put("Ungenerated", ungenerated);

        CreateRNS.LOGGER.info("Serialized deposit index with {}", root);
        return root;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (!(nbt.get("GeneratedFound") instanceof LongArrayTag generatedFound) ||
                !(nbt.get("Generated") instanceof CompoundTag generated) ||
                !(nbt.get("Ungenerated") instanceof CompoundTag ungenerated)) {
            CreateRNS.LOGGER.error("Failed to deserialize deposit index from nbt");
            return;
        }
        CreateRNS.LOGGER.info("Deserializing deposit index with tag {}", nbt);
        generatedFoundDeposits.clear();
        generatedDeposits.clear();
        ungeneratedDeposits.clear();

        for (long l : generatedFound.getAsLongArray()) {
            generatedFoundDeposits.add(BlockPos.of(l));
        }
        for (String key : generated.getAllKeys()) {
            var rl = ResourceLocation.parse(key);
            long[] packed = generated.getLongArray(key);
            var set = new ObjectOpenHashSet<BlockPos>(packed.length);
            for (long l : packed) set.add(BlockPos.of(l));
            generatedDeposits.put(rl, set);
        }
        for (String key : ungenerated.getAllKeys()) {
            var rl = ResourceLocation.parse(key);
            long[] packed = ungenerated.getLongArray(key);
            var set = new ObjectOpenHashSet<BlockPos>(packed.length);
            for (long l : packed) set.add(BlockPos.of(l));
            ungeneratedDeposits.put(rl, set);
        }
        CreateRNS.LOGGER.info("Deserialized GeneratedFound to {}", generatedFoundDeposits);
        CreateRNS.LOGGER.info("Deserialized Generated to {}", generatedDeposits);
        CreateRNS.LOGGER.info("Deserialized Ungenerated to {}", ungeneratedDeposits);
    }

    private void discoverNearest(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks) {
        var sl = (ServerLevel) sp.level();
        var gen = sl.getChunkSource().getGenerator();
        var target = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(depositKey);

        var newDeposit = gen.findNearestMapStructure(sl, HolderSet.direct(target), sp.blockPosition(),
                searchRadiusChunks, true);

        // If found invalid (not yet generated) deposit, save it to the ungenerated list
        var ss = (newDeposit == null) ? null : sl.structureManager().getStructureWithPieceAt(newDeposit.getFirst(),
                newDeposit.getSecond().get());
        if (newDeposit != null && !ss.isValid()) {
            var pos = newDeposit.getFirst();
            CreateRNS.LOGGER.info("Found undiscovered deposit at {}, {}, {}", pos.getX(), pos.getY(),
                    pos.getZ());
            ungeneratedDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>())
                    .add(pos);
        }
        // If found valid (generated) deposit, save it to the generated list
        else if (newDeposit != null) {
            var pos = ss.getBoundingBox().getCenter();
            CreateRNS.LOGGER.info("Found undiscovered generated deposit at {}, {}, {}", pos.getX(), pos.getY(),
                    pos.getZ());
            generatedDeposits.computeIfAbsent(depositKey.location(), k -> new ObjectOpenHashSet<>())
                    .add(pos);
        }
    }
}

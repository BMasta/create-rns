package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSPacks;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerLocateContext;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerLocateContext.DepositCandidateFilter;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class StructureServerDepositLocation extends ServerDepositLocation {
    public static boolean hasStructureAtChunk(ServerLevel sl, ResourceKey<Structure> depKey, ChunkPos pos) {
        var chunkAccess = sl.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
        var structure = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(depKey);
        var start = sl.structureManager().getStartForStructure(SectionPos.bottomOf(chunkAccess), structure, chunkAccess);
        return start != null && start.isValid();
    }

    public static boolean isStructureAtChunkFound(ServerLevel sl, ResourceKey<Structure> depKey, ChunkPos pos) {
        var depData = IDepositIndex.get(sl);
        return depData.foundDeposits.stream()
                .anyMatch(dl -> dl instanceof StructureServerDepositLocation
                        && dl.getKey().equals(depKey)
                        && dl.getOrigin().equals(pos));
    }

    public static @Nullable StructureServerDepositLocation getNearestStructure(
            ServerLevel sl, Either<ResourceKey<Structure>, TagKey<Structure>> depResOrTag, BlockPos pos,
            boolean allowFound, int searchRadiusChunks
    ) {
        var res = depResOrTag.left().orElse(null);
        var tag = depResOrTag.right().orElse(null);
        assert res != null || tag != null;
        if (res != null) return getNearestStructure(sl, res, pos, allowFound, searchRadiusChunks);
        return getNearestStructure(sl, tag, pos, allowFound, searchRadiusChunks);
    }

    /// Find nearest structure using a structure key (single structure)
    public static @Nullable StructureServerDepositLocation getNearestStructure(
            ServerLevel sl, ResourceKey<Structure> depKey, BlockPos pos, boolean allowFound, int searchRadiusChunks
    ) {
        var gen = sl.getChunkSource().getGenerator();
        var searchRadiusRegions = Mth.positiveCeilDiv(searchRadiusChunks, getSpacing(sl, depKey));
        var targetStructure = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(depKey);
        DepositCandidateFilter ignoreFilter = (level, structure, chunkPos) ->
                !allowFound && structure == targetStructure.value() && isStructureAtChunkFound(sl, depKey, chunkPos);

        Pair<BlockPos, Holder<Structure>> hit;
        if (allowFound) {
            hit = gen.findNearestMapStructure(sl, HolderSet.direct(targetStructure), pos, searchRadiusRegions, false);
        } else {
            // The thread-local context within this scope is used by a mixin to filter out found deposits
            try (var ignored = DepositScannerLocateContext.push(ignoreFilter)) {
                hit = gen.findNearestMapStructure(sl, HolderSet.direct(targetStructure), pos, searchRadiusRegions, false);
            }
        }

        return (hit != null) ? new StructureServerDepositLocation(sl, depKey, new ChunkPos(hit.getFirst())) : null;
    }

    /// Find nearest structure using a tag
    public static @Nullable StructureServerDepositLocation getNearestStructure(
            ServerLevel sl, TagKey<Structure> depTag, BlockPos pos, boolean allowFound, int searchRadiusChunks
    ) {
        var lookup = sl.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var reg = sl.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var namedDepHS = lookup.get(depTag).orElse(null);
        if (namedDepHS == null) return null;

        var gen = sl.getChunkSource().getGenerator();
        var searchRadiusRegions = Mth.positiveCeilDiv(searchRadiusChunks, getSpacing(sl, depTag));
        DepositCandidateFilter ignoreFilter = (level, structure, chunkPos) -> {
            if (allowFound) return false;
            for (var h : namedDepHS) {
                var s = h.get();
                var hKey = reg.getResourceKey(s).orElse(null);
                if (hKey == null) continue;
                if (structure == h.value() && isStructureAtChunkFound(sl, hKey, chunkPos)) return true;
            }
            return false;
        };

        Pair<BlockPos, Holder<Structure>> hit;
        if (allowFound) {
            hit = gen.findNearestMapStructure(sl, namedDepHS, pos, searchRadiusRegions, false);
        } else {
            // The thread-local context within this scope is used by a mixin to filter out found deposits
            try (var ignored = DepositScannerLocateContext.push(ignoreFilter)) {
                hit = gen.findNearestMapStructure(sl, namedDepHS, pos, searchRadiusRegions, false);
            }
        }

        if (hit == null) return null;
        var s = hit.getSecond();
        if (s == null) return null;
        var depKey = reg.getResourceKey(s.get()).orElse(null);
        if (depKey == null) return null;
        return new StructureServerDepositLocation(sl, depKey, new ChunkPos(hit.getFirst()));
    }

    public static StructureServerDepositLocation of(ServerLevel sl, CompoundTag nbt) {
        var key = ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse(nbt.getString("id")));
        var origin = new ChunkPos(nbt.getLong("start_chunk"));
        return new StructureServerDepositLocation(sl, key, origin);
    }

    private static int getSpacing(ServerLevel sl, ResourceKey<Structure> key) {
        var structure = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(key);
        return getSpacing(sl, structure);
    }

    private static int getSpacing(ServerLevel sl, TagKey<Structure> tag) {
        var structures = sl.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(tag).orElse(null);
        if (structures == null) {
            CreateRNS.LOGGER.error("Failed to resolve structure tag {}. Using default spacing of {}",
                    tag.location(), RNSPacks.DEFAULT_SPACING);
            return RNSPacks.DEFAULT_SPACING;
        }

        int spacing = Integer.MAX_VALUE;
        for (var structure : structures) {
            spacing = Math.min(spacing, getSpacing(sl, structure));
        }

        return spacing;
    }

    private static int getSpacing(ServerLevel sl, Holder<Structure> structure) {
        var key = structure.unwrapKey().orElse(null);
        int spacing = sl.getChunkSource().getGeneratorState().getPlacementsForStructure(structure).stream()
                .filter(RandomSpreadStructurePlacement.class::isInstance)
                .map(p -> ((RandomSpreadStructurePlacement) p).spacing())
                .min(Integer::compareTo)
                .orElse(-1);
        if (spacing == -1) {
            spacing = RNSPacks.DEFAULT_SPACING;
            var id = key == null ? "<unknown>" : key.location();
            CreateRNS.LOGGER.error("Failed to resolve spacing of structure {}. Using default spacing of {}",
                    id, spacing);
        }
        return spacing;
    }

    protected ServerLevel level;
    protected @Nullable BlockPos center;
    protected @Nullable StructureStart start;

    public StructureServerDepositLocation(ServerLevel sl, ResourceKey<Structure> key, ChunkPos structureStartPos) {
        super(key, structureStartPos);
        level = sl;
    }

    @Override
    public BlockPos getLocation() {
        return (center != null) ? center : origin.getBlockAt(7, 62, 7);
    }

    @Override
    public boolean computePreciseLocation() {
        if (center != null) return true;

        // Compute structure start
        if (start == null) {
            var chunkAccess = level.getChunk(origin.x, origin.z, ChunkStatus.STRUCTURE_STARTS);
            var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(key);
            start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunkAccess), structure, chunkAccess);
        }
        if (start == null || !start.isValid()) return false;

        // Compute center if start is valid
        center = start.getBoundingBox().getCenter();
        return true;
    }

    @Override
    public String getTypeStr() {
        return center == null ? "ungenerated" : "generated";
    }

    @Override
    public String getLocationStr() {
        return center == null
                ? origin.getMiddleBlockX() + ",~," + origin.getMiddleBlockZ()
                : center.getX() + "," + center.getY() + "," + center.getZ();
    }

    public CompoundTag serialize() {
        var root = new CompoundTag();
        root.putString("id", key.location().toString());
        root.putLong("start_chunk", origin.toLong());
        return root;
    }
}

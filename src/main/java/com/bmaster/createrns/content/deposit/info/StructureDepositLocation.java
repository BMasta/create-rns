package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.RNSMisc;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class StructureDepositLocation extends DepositLocation {
    /// Spacing may be overridden by datapacks and is therefore resolved at runtime after all datapacks have been loaded.
    /// The default value serves as a fallback.
    private static int SPACING = RNSPacks.DEFAULT_SPACING;

    public static int getSpacing() {
        return SPACING;
    }

    public static void setSpacing(int spacing) {
        if (spacing <= 0) throw new IllegalArgumentException("Deposit structure spacing must be positive");
        SPACING = spacing;
    }

    public static boolean hasStructureAtChunk(ServerLevel sl, ResourceKey<Structure> depKey, ChunkPos pos) {
        var chunkAccess = sl.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
        var structure = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(depKey);
        var start = sl.structureManager().getStartForStructure(SectionPos.bottomOf(chunkAccess), structure, chunkAccess);
        return start != null && start.isValid();
    }

    public static boolean isStructureAtChunkFound(ServerLevel sl, ResourceKey<Structure> depKey, ChunkPos pos) {
        var depData = sl.getData(RNSMisc.LEVEL_DEPOSIT_DATA.get());
        return depData.foundDeposits.stream()
                .anyMatch(dl -> dl instanceof StructureDepositLocation
                        && dl.getKey().equals(depKey)
                        && dl.getOrigin().equals(pos));
    }

    public static @Nullable StructureDepositLocation getNearestStructure(
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
    public static @Nullable StructureDepositLocation getNearestStructure(
            ServerLevel sl, ResourceKey<Structure> depKey, BlockPos pos, boolean allowFound, int searchRadiusChunks
    ) {
        var gen = sl.getChunkSource().getGenerator();
        var searchRadiusRegions = searchRadiusChunks / SPACING;
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

        return (hit != null) ? new StructureDepositLocation(sl, depKey, new ChunkPos(hit.getFirst())) : null;
    }

    /// Find nearest structure using a tag
    public static @Nullable StructureDepositLocation getNearestStructure(
            ServerLevel sl, TagKey<Structure> depTag, BlockPos pos, boolean allowFound, int searchRadiusChunks
    ) {
        var lookup = sl.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var namedDepHS = lookup.get(depTag).orElse(null);
        if (namedDepHS == null) return null;

        var gen = sl.getChunkSource().getGenerator();

        var searchRadiusRegions = searchRadiusChunks / SPACING;
        DepositCandidateFilter ignoreFilter = (level, structure, chunkPos) -> {
            if (allowFound) return false;
            for (var h : namedDepHS) {
                var hKey = h.getKey();
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
        var depKey = hit.getSecond().getKey();
        if (depKey == null) return null;
        return new StructureDepositLocation(sl, depKey, new ChunkPos(hit.getFirst()));
    }

    public static StructureDepositLocation of(ServerLevel sl, CompoundTag nbt) {
        var key = ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse(nbt.getString("id")));
        var origin = new ChunkPos(nbt.getLong("start_chunk"));
        return new StructureDepositLocation(sl, key, origin);
    }

    protected ServerLevel level;
    protected @Nullable BlockPos center;
    protected @Nullable StructureStart start;

    public StructureDepositLocation(ServerLevel sl, ResourceKey<Structure> key, ChunkPos structureStartPos) {
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

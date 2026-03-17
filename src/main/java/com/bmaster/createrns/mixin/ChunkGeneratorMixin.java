package com.bmaster.createrns.mixin;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerLocateContext;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    @Inject(method = "getStructureGeneratingAt(" +
            "Ljava/util/Set;Lnet/minecraft/world/level/LevelReader;" +
            "Lnet/minecraft/world/level/StructureManager;" +
            "ZLnet/minecraft/world/level/levelgen/structure/placement/StructurePlacement;" +
            "Lnet/minecraft/world/level/ChunkPos;)Lcom/mojang/datafixers/util/Pair;",
            at = @At("RETURN"), cancellable = true)
    private static void create_rns$filterFound(
            Set<Holder<Structure>> structureHoldersSet, LevelReader level, StructureManager structureManager,
            boolean skipKnownStructures, StructurePlacement placement, ChunkPos chunkPos,
            CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir
    ) {
        var result = cir.getReturnValue();
        if (result == null) return;
        if (!(level instanceof ServerLevel sl)) return;

        var filter = DepositScannerLocateContext.getFilter();
        if (filter == null) return;
        var structure = result.getSecond().value();
        var structureId = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getResourceKey(structure)
                .map(key -> key.location().toString()).orElse("<unknown>");
        var ignored = filter.shouldIgnore(sl, structure, chunkPos);
        CreateRNS.LOGGER.trace("[Scanner locate filter] Candidate {} at chunk {},{} -> {}", structureId,
                chunkPos.x, chunkPos.z, ignored ? "ignored" : "accepted");
        if (ignored) {
            cir.setReturnValue(null);
        }
    }
}

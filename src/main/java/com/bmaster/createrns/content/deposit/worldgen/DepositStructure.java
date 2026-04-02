package com.bmaster.createrns.content.deposit.worldgen;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSMisc;
import com.bmaster.createrns.util.codec.Range;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructure extends Structure {
    public static final Codec<DepositStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
                    RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes")
                            .forGetter(s -> s.biomes),
                    WeightedStructure.CODEC.listOf().fieldOf("structures")
                            .forGetter(s -> s.structures),
                    PlacementStrategy.CODEC.fieldOf("placement_strategy")
                            .forGetter(s -> s.placementStrategy),
                    Range.FLEXIBLE_CODEC.fieldOf("height")
                            .forGetter(s -> s.height)
            )
            .apply(i, DepositStructure::new));

    private static float elapsedCounter = 0;

    private final HolderSet<Biome> biomes;
    private final List<WeightedStructure> structures;
    private final PlacementStrategy placementStrategy;
    private final Range height;

    private DepositStructure(HolderSet<Biome> biomes, List<WeightedStructure> structures,
            PlacementStrategy placementStrategy, Range height
    ) {
        super(createSettings(biomes));
        if (structures.isEmpty()) throw new IllegalArgumentException("Deposit structure list cannot be empty");
        this.biomes = biomes;
        this.structures = structures;
        this.placementStrategy = placementStrategy;
        this.height = height;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        var chunkPos = context.chunkPos();
        CreateRNS.LOGGER.trace("Finding structure at {}", chunkPos);
        long swStart = System.nanoTime();

        // Pick random structure rotation
        var rotation = Rotation.getRandom(context.random());

        // Pick random structure according to specified weights
        var structure = WeightedStructure.pick(context.random(), structures);

        // Create a single-pool element
        var element = StructurePoolElement.single(structure.id().toString(), structure.processor())
                .apply(StructureTemplatePool.Projection.RIGID);

        // Determine start position
        var worldgenContext = new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor());
        var yOffset = placementStrategy.sampleYOffset(height, context.random(), worldgenContext);

        // Create structure piece
        var piece = createPiece(context, chunkPos, element, rotation, yOffset);
        if (piece == null) {
            float delta = ((int) ((System.nanoTime() - swStart) / 10_000.0)) / 100f;
            elapsedCounter += delta;
            CreateRNS.LOGGER.trace("  -> rejected ({} ms | total {} ms)",
                    ((int) (delta / 10_000.0)) / 100f,
                    ((int) (elapsedCounter / 10_000.0)) / 100f);
            return Optional.empty();
        }

        // Determine representative structure position
        var boundingBox = piece.getBoundingBox();
        var center = boundingBox.getCenter();
        var representativePos = new BlockPos(center.getX(), boundingBox.minY(), center.getZ());

        long delta = System.nanoTime() - swStart;
        elapsedCounter += delta;
        CreateRNS.LOGGER.trace("  -> accepted ({} ms | total {} ms)",
                ((int) (delta / 10_000.0)) / 100f,
                ((int) (elapsedCounter / 10_000.0)) / 100f);
        return Optional.of(new GenerationStub(representativePos, builder -> builder.addPiece(piece)));
    }

    @Override
    public StructureType<?> type() {
        return RNSMisc.DEPOSIT_STRUCTURE.get();
    }

    private static StructureSettings createSettings(HolderSet<Biome> biomes) {
        return new StructureSettings(biomes, Map.of(), GenerationStep.Decoration.STRONGHOLDS, TerrainAdjustment.NONE);
    }

    private @Nullable PoolElementStructurePiece createPiece(
            GenerationContext context, ChunkPos chunkPos, StructurePoolElement element,
            Rotation rotation, int yOffset
    ) {
        var startPos = new BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ());
        var boundingBox = element.getBoundingBox(context.structureTemplateManager(), startPos, rotation);
        var piece = new PoolElementStructurePiece(context.structureTemplateManager(), element, startPos,
                0, rotation, boundingBox);

        var targetY = placementStrategy.getPlacementBottomY(context, piece, yOffset);
        if (targetY.isEmpty()) return null;

        var offsetY = targetY.getAsInt() - (boundingBox.minY());
        piece.move(0, offsetY, 0);
        return piece;
    }

}

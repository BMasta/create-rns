package com.bmaster.createrns.content.deposit.worldgen;

import com.bmaster.createrns.util.Range;
import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.OptionalInt;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum PlacementStrategy implements StringRepresentable {
    OVERWORLD("overworld", Optional.of(Heightmap.Types.OCEAN_FLOOR_WG)),
    NETHER("nether", Optional.empty());

    public static final Codec<PlacementStrategy> CODEC = StringRepresentable.fromEnum(PlacementStrategy::values);

    private final String name;
    private final Optional<Heightmap.Types> projectStartToHeightmap;

    PlacementStrategy(String name, Optional<Heightmap.Types> projectStartToHeightmap) {
        this.name = name;
        this.projectStartToHeightmap = projectStartToHeightmap;
    }

    /// Returns the random Y coordinate picked from the provided range.
    public int getStartY(Range range, WorldgenRandom random, WorldGenerationContext context) {
        if (range.min() == range.max()) {
            return ConstantHeight.of(VerticalAnchor.absolute(range.min()))
                    .sample(random, context);
        } else {
            return UniformHeight.of(VerticalAnchor.absolute(range.min()), VerticalAnchor.absolute(range.max()))
                    .sample(random, context);
        }
    }

    /// Returns the Y coordinate at which the structure piece should be placed.
    /// Picks the first empty block above ground level.
    public OptionalInt getPlacementY(Structure.GenerationContext context, PoolElementStructurePiece piece, BlockPos startPos) {
        if (this.projectStartToHeightmap.isEmpty()) return OptionalInt.of(startPos.getY());

        var boundingBox = piece.getBoundingBox();
        var center = boundingBox.getCenter();
        var heightmap = this.projectStartToHeightmap.get();
        var placementY = startPos.getY() + context.chunkGenerator().getFirstFreeHeight(
                center.getX(),
                center.getZ(),
                heightmap,
                context.heightAccessor(),
                context.randomState()
        );
        return OptionalInt.of(placementY);
    }

    /// Returns the bottom-center position of the structure piece
    public BlockPos getRepresentativePosition(PoolElementStructurePiece piece, BlockPos startPos) {
        var boundingBox = piece.getBoundingBox();
        var centerX = (boundingBox.maxX() + boundingBox.minX()) / 2;
        var centerZ = (boundingBox.maxZ() + boundingBox.minZ()) / 2;
        var projectedY = this.projectStartToHeightmap.isPresent()
                ? boundingBox.minY() + piece.getGroundLevelDelta()
                : startPos.getY();
        return new BlockPos(centerX, projectedY, centerZ);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

}

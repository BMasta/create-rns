package com.bmaster.createrns.content.deposit.worldgen;

import com.bmaster.createrns.util.codec.Range;
import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalInt;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum PlacementStrategy implements StringRepresentable {
    OVERWORLD("overworld", new OverworldPlacementHelper()),
    NETHER("nether", new NetherPlacementHelper());

    public static final Codec<PlacementStrategy> CODEC = StringRepresentable.fromEnum(PlacementStrategy::values);

    private final String name;
    private final PlacementHelper helper;

    PlacementStrategy(String name, PlacementHelper helper) {
        this.name = name;
        this.helper = helper;
    }

    /// Returns the random Y coordinate picked from the provided range.
    public int sampleYOffset(Range range, WorldgenRandom random, WorldGenerationContext context) {
        if (range.min() == range.max()) {
            return ConstantHeight.of(VerticalAnchor.absolute(range.min()))
                    .sample(random, context);
        } else {
            return UniformHeight.of(VerticalAnchor.absolute(range.min()), VerticalAnchor.absolute(range.max()))
                    .sample(random, context);
        }
    }

    /// Returns the Y coordinate at which the structure piece should be placed.
    /// Unlike structure start, this operation is terrain-aware.
    /// If terrain does not allow for a good placement, an empty optional is returned.
    public OptionalInt getPlacementBottomY(GenerationContext context, PoolElementStructurePiece piece, int yOffset) {
        return helper.getPlacementBottomY(context, piece, yOffset);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    private static abstract class PlacementHelper {
        public abstract OptionalInt getPlacementBottomY(GenerationContext context, PoolElementStructurePiece piece, int yOffset);
    }

    private static class OverworldPlacementHelper extends PlacementHelper {
        @Override
        public OptionalInt getPlacementBottomY(GenerationContext context, PoolElementStructurePiece piece, int yOffset) {
            var center = piece.getBoundingBox().getCenter();
            return OptionalInt.of(yOffset + context.chunkGenerator().getFirstFreeHeight(
                    center.getX(),
                    center.getZ(),
                    Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(),
                    context.randomState()));
        }
    }

    private static class NetherPlacementHelper extends PlacementHelper {
        private static final int N_BEDROCK_LAYERS = 5;
        private static final int REQUIRED_AIR_BLOCKS = 8;
        private static final double BEARING_BLOCK_THRESHOLD = 0.9f;

        @Override
        public OptionalInt getPlacementBottomY(GenerationContext context, PoolElementStructurePiece piece, int yOffset) {
            var bb = piece.getBoundingBox();
            var center = bb.getCenter();
            var gen = context.chunkGenerator();
            int minY = gen.getMinY();
            int maxY = minY + gen.getGenDepth();
            minY += N_BEDROCK_LAYERS;
            maxY -= N_BEDROCK_LAYERS;
            var centerColumn = gen.getBaseColumn(center.getX(), center.getZ(), context.heightAccessor(), context.randomState());

            int airBlocks = 0;
            for (int y = minY; y < maxY; y++) {
                airBlocks = (centerColumn.getBlock(y).isAir()) ? airBlocks + 1 : 0;
                if (airBlocks < REQUIRED_AIR_BLOCKS) continue;

                var bottomY = yOffset + y - REQUIRED_AIR_BLOCKS - bb.getYSpan() + 1;
                if (hasEnoughBearingBlocks(context, bb, bottomY)) {
                    return OptionalInt.of(bottomY);
                } else {
                    return OptionalInt.empty();
                }
            }

            return OptionalInt.empty();
        }

        private boolean hasEnoughBearingBlocks(GenerationContext context, BoundingBox boundingBox, int bottomY) {
            int xSpan = boundingBox.getXSpan();
            int ySpan = boundingBox.getYSpan();
            int zSpan = boundingBox.getZSpan();
            int totalBlocks = 2 * xSpan * zSpan + 2 * ySpan * zSpan + 2 * ySpan * xSpan;
            int requiredBearingBlocks = (int) Math.ceil(totalBlocks * BEARING_BLOCK_THRESHOLD);
            int bearingBlocks = 0;
            int minX = boundingBox.minX();
            int maxX = boundingBox.maxX();
            int minZ = boundingBox.minZ();
            int maxZ = boundingBox.maxZ();
            int topY = bottomY + ySpan - 1;

            boolean accepted = false;
            do {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        bearingBlocks += countBearingBlock(context, x, bottomY - 1, z);
                        bearingBlocks += countBearingBlock(context, x, topY + 1, z);
                        if (bearingBlocks >= requiredBearingBlocks) {
                            accepted = true;
                            break;
                        }
                    }
                }

                for (int y = bottomY; y <= topY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        bearingBlocks += countBearingBlock(context, minX - 1, y, z);
                        bearingBlocks += countBearingBlock(context, maxX + 1, y, z);
                        if (bearingBlocks >= requiredBearingBlocks) {
                            accepted = true;
                            break;
                        }
                    }
                }

                for (int y = bottomY; y <= topY; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        bearingBlocks += countBearingBlock(context, x, y, minZ - 1);
                        bearingBlocks += countBearingBlock(context, x, y, maxZ + 1);
                        if (bearingBlocks >= requiredBearingBlocks) {
                            accepted = true;
                            break;
                        }
                    }
                }
            } while (false);

            return accepted;
        }

        private int countBearingBlock(GenerationContext context, int x, int y, int z) {
            var column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
            return column.getBlock(y).is(BlockTags.NETHER_CARVER_REPLACEABLES) ? 1 : 0;
        }
    }
}

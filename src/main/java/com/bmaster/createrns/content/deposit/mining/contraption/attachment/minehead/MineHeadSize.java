package com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public enum MineHeadSize implements StringRepresentable {
    SMALL(0, 0, "block/mine_head", 1, 0f),
    MEDIUM(1, 1, "block/mine_head_large", 2, 0.248f),
    LARGE(2, 3, "block/mine_head_large", 3.63f, 0.3604f);

    public final int radiusBonus;
    public final int tipOffset;
    public final ResourceLocation model;
    public final float modelScale;
    public final float modelOffset;


    MineHeadSize(int radiusBonus, int tipOffset, String modelPath, float modelScale, float modelOffset) {
        this.radiusBonus = radiusBonus;
        this.tipOffset = tipOffset;
        this.model = CreateRNS.asResource(modelPath);
        this.modelScale = modelScale;
        this.modelOffset = modelOffset;
    }

    public BlockPos getControllerPos(BlockPos tipPos, Direction facing) {
        return tipPos.relative(facing.getOpposite(), tipOffset);
    }

    public BlockPos getTipPos(BlockPos controllerPos, Direction facing) {
        return controllerPos.relative(facing, tipOffset);
    }

    public Set<BlockPos> getOccupiedPositions(BlockPos controllerPos, Direction direction) {
        Set<BlockPos> positions = new HashSet<>();
        Direction u = getUDirection(direction);
        Direction v = getVDirection(direction);
        switch (this) {
            case SMALL -> positions.add(controllerPos);
            case MEDIUM -> {
                addSquareLayer(positions, controllerPos, u, v, 1, true);
                positions.add(controllerPos.relative(direction));
            }
            case LARGE -> {
                addSquareLayer(positions, controllerPos, u, v, 2, false);
                addSquareLayer(positions, controllerPos.relative(direction), u, v, 2, false);
                addSquareLayer(positions, controllerPos.relative(direction, 2), u, v, 1, false);
                positions.add(controllerPos.relative(direction, 3));
            }
        }
        return positions;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static Direction getUDirection(Direction direction) {
        return switch (direction.getAxis()) {
            case X -> Direction.UP;
            case Y, Z -> Direction.EAST;
        };
    }

    private static Direction getVDirection(Direction direction) {
        return switch (direction.getAxis()) {
            case X, Y -> Direction.SOUTH;
            case Z -> Direction.UP;
        };
    }

    private static BlockPos offsetInPlane(BlockPos pos, Direction u, int du, Direction v, int dv) {
        return pos.offset(
                u.getStepX() * du + v.getStepX() * dv,
                u.getStepY() * du + v.getStepY() * dv,
                u.getStepZ() * du + v.getStepZ() * dv
        );
    }

    private static void addSquareLayer(
            Set<BlockPos> positions, BlockPos center, Direction u, Direction v, int radius, boolean addCorners
    ) {
        for (int du = -radius; du <= radius; du++) {
            for (int dv = -radius; dv <= radius; dv++) {
                if (!addCorners && Math.abs(du) == radius && Math.abs(dv) == radius) continue;
                positions.add(offsetInPlane(center, u, du, v, dv));
            }
        }
    }
}

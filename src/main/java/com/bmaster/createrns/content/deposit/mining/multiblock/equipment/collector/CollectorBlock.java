package com.bmaster.createrns.content.deposit.mining.multiblock.equipment.collector;

import com.bmaster.createrns.content.deposit.mining.multiblock.equipment.MiningEquipmentBlock;
import com.simibubi.create.AllShapes;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CollectorBlock extends MiningEquipmentBlock {
    public static final AllShapes.Builder SHAPE = new AllShapes.Builder(Block.box(0, 0, 7, 16, 16, 9));
    public static final AllShapes.Builder SHAPE_90 = new AllShapes.Builder(Block.box(7, 0, 0, 9, 16, 16));


    public CollectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShaper getAttachedRotatedShape(Direction attachedFace, Rotation rot) {
        return switch (rot) {
            case NONE, CLOCKWISE_180 -> SHAPE.forDirectional(attachedFace);
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> SHAPE_90.forDirectional(attachedFace);
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}

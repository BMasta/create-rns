package com.bmaster.createrns.content.deposit.mining.multiblock.attachment.drillhead;

import com.bmaster.createrns.content.deposit.mining.multiblock.attachment.MiningEquipmentBlock;
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
public class DrillHeadBlock extends MiningEquipmentBlock {
    public static final AllShapes.Builder SHAPE = new AllShapes.Builder(Block.box(2, 0, 2, 14, 22, 14));

    public DrillHeadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShaper getAttachedRotatedShape(Direction attachedFace, Rotation rot) {
        return SHAPE.forDirectional(attachedFace);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}

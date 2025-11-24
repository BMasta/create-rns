package com.bmaster.createrns.content.deposit.mining.multiblock.equipment.resonator;

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
public class ResonatorBlock extends MiningEquipmentBlock {
    public static final AllShapes.Builder SHAPE = new AllShapes.Builder(Block.box(6, 0, 6, 10, 14, 10));

    public ResonatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShaper getAttachedRotatedShape(Direction attachedFace, Rotation rot) {
        return SHAPE.forDirectional(attachedFace);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}

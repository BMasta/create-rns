package com.bmaster.createrns.content.deposit.mining.multiblock.equipment.resonator;

import com.bmaster.createrns.content.deposit.mining.multiblock.equipment.MiningEquipmentBlock;
import com.simibubi.create.AllShapes;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResonatorBlock extends MiningEquipmentBlock {
    public static final VoxelShaper SHAPE = new AllShapes.Builder(Block.box(6, 0, 6, 10, 14, 10)).forDirectional();

    public ResonatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(FACING));
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}

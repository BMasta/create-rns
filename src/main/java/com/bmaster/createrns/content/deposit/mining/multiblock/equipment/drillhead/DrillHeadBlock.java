package com.bmaster.createrns.content.deposit.mining.multiblock.equipment.drillhead;

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
public class DrillHeadBlock extends MiningEquipmentBlock {
    public static final VoxelShaper SHAPE = new AllShapes.Builder(Block.box(2, 0, 2, 14, 22, 14)).forDirectional();

    public DrillHeadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(FACING));
    }
}

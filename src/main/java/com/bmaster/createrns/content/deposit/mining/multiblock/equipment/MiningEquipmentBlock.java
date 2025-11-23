package com.bmaster.createrns.content.deposit.mining.multiblock.equipment;

import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer.ClaimerType;
import com.bmaster.createrns.content.deposit.claiming.IDepositClaimerOutlineTarget;
import com.bmaster.createrns.content.deposit.mining.block.MiningBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MiningEquipmentBlock extends DirectionalBlock implements IDepositClaimerOutlineTarget {
    public MiningEquipmentBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        return defaultBlockState().setValue(FACING, context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown() ? clickedFace.getOpposite() : clickedFace);
    }

    @Override
    public ClaimerType getClaimerType() {
        return MiningBehaviour.CLAIMER_TYPE;
    }
}

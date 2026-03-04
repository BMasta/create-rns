package com.bmaster.createrns.content.deposit.mining.contraption;

import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.MiningEquipmentBlock;
import com.bmaster.createrns.util.Utils;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.Set;

public class MinerContraption extends BearingContraption {
    public BlockPos drillHeadPos;
    protected Set<BlockPos> equipmentPositions = new ObjectOpenHashSet<>();

    public MinerContraption(boolean isWindmill, Direction facing) {
        super(isWindmill, facing);
    }

    @Override
    public boolean isActorTypeDisabled(ItemStack filter) {
        return super.isActorTypeDisabled(filter);
    }

    @Override
    public boolean searchMovedStructure(Level world, BlockPos pos, @Nullable Direction forcedDirection) throws AssemblyException {
        drillHeadPos = null;
        if (world.getBlockState(pos).is(RNSBlocks.DRILL_HEAD_BLOCK.get())) {
            anchor = pos;
            if (bounds == null) bounds = new AABB(BlockPos.ZERO);
            addBlock(world, pos, capture(world, pos));
            return true;
        }
        boolean result = super.searchMovedStructure(world, pos, forcedDirection);
        if (drillHeadPos == null) throw new RNSAssemblyException("not_one_drill_head");

//        for (var bp : equipmentPositions) {
//            if (bp.distManhattan(drillHeadPos) > 1) throw new RNSAssemblyException("wrong_equipment_position");
//        }

        return result;
    }

    @Override
    protected boolean moveBlock(Level world, @Nullable Direction forcedDirection, Queue<BlockPos> frontier, Set<BlockPos> visited) throws AssemblyException {
        var pos = frontier.peek();
        if (pos != null) {
            var bs = world.getBlockState(pos);
            if (bs.getBlock() instanceof MiningEquipmentBlock) {
//                if (MiningEquipmentBlock.getConnectedDirection(bs) != facing) {
//                    throw new RNSAssemblyException("wrong_equipment_direction");
//                }
                equipmentPositions.add(pos);
            }

            if (bs.is(RNSBlocks.DRILL_HEAD_BLOCK.get())) {
                if (drillHeadPos != null) throw new RNSAssemblyException("not_one_drill_head");
                // Local position of the drill head must not differ from origin
                // on any axis other than the one the contraption is facing.
                if (Utils.dot(Utils.normalVecFlip(facing, true), toLocalPos(pos)) != 0) {
                    throw new RNSAssemblyException("drill_head_not_aligned_with_bearing");
                }
                drillHeadPos = pos;
            }
        }
        return super.moveBlock(world, forcedDirection, frontier, visited);
    }
}

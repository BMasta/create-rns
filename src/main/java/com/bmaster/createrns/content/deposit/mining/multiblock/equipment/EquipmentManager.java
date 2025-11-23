package com.bmaster.createrns.content.deposit.mining.multiblock.equipment;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.content.deposit.mining.multiblock.equipment.drillhead.DrillHeadBlock;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EquipmentManager {
    public static EquipmentManager from(Contraption contraption) {
        return new EquipmentManager(contraption);
    }

    public final Direction correctFacingDirection;
    public final BlockPos drillHeadPos;
    public final int resonatorCount;

    protected final Contraption contraption;
    protected final MovementContext drillMovementContext;

    public void dropItem(ItemStack stack) {
        ItemStack remainder;
        if (AllConfigs.server().kinetics.moveItemsToStorage.get())
            remainder = ItemHandlerHelper.insertItem(drillMovementContext.contraption.getStorage().getAllItems(),
                    stack, false);
        else
            remainder = stack;
        if (remainder.isEmpty())
            return;

        // Actors might void items if their positions is undefined
        Vec3 vec = drillMovementContext.position;
        if (vec == null)
            return;

        ItemEntity itemEntity = new ItemEntity(drillMovementContext.world,
                vec.x + 0.5 * drillMovementContext.world.random.nextFloat(),
                vec.y + 0.5 * drillMovementContext.world.random.nextFloat(),
                vec.z + 0.5 * drillMovementContext.world.random.nextFloat(), remainder);
        itemEntity.setDeltaMovement(drillMovementContext.motion.add(0, 0.5f, 0)
                .scale(drillMovementContext.world.random.nextFloat() * .3f));
        drillMovementContext.world.addFreshEntity(itemEntity);
    }

    protected EquipmentManager(Contraption contraption) {
        this.contraption = contraption;

        BlockPos drillHeadPos = null;
        MovementContext drillMovementContext = null;
        Direction correctFacingDirection = null;
        int resonatorCount = 0;

        for (var a : contraption.getActors()) {
            var bs = a.left.state();
            if (bs.is(RNSContent.DRILL_HEAD_BLOCK.get())) {
                // Assumes that drill is facing the same way as the bearing
                drillMovementContext = a.right;
                drillHeadPos = a.left.pos().offset(contraption.anchor);
                correctFacingDirection = bs.getValue(DrillHeadBlock.FACING);
            } else if (bs.is(RNSContent.RESONATOR_BLOCK.get())) {
                resonatorCount++;
            }
        }
        if (drillHeadPos == null) throw new IllegalStateException("Miner contraption does not have a drill head");

        this.drillHeadPos = drillHeadPos;
        this.correctFacingDirection = correctFacingDirection;
        this.drillMovementContext = drillMovementContext;
        this.resonatorCount = resonatorCount;
    }
}

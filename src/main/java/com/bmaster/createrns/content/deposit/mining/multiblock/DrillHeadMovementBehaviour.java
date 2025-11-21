package com.bmaster.createrns.content.deposit.mining.multiblock;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class DrillHeadMovementBehaviour implements MovementBehaviour {
    static void dropItemStatic(MovementContext context, ItemStack stack) {
        ItemStack remainder;
        if (AllConfigs.server().kinetics.moveItemsToStorage.get())
            remainder = ItemHandlerHelper.insertItem(context.contraption.getStorage().getAllItems(), stack, false);
        else
            remainder = stack;
        if (remainder.isEmpty())
            return;

        // Actors might void items if their positions is undefined
        Vec3 vec = context.position;
        if (vec == null)
            return;

        ItemEntity itemEntity = new ItemEntity(context.world, vec.x + 0.5 * context.world.random.nextFloat(),
                vec.y + 0.5 * context.world.random.nextFloat(), vec.z + 0.5 * context.world.random.nextFloat(), remainder);
        itemEntity.setDeltaMovement(context.motion.add(0, 0.5f, 0)
                .scale(context.world.random.nextFloat() * .3f));
        context.world.addFreshEntity(itemEntity);
    }
}

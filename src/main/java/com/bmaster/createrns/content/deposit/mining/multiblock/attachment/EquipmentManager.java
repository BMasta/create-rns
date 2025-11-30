package com.bmaster.createrns.content.deposit.mining.multiblock.attachment;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.content.deposit.mining.multiblock.MinerBearingBlockEntity;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.ResonanceCatalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.ShatteringResonanceCatalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.StabilizingResonanceCatalyst;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.infrastructure.config.AllConfigs;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EquipmentManager {
    public static EquipmentManager from(BearingContraption contraption) {
        return new EquipmentManager(contraption);
    }

    public final BlockPos drillHeadPos;
    public final ObjectOpenHashSet<Catalyst> catalysts = new ObjectOpenHashSet<>();

    protected final MinerBearingBlockEntity bearing;
    protected final BearingContraption contraption;

    public void dropItem(ItemStack stack) {
        ItemStack remainder;
        if (AllConfigs.server().kinetics.moveItemsToStorage.get())
            remainder = ItemHandlerHelper.insertItem(contraption.getStorage().getAllItems(), stack, false);
        else
            remainder = stack;
        if (remainder.isEmpty())
            return;

        Vec3 vec = new Vec3(drillHeadPos.getX(), drillHeadPos.getY(), drillHeadPos.getZ());

        ItemEntity itemEntity = new ItemEntity(contraption.entity.level(),
                vec.x + 0.4 * contraption.entity.level().random.nextFloat(),
                vec.y + 0.4 * contraption.entity.level().random.nextFloat(),
                vec.z + 0.4 * contraption.entity.level().random.nextFloat(), remainder);
        itemEntity.setDeltaMovement(new Vec3(0, 0.5f, 0));
        contraption.entity.level().addFreshEntity(itemEntity);
    }

    protected EquipmentManager(BearingContraption contraption) {
        this.contraption = contraption;

        BlockPos drillHeadPos = null;

        for (var e : contraption.getBlocks().entrySet()) {
            var pos = e.getKey();
            var bs = e.getValue().state();
            if (bs.is(RNSContent.DRILL_HEAD_BLOCK.get())) {
                drillHeadPos = pos.offset(contraption.anchor);
            }
        }

        if (drillHeadPos == null) throw new IllegalStateException("Miner contraption does not have a drill head");
        var bearingPos = contraption.anchor.relative(contraption.getFacing().getOpposite());
        if (!(contraption.entity.level().getBlockEntity(bearingPos) instanceof MinerBearingBlockEntity be)) {
            throw new IllegalStateException("Could not find the bearing block entity");
        }

        this.bearing = be;
        this.drillHeadPos = drillHeadPos;

        var rc = ResonanceCatalyst.fromContraption(contraption);
        if (rc != null) catalysts.add(rc);

        var stc = ShatteringResonanceCatalyst.fromContraption(contraption);
        if (stc != null) catalysts.add(stc);

        var sbc = StabilizingResonanceCatalyst.fromContraption(contraption);
        if (sbc != null) catalysts.add(sbc);
    }
}

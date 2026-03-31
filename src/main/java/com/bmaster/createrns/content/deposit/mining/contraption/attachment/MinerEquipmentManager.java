package com.bmaster.createrns.content.deposit.mining.contraption.attachment;

import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead.MineHeadBlock;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead.MineHeadSize;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.AttachmentCatalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.FluidCatalyst;
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
public class MinerEquipmentManager {
    public static MinerEquipmentManager from(BearingContraption contraption) {
        return new MinerEquipmentManager(contraption);
    }

    public final BlockPos mineHeadPos;
    public final MineHeadSize mineHeadSize;
    public final ObjectOpenHashSet<Catalyst> catalysts = new ObjectOpenHashSet<>();
    public final boolean isResonanceActive;

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

        Vec3 vec = new Vec3(mineHeadPos.getX(), mineHeadPos.getY(), mineHeadPos.getZ());

        ItemEntity itemEntity = new ItemEntity(contraption.entity.level(),
                vec.x + 0.4 * contraption.entity.level().random.nextFloat(),
                vec.y + 0.4 * contraption.entity.level().random.nextFloat(),
                vec.z + 0.4 * contraption.entity.level().random.nextFloat(), remainder);
        itemEntity.setDeltaMovement(new Vec3(0, 0.5f, 0));
        contraption.entity.level().addFreshEntity(itemEntity);
    }

    protected MinerEquipmentManager(BearingContraption contraption) {
        this.contraption = contraption;
        // bearing pos = -27 65 8
        // tip pos = -27 58 8
        var bearingPos = contraption.anchor.relative(contraption.getFacing().getOpposite());
        BlockPos mineHeadPos = null;
        MineHeadSize mineHeadSize = null;

        for (var e : contraption.getBlocks().entrySet()) {
            var pos = e.getKey();
            var bs = e.getValue().state();
            if (bs.is(RNSBlocks.MINE_HEAD.get())) {
                var tipOffset = MineHeadBlock.getConnectedDirection(bs)
                        .getNormal()
                        .multiply(bs.getValue(MineHeadBlock.SIZE).getTipOffset());
                mineHeadPos = pos.offset(contraption.anchor).offset(tipOffset);
                mineHeadSize = bs.getValue(MineHeadBlock.SIZE);
            }
        }

        if (mineHeadPos == null) throw new IllegalStateException("Miner contraption does not have a mine head");
        if (!(contraption.entity.level().getBlockEntity(bearingPos) instanceof MinerBearingBlockEntity be)) {
            throw new IllegalStateException("Could not find the bearing block entity");
        }

        this.bearing = be;
        this.mineHeadPos = mineHeadPos;
        this.mineHeadSize = mineHeadSize;

        var attachments = AttachmentCatalyst.fromContraption(contraption);
        boolean resonanceActive = false;
        for (var a : attachments) {
            if (a.attachmentBlock.defaultBlockState().is(RNSTags.RNSBlockTags.RESONATOR_ATTACHMENTS)) resonanceActive = true;
        }
        this.isResonanceActive = resonanceActive;
        catalysts.addAll(attachments);

        var fluid = FluidCatalyst.fromContraption(contraption);
        if (fluid != null) catalysts.add(fluid);

    }
}

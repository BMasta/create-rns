package com.bmaster.createrns.content.deposit.mining.block;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.content.deposit.mining.IHaveAdaptiveGoggleInformation;
import com.bmaster.createrns.content.deposit.mining.MiningEffectsGenerator;
import com.bmaster.createrns.content.deposit.mining.MiningItemHandler;
import com.bmaster.createrns.util.GoggleTooltipModifiers;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.BiFunction;

@ParametersAreNonnullByDefault
public class MinerBlockEntity extends KineticBlockEntity implements IHaveAdaptiveGoggleInformation {
    protected final MiningItemHandler inventory = new MiningItemHandler(this);
    protected MiningEffectsGenerator effects = null;

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(RNSContent.MINER_BE.get(), pos, state);
    }

    public MiningItemHandler getItemHandler(@Nullable Direction side) {
        return inventory;
    }

    public boolean isMining() {
        return getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE).isMining();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        effects = new MiningEffectsGenerator(level, () -> worldPosition, () -> Direction.DOWN);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;

        if (level.isClientSide) {
            if (isMining()) effects.spawnParticles();
        } else {
            tryEjectUp();
        }
    }

    @Override
    public void tickAudio() {
        if (!isMining()) return;
        effects.tickSoundScape(getSpeed());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        behaviours.add(new MiningBehaviour(this, () -> Direction.DOWN));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider p, boolean clientPacket) {
        super.write(tag, p, clientPacket);
        tag.put("inventory", inventory.serializeNBT(p));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider p, boolean clientPacket) {
        super.read(tag, p, clientPacket);
        inventory.deserializeNBT(p, tag.getCompound("inventory"));

        if (clientPacket && effects != null) {
            effects.setParticles(getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE).getClaimedDepositBlocks());
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, inventory);
    }

    @Override
    public String getLangIdentifier() {
        return "miner";
    }

    @Override
    public KineticBlockEntity getTargetBlockEntity() {
        return this;
    }

    @Override
    public List<BiFunction<Context, List<Component>, Boolean>> getPrimarySections() {
        return List.of(GoggleTooltipModifiers::addInventoryToGoggleTooltip);
    }

    @Override
    public List<BiFunction<Context, List<Component>, Boolean>> getSecondarySections() {
        return List.of(GoggleTooltipModifiers::addRatesToGoggleTooltip, GoggleTooltipModifiers::addUsesToGoggleTooltip);
    }

    @Override
    public List<BiFunction<Context, List<Component>, Boolean>> getMandatoryBottomSections() {
        return List.of(GoggleTooltipModifiers::addKineticsToGoggleTooltip);
    }

    /// Kinetic BE already implements IHaveGoggleInformation, so an explicit override is needed
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return IHaveAdaptiveGoggleInformation.super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    protected void tryEjectUp() {
        if (level == null) return;

        var above = worldPosition.above();
        var targetInv = level.getCapability(Capabilities.ItemHandler.BLOCK, above, Direction.DOWN);
        if (targetInv == null) {
            InvManipulationBehaviour inserter = level.getBlockEntity(above) == null ? null
                    : BlockEntityBehaviour.get(level, above, InvManipulationBehaviour.TYPE);
            targetInv = (inserter == null) ? null : inserter.getInventory();
        }
        if (targetInv == null) return;

        var extracted = inventory.extractFirstAvailableItem(true);
        if (extracted.isEmpty()) return;
        for (int i = 0; i < targetInv.getSlots(); ++i) {
            var remaining = targetInv.insertItem(i, extracted, true);
            // We extract a single item, so insertion is always atomic
            if (remaining.isEmpty()) {
                extracted = inventory.extractFirstAvailableItem(false);
                assert !extracted.isEmpty();
                remaining = targetInv.insertItem(i, extracted, false);
                assert remaining.isEmpty();
                return;
            }
        }
    }
}

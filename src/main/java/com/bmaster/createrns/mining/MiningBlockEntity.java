package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class MiningBlockEntity extends KineticBlockEntity {
    private LazyOptional<IItemHandler> inventoryCap = LazyOptional.empty();
    protected final MiningEntityItemHandler inventory = new MiningEntityItemHandler(this);

    public MiningBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract String getLangIdentifier();

    @Override
    public void onLoad() {
        super.onLoad();

        // Initialize the inventory capability when the BE is first loaded
        inventoryCap = LazyOptional.of(() -> inventory);
    }

    public MiningEntityItemHandler getItemHandler(@Nullable Direction side) {
        return inventory;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        inventoryCap.invalidate();
    }

    public boolean isMining() {
        return getBehaviour(MiningBehaviour.TYPE).isMining();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(new MiningBehaviour(this));
    }


    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added;

        // Try adding desired section(s)
        if (!isPlayerSneaking) added = addInventoryToGoggleTooltip(tooltip, true);
        else {
            added = addRatesToGoggleTooltip(tooltip, true);
            if (!ServerConfig.infiniteDeposits && addUsesToGoggleTooltip(tooltip)) added = true;
        }

        // If unsuccessful, try adding the less desired
        if (!added) {
            if (!isPlayerSneaking) {
                added = addRatesToGoggleTooltip(tooltip, true);
                if (!ServerConfig.infiniteDeposits && addUsesToGoggleTooltip(tooltip)) added = true;
            } else added = addInventoryToGoggleTooltip(tooltip, true);
        }

        // Add kinetics regardless
        added = addKineticsToGoggleTooltip(tooltip, !added);
        return added;
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("inventory", inventory.serializeNBT());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        inventory.deserializeNBT(tag.getCompound("inventory"));
    }

    @Override
    protected void addStressImpactStats(List<Component> tooltip, float stressAtBase) {
        super.addStressImpactStats(tooltip, stressAtBase);
    }

    @SuppressWarnings("SameParameterValue")
    protected boolean addInventoryToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
        if (inventory.isEmpty()) return false;

        if (isMainSection) {
            new LangBuilder(CreateRNS.MOD_ID).translate(getLangIdentifier() + ".contents").forGoggles(tooltip);
        } else {
            // Newline between sections
            new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        }

        for (int slot = 0; slot < inventory.getSlots(); ++slot) {
            var is = inventory.getStackInSlot(slot);
            if (is.equals(ItemStack.EMPTY)) continue;
            new LangBuilder(CreateRNS.MOD_ID)
                    .add(is.getHoverName().copy().withStyle(ChatFormatting.GRAY))
                    .add(Component.literal(" x" + is.getCount()).withStyle(ChatFormatting.GREEN))
                    .forGoggles(tooltip, 1);
        }

        return true;
    }

    protected boolean addUsesToGoggleTooltip(List<Component> tooltip) {
        var mb = getBehaviour(MiningBehaviour.TYPE);
        var process = mb.getProcess();
        if (process == null || mb.getClaimedDepositBlocks().isEmpty() || level == null) return false;

        new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        new LangBuilder(CreateRNS.MOD_ID).translate(getLangIdentifier() + ".remaining_deposit_uses").forGoggles(tooltip);

        process.innerProcesses.stream().sorted((a, b) -> {
                    var au = (a.remainingUses == 0) ? Long.MAX_VALUE : a.remainingUses;
                    var bu = (b.remainingUses == 0) ? Long.MAX_VALUE : b.remainingUses;
                    // First sort by remaining uses
                    if (au != bu) return -Long.compare(au, bu);
                    // Then by deposit block id
                    return a.recipe.getDepositBlock().getDescriptionId()
                            .compareToIgnoreCase(b.recipe.getDepositBlock().getDescriptionId());
                })
                .forEachOrdered(p -> {
                    var usesComp = (p.remainingUses > 0)
                            ? Component.literal(Long.toString(p.remainingUses))
                            : Component.translatable("create_rns." + getLangIdentifier() + ".infinite");
                    new LangBuilder(CreateRNS.MOD_ID)
                            .add(p.recipe.getDepositBlock().getName()
                                    .append(": ")
                                    .withStyle(ChatFormatting.GRAY))
                            .add(usesComp
                                    .withStyle(ChatFormatting.GREEN))
                            .forGoggles(tooltip, 1);
                });
        return true;
    }

    @SuppressWarnings("SameParameterValue")
    protected boolean addRatesToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
        var mb = getBehaviour(MiningBehaviour.TYPE);
        var process = mb.getProcess();
        if (process == null || mb.getClaimedDepositBlocks().isEmpty()) return false;

        if (isMainSection) {
            new LangBuilder(CreateRNS.MOD_ID).translate(getLangIdentifier() + ".production_rates").forGoggles(tooltip);
        } else {
            // Newline between sections
            new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        }

        var rates = process.getEstimatedRates(mb.getCurrentProgressIncrement());
        rates.object2FloatEntrySet().stream().sorted((a, b) -> {
                    float av = a.getFloatValue();
                    float bv = b.getFloatValue();
                    // First sort by rate
                    if (av != bv) return -Float.compare(av, bv);
                    // Then by item id
                    var arl = ForgeRegistries.ITEMS.getKey(a.getKey());
                    var brl = ForgeRegistries.ITEMS.getKey(b.getKey());
                    if (arl == null) return 1;
                    if (brl == null) return -1;
                    return arl.toString().compareToIgnoreCase(brl.toString());
                })
                .forEachOrdered(e -> new LangBuilder(CreateRNS.MOD_ID)
                        .add(e.getKey().getDescription().copy()
                                .append(": ")
                                .withStyle(ChatFormatting.GRAY))
                        .add(Component.literal(String.format(java.util.Locale.ROOT, "%.1f", e.getFloatValue()))
                                .append(Component.translatable(CreateRNS.MOD_ID + "." + getLangIdentifier() + ".per_hour"))
                                .withStyle(ChatFormatting.GREEN))
                        .forGoggles(tooltip, 1)
                );
        return true;
    }

    protected boolean addKineticsToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
        float stressAtBase = 0f;
        if (IRotate.StressImpact.isEnabled()) stressAtBase = calculateStressApplied();
        if (Mth.equal(stressAtBase, 0)) return false;

        if (isMainSection) {
            CreateLang.translate("gui.goggles.kinetic_stats").forGoggles(tooltip);
        } else {
            // Newline between sections
            new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        }
        addStressImpactStats(tooltip, calculateStressApplied());
        return true;
    }
}

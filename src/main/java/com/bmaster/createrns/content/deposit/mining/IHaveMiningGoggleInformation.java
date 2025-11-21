package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.block.MiningBehaviour;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import static net.minecraft.ChatFormatting.GRAY;

public interface IHaveMiningGoggleInformation extends IHaveGoggleInformation {
    /// Must have a mining behavior
    KineticBlockEntity getTargetBlockEntity();

    String getLangIdentifier();

    @Override
    default boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
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

    @SuppressWarnings("SameParameterValue")
    default boolean addInventoryToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
        var be = getTargetBlockEntity();
        var level = be.getLevel();
        if (level == null) return false;
        var inventory = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElse(null);
        if (inventory == null) return false;
        boolean empty = true;
        for (int i = 0; i < inventory.getSlots(); ++i) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                empty = false;
                break;
            }
        }
        if (empty) return false;

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

    default boolean addUsesToGoggleTooltip(List<Component> tooltip) {
        var be = getTargetBlockEntity();
        var mb = be.getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE);
        var process = mb.getProcess();
        if (process == null || mb.getClaimedDepositBlocks().isEmpty()) return false;

        new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        new LangBuilder(CreateRNS.MOD_ID).translate("mining.remaining_deposit_uses").forGoggles(tooltip);

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
                            : Component.translatable("create_rns.mining.infinite");
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
    default boolean addRatesToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
        var be = getTargetBlockEntity();
        var mb = be.getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE);
        var process = mb.getProcess();
        if (process == null || mb.getClaimedDepositBlocks().isEmpty()) return false;

        if (isMainSection) {
            new LangBuilder(CreateRNS.MOD_ID).translate("mining.production_rates").forGoggles(tooltip);
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
                .forEachOrdered(e ->
                        new LangBuilder(CreateRNS.MOD_ID)
                                .add(e.getKey().getDescription().copy()
                                        .append(": ")
                                        .withStyle(ChatFormatting.GRAY))
                                .add(Component.literal(String.format(java.util.Locale.ROOT, "%.1f", e.getFloatValue()))
                                        .append(Component.translatable(CreateRNS.MOD_ID + ".mining.per_hour"))
                                        .withStyle(ChatFormatting.GREEN))
                                .forGoggles(tooltip, 1)
                );
        return true;
    }

    default boolean addKineticsToGoggleTooltip(List<Component> tooltip, boolean isMainSection) {
        var be = getTargetBlockEntity();

        float stressAtBase = 0f;
        if (IRotate.StressImpact.isEnabled()) stressAtBase = be.calculateStressApplied();
        if (Mth.equal(stressAtBase, 0)) return false;

        if (isMainSection) {
            CreateLang.translate("gui.goggles.kinetic_stats").forGoggles(tooltip);
        } else {
            // Newline between sections
            new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        }

        CreateLang.translate("tooltip.stressImpact")
                .style(GRAY)
                .forGoggles(tooltip);

        float stressTotal = stressAtBase * Math.abs(be.getTheoreticalSpeed());

        CreateLang.number(stressTotal)
                .translate("generic.unit.stress")
                .style(ChatFormatting.AQUA)
                .space()
                .add(CreateLang.translate("gui.goggles.at_current_speed")
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        return true;
    }
}

package com.bmaster.createrns.util;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.IHaveAdaptiveGoggleInformation.Context;
import com.bmaster.createrns.content.deposit.mining.block.MiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.multiblock.ContraptionMiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.ResonanceCatalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.ShatteringResonanceCatalyst;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.resonance.StabilizingResonanceCatalyst;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.List;

public class GoggleTooltipModifiers {
    @SuppressWarnings("SameParameterValue")
    public static boolean addInventoryToGoggleTooltip(Context c, List<Component> tooltip) {
        var be = c.target();
        var level = be.getLevel();
        if (level == null) return false;
        var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), null);
        if (inventory == null) return false;
        boolean empty = true;
        for (int i = 0; i < inventory.getSlots(); ++i) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                empty = false;
                break;
            }
        }
        if (empty) return false;

        if (c.isFirstSection()) {
            new LangBuilder(CreateRNS.MOD_ID).translate(c.langId() + ".contents").forGoggles(tooltip);
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

    public static boolean addUsesToGoggleTooltip(Context c, List<Component> tooltip) {
        if (ServerConfig.infiniteDeposits) return false;
        var be = c.target();
        if (!(be instanceof SmartBlockEntity sbe)) return false;
        var mb = sbe.getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE);
        if (mb == null) return false;
        var process = mb.getProcess();
        if (process == null || mb.getClaimedDepositBlocks().isEmpty()) return false;

        if (!c.isFirstSection()) {
            new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        }
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

    public static boolean addRatesToGoggleTooltip(Context c, List<Component> tooltip) {
        var be = c.target();
        if (!(be instanceof SmartBlockEntity sbe)) return false;
        var mb = sbe.getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE);
        if (mb == null) return false;
        var process = mb.getProcess();
        if (process == null || !mb.isMining()) return false;

        if (c.isFirstSection()) {
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
                    var arl = BuiltInRegistries.ITEM.getKeyOrNull(a.getKey());
                    var brl = BuiltInRegistries.ITEM.getKeyOrNull(b.getKey());
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

    public static boolean addMinerInfoToGoggleTooltip(Context c, List<Component> tooltip) {
        var be = c.target();
        if (!(be instanceof SmartBlockEntity sbe)) return false;
        var mb = sbe.getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE);
        if (!(mb instanceof ContraptionMiningBehaviour cmb)) return false;
        if (cmb.equipment == null) return false;
        var spec = mb.getSpec();
        if (spec == null) return false;
        var process = mb.getProcess();
        if (process == null) return false;

        if (c.isFirstSection()) {
            new LangBuilder(CreateRNS.MOD_ID)
                    .translate("contraption_mining.info")
                    .forGoggles(tooltip);
        } else {
            // Newline between sections
            new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        }

        int standard = 0;
        int shattering = 0;
        int stabilizing = 0;
        for (var cat : cmb.equipment.catalysts) {
            if (cat instanceof ResonanceCatalyst) standard += ((ResonanceCatalyst) cat).resonatorCount;
            if (cat instanceof ShatteringResonanceCatalyst)
                shattering += ((ShatteringResonanceCatalyst) cat).resonatorCount;
            else if (cat instanceof StabilizingResonanceCatalyst)
                stabilizing += ((StabilizingResonanceCatalyst) cat).resonatorCount;
        }

        if (standard > 0) {
            new LangBuilder(CreateRNS.MOD_ID)
                    .translate("contraption_mining.resonance.standard", Integer.toString(standard))
                    .style(ChatFormatting.LIGHT_PURPLE)
                    .forGoggles(tooltip);
        }
        if (shattering > 0) {
            new LangBuilder(CreateRNS.MOD_ID)
                    .translate("contraption_mining.resonance.shattering", Integer.toString(shattering))
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip);
        }
        if (stabilizing > 0) {
            new LangBuilder(CreateRNS.MOD_ID)
                    .translate("contraption_mining.resonance.stabilizing", Integer.toString(stabilizing))
                    .style(ChatFormatting.AQUA)
                    .forGoggles(tooltip);
        }

        return true;
    }

    public static boolean addKineticsToGoggleTooltip(Context c, List<Component> tooltip) {
        var be = c.target();
        if (!(be instanceof KineticBlockEntity kbe)) return false;
        float stressAtBase = 0f;
        if (IRotate.StressImpact.isEnabled()) stressAtBase = kbe.calculateStressApplied();
        if (Mth.equal(stressAtBase, 0)) return false;

        if (c.isFirstSection()) {
            CreateLang.translate("gui.goggles.kinetic_stats").forGoggles(tooltip);
        } else {
            // Newline between sections
            new LangBuilder(CreateRNS.MOD_ID).space().forGoggles(tooltip);
        }

        CreateLang.translate("tooltip.stressImpact")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        float stressTotal = stressAtBase * Math.abs(kbe.getTheoreticalSpeed());

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

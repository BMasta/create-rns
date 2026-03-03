package com.bmaster.createrns.util;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.IHaveAdaptiveGoggleInformation.Context;
import com.bmaster.createrns.content.deposit.mining.MiningProcess.RateEstimationStatus;
import com.bmaster.createrns.content.deposit.mining.block.MiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.multiblock.ContraptionMiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystUsageStats;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GoggleTooltipModifiers {
    @SuppressWarnings("SameParameterValue")
    public static boolean addInventoryToGoggleTooltip(Context c, List<Component> tooltip) {
        var be = c.target();
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

        if (c.isFirstSection()) {
            CreateRNS.lang().translate(c.langId() + ".contents").forGoggles(tooltip);
        } else {
            // Newline between sections
            CreateRNS.lang().space().forGoggles(tooltip);
        }

        for (int slot = 0; slot < inventory.getSlots(); ++slot) {
            var is = inventory.getStackInSlot(slot);
            if (is.equals(ItemStack.EMPTY)) continue;
            CreateRNS.lang()
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
            CreateRNS.lang().space().forGoggles(tooltip);
        }
        CreateRNS.lang().translate("mining.remaining_deposit_uses").forGoggles(tooltip);

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
                    CreateRNS.lang()
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
        if (process == null) return false;
        if (mb instanceof ContraptionMiningBehaviour cmb) {
            if (!cmb.isMiningOrStalled()) return false;
        } else if (!mb.isMining()) return false;

        if (c.isFirstSection()) {
            CreateRNS.lang().translate("mining.production_rates").forGoggles(tooltip);
        } else {
            // Newline between sections
            CreateRNS.lang().space().forGoggles(tooltip);
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
                        CreateRNS.lang()
                                .add(e.getKey().getDescription().copy()
                                        .append(": ")
                                        .withStyle(ChatFormatting.GRAY))
                                .add(Component.literal(String.format(Locale.ROOT, "%.1f", e.getFloatValue()))
                                        .append(Component.translatable(CreateRNS.ID + ".mining.per_hour"))
                                        .withStyle(ChatFormatting.GREEN))
                                .forGoggles(tooltip, 1)
                );

        addEstimationComponent(process.getRateEstimationStatus(), tooltip);

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
            CreateRNS.lang()
                    .translate("contraption_mining.info")
                    .forGoggles(tooltip);
        } else {
            // Newline between sections
            CreateRNS.lang().space().forGoggles(tooltip);
        }

        // Add descriptions contributed by active CRSes
        var aggStats = process.innerProcesses.stream().map(p -> p.catStats).collect(Collectors.toSet());
        var activeCRSes = CatalystUsageStats.getLastSatisfiedCRSes(aggStats);
        for (var crs : activeCRSes) {
            for (var d : crs.activeTooltipDescriptions(activeCRSes)) {
                CreateRNS.lang().add(d).forGoggles(tooltip);
            }
        }

        addEstimationComponent(process.getRateEstimationStatus(), tooltip);

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
            CreateRNS.lang().space().forGoggles(tooltip);
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

    protected static void addEstimationComponent(RateEstimationStatus status, List<Component> tooltip) {
        if (status == RateEstimationStatus.ALL) return;
        var statusTag = switch (status) {
            case NONE -> "none_estimated";
            case SOME -> "some_estimated";
            default -> throw new IllegalStateException("Unexpected value: " + status);
        };
        CreateRNS.lang()
                .add(Component.translatable(CreateRNS.ID + ".mining." + statusTag)
                        .withStyle(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);
    }
}

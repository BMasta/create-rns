package com.bmaster.createrns.util;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.IHaveAdaptiveGoggleInformation.Context;
import com.bmaster.createrns.content.deposit.mining.IMinerHolderBE;
import com.bmaster.createrns.content.deposit.mining.MiningProcess.RateEstimationStatus;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Locale;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class GoggleTooltipModifiers {
    public static boolean addUsesToGoggleTooltip(Context c, List<Component> tooltip) {
        if (ServerConfig.INFINITE_DEPOSITS.get()) return false;
        if (!(c.target() instanceof IMinerHolderBE holder)) return false;
        var miner = holder.getMiner();
        var process = miner.getProcess();
        var stats = miner.getStats();
        var claimedBlocks = miner.getClaimedDepositBlocks();
        if (process == null || claimedBlocks == null || claimedBlocks.isEmpty()) return false;

        if (!c.isFirstSection()) {
            CreateRNS.lang().space().forGoggles(tooltip);
        }
        CreateRNS.lang().translate("mining.remaining_deposit_uses").forGoggles(tooltip);

        stats.remainingResources.object2LongEntrySet().stream().sorted((a, b) -> {
                    var au = (a.getLongValue() == 0) ? Long.MAX_VALUE : a.getLongValue();
                    var bu = (b.getLongValue() == 0) ? Long.MAX_VALUE : b.getLongValue();
                    // First sort by remaining uses
                    if (au != bu) return -Long.compare(au, bu);
                    // Then by deposit block id
                    return a.getKey().getDescriptionId().compareToIgnoreCase(b.getKey().getDescriptionId());
                })
                .forEachOrdered(e -> {
                    var usesComp = (e.getLongValue() > 0)
                            ? Component.literal(Long.toString(e.getLongValue()))
                            : CreateRNS.translatable("mining.infinite");
                    CreateRNS.lang()
                            .add(e.getKey().getName()
                                    .append(": ")
                                    .withStyle(ChatFormatting.GRAY))
                            .add(usesComp
                                    .withStyle(ChatFormatting.GREEN))
                            .forGoggles(tooltip, 1);
                });
        return true;
    }

    public static boolean addRatesToGoggleTooltip(Context c, List<Component> tooltip) {
        if (!(c.target() instanceof IMinerHolderBE holder)) return false;
        var miner = holder.getMiner();
        var process = miner.getProcess();
        if (process == null || !miner.isMining()) return false;

        if (c.isFirstSection()) {
            CreateRNS.lang().translate("mining.production_rates").forGoggles(tooltip);
        } else {
            // Newline between sections
            CreateRNS.lang().space().forGoggles(tooltip);
        }

        var rates = process.getEstimatedRates(miner.getCurrentProgressIncrement());
        for (var e : rates.entrySet()) {
            CreateRNS.lang()
                    .add(e.getKey().getDescription().copy()
                            .append(": ")
                            .withStyle(ChatFormatting.GRAY))
                    .add(CreateRNS.translatable("mining.per_hour", String.format(Locale.ROOT, "%.1f", e.getValue()))
                            .withStyle(ChatFormatting.GREEN))
                    .forGoggles(tooltip, 1);
        }
        addEstimationComponent(process.getRateEstimationStatus(), tooltip);

        return true;
    }

    public static boolean addMinerInfoToGoggleTooltip(Context c, List<Component> tooltip) {
        if (!(c.target() instanceof IMinerHolderBE holder)) return false;
        var miner = holder.getMiner();
        var process = miner.getProcess();
        var spec = miner.getSpec();
        if (process == null || spec == null) return false;

        if (c.isFirstSection()) {
            CreateRNS.lang()
                    .translate("contraption_mining.info")
                    .forGoggles(tooltip);
        } else {
            // Newline between sections
            CreateRNS.lang().space().forGoggles(tooltip);
        }

        CreateRNS.lang()
                .translate("mining.area", spec.miningDimensions().radius() * 2 + 1, spec.miningDimensions().length())
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        // Add descriptions contributed by active CRSes
        var activeCRSes = process.getLastSatisfiedCRSes();
        for (var crs : activeCRSes) {
            var comp = CatalystRequirementSet.getNameComponent(activeCRSes, crs);
            if (comp != null) {
                CreateRNS.lang().add(comp).forGoggles(tooltip);
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
                .add(CreateRNS.translatable("mining." + statusTag)
                        .withStyle(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);
    }
}

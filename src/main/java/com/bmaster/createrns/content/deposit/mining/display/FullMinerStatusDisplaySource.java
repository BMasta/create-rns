package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FullMinerStatusDisplaySource extends TwoColumnDisplaySource {
    @Override
    protected List<TwoColumnRow> provideRows(DisplayLinkContext context, DisplayTargetStats targetStats) {
        if (!(context.getSourceBlockEntity() instanceof MinerBearingBlockEntity bearing)) return List.of();

        var level = bearing.getLevel();
        if (level == null) return List.of();

        var behaviour = bearing.miningBehaviour;
        var stats = behaviour.stats;
        var process = behaviour.getProcess();
        var claimed = behaviour.getClaimedDepositBlocks();
        var style = Style.from(context.sourceConfig().getInt("style"));

        var res = new ArrayList<TwoColumnRow>();
        var status = MinerDisplaySourceFormatter.getStatus(bearing, behaviour);
        res.add(row("status", status));
        res.add(row("claimed", MinerDisplaySourceFormatter.formatCount((claimed != null) ? claimed.size() : 0)));
        var uptime = MinerDisplaySourceFormatter.formattedPercentage(
                stats.getUptimeTicks(), stats.getElapsedTicks(level), MinerDisplaySourceFormatter.NOT_COMPUTED);
        res.add(row("uptime", uptime));
        res.add(row("mined", MinerDisplaySourceFormatter.formatCount(stats.minedResources)));
        if (!ServerConfig.INFINITE_DEPOSITS.getAsBoolean()) {
            res.add(row("consumed", MinerDisplaySourceFormatter.formatCount(stats.consumedResources)));
            res.add(row("remaining", MinerDisplaySourceFormatter.formatCount(stats.getTotalRemainingResources())));
        }
        if (process != null && style == Style.WITH_CATALYSTS) {
            var activeCRSes = stats.getLastSatisfiedCRSes(process);
            int cnt = 0;
            for (int i = 0; i < activeCRSes.size(); ++i) {
                var crsName = CatalystRequirementSet.getNameComponent(activeCRSes, activeCRSes.get(i));
                if (crsName == null) continue;
                cnt++;
                res.add(new TwoColumnRow(
                        CreateRNS.translatable("display_source.full_miner_status.catalyst", cnt + ""),
                        Component.literal(ChatFormatting.stripFormatting(crsName.getString()))));
            }
        }

        return List.copyOf(res);
    }

    @Override
    public int getPassiveRefreshTicks() {
        return SharedConstants.TICKS_PER_SECOND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(
            DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine
    ) {
        super.initConfigurationWidgets(context, builder, isFirstLine);
        if (isFirstLine) return;

        builder.addSelectionScrollInput(0, 137, (input, label) -> input
                .forOptions(Arrays.stream(Style.values())
                        .map(Style::getName)
                        .toList())
                .titled(CreateRNS.translatable("display_source.full_miner_status.style")), "style");
    }

    private static TwoColumnRow row(@Nullable String name, Component value) {
        var key = (name != null)
                ? CreateRNS.translatable("display_source.full_miner_status." + name)
                : Component.empty();
        return new TwoColumnRow(key, value);
    }

    private enum Style {
        WITH_CATALYSTS("with_catalysts"),
        WITHOUT_CATALYSTS("without_catalysts");

        private final String translationKey;

        Style(String translationKey) {
            this.translationKey = translationKey;
        }

        public static Style from(int ordinal) {
            return values()[Mth.clamp(ordinal, 0, values().length - 1)];
        }

        public MutableComponent getName() {
            return CreateRNS.translatable("display_source.full_miner_status.style." + translationKey);
        }
    }
}

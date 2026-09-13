package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
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

        var res = new ArrayList<TwoColumnRow>();
        var status = MinerDisplaySourceFormatter.getStatus(bearing, behaviour);
        res.add(row("status", status));
        var uptime = MinerDisplaySourceFormatter.formattedPercentage(
                stats.getUptimeTicks(), stats.getElapsedTicks(level), MinerDisplaySourceFormatter.NOT_COMPUTED);
        res.add(row("uptime", uptime));
        res.add(row("mined", MinerDisplaySourceFormatter.formatCount(stats.minedResources)));
        if (!ServerConfig.INFINITE_DEPOSITS.getAsBoolean()) {
            res.add(row("consumed", MinerDisplaySourceFormatter.formatCount(stats.consumedResources)));
            res.add(row("remaining", MinerDisplaySourceFormatter.formatCount(stats.getTotalRemainingResources())));
        }

        return List.copyOf(res);
    }

    @Override
    public int getPassiveRefreshTicks() {
        return SharedConstants.TICKS_PER_SECOND;
    }

    private static TwoColumnRow row(String name, Component value) {
        return new TwoColumnRow(CreateRNS.translatable("display_source.full_miner_status." + name), value);
    }
}

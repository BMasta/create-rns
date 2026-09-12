package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FullMinerStatusDisplaySource extends DisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats targetStats) {
        if (!(context.getSourceBlockEntity() instanceof MinerBearingBlockEntity bearing)) return EMPTY;

        var level = bearing.getLevel();
        if (level == null) return EMPTY;

        var behaviour = bearing.miningBehaviour;
        var stats = behaviour.stats;

        var res = new ArrayList<MutableComponent>();
        res.add(CreateRNS.translatable("display_source.full_miner_status.status",
                MinerDisplaySourceFormatter.getStatus(bearing, behaviour)));
        res.add(CreateRNS.translatable("display_source.full_miner_status.uptime",
                MinerDisplaySourceFormatter.formattedPercentage(
                        stats.getUptimeTicks(), stats.getElapsedTicks(level),
                        MinerDisplaySourceFormatter.NOT_COMPUTED)));
        res.add(CreateRNS.translatable("display_source.full_miner_status.mined",
                MinerDisplaySourceFormatter.formatCount(stats.minedResources)));
        if (!ServerConfig.INFINITE_DEPOSITS.getAsBoolean()) {
            res.add(CreateRNS.translatable("display_source.full_miner_status.consumed",
                    MinerDisplaySourceFormatter.formatCount(stats.consumedResources)));
            res.add(CreateRNS.translatable("display_source.full_miner_status.remaining",
                    MinerDisplaySourceFormatter.formatCount(stats.getTotalRemainingResources())));
        }

        return List.copyOf(res);
    }

    @Override
    public int getPassiveRefreshTicks() {
        return SharedConstants.TICKS_PER_SECOND;
    }
}

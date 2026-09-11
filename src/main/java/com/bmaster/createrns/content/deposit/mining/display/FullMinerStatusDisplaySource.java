package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.ParametersAreNonnullByDefault;
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
        var claimedBlocks = behaviour.getClaimedDepositBlocks();
        boolean claimed = claimedBlocks != null && !claimedBlocks.isEmpty();
        int minedPercentage = MinerDisplaySourceFormatter.percentage(
                stats.consumedResources, stats.consumedResources + stats.remainingResources);
        int uptimePercentage = MinerDisplaySourceFormatter.percentage(
                stats.uptimeTicks, stats.elapsedTicks(level));

        return List.of(
                CreateRNS.translatable("display_source.full_miner_status.status",
                        MinerDisplaySourceFormatter.getStatus(bearing, behaviour)),
                CreateRNS.translatable("display_source.full_miner_status.uptime", uptimePercentage),
                claimed ?
                        CreateRNS.translatable("display_source.full_miner_status.mined_resources_with_percentage",
                        MinerDisplaySourceFormatter.formatResources(stats.minedResources), minedPercentage)
                        : CreateRNS.translatable("display_source.full_miner_status.mined_resources",
                        MinerDisplaySourceFormatter.formatResources(stats.minedResources)),
                claimed && stats.remainingResources == 0
                        ? CreateRNS.translatable("display_source.full_miner_status.infinite_resources")
                        : CreateRNS.translatable("display_source.full_miner_status.remaining_resources",
                        MinerDisplaySourceFormatter.formatResources(stats.remainingResources))
        );
    }

    @Override
    public int getPassiveRefreshTicks() {
        return SharedConstants.TICKS_PER_SECOND;
    }
}

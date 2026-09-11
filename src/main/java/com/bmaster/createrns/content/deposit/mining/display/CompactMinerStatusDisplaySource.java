package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.behaviour.MiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CompactMinerStatusDisplaySource extends SingleLineDisplaySource {
    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof MinerBearingBlockEntity bearing)) return EMPTY_LINE;

        var level = bearing.getLevel();
        if (level == null) return EMPTY_LINE;

        var status = MinerDisplaySourceFormatter.getStatus(bearing, bearing.miningBehaviour);
        var style = Style.from(context.sourceConfig().getInt("style"));
        if (style == Style.SIMPLE) return status;

        return status.append(" (")
                .append(getAdditionalMessage(level, bearing.miningBehaviour, style))
                .append(")");
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
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
                .titled(CreateRNS.translatable("display_source.compact_miner_status.style")), "style");
    }

    private static MutableComponent getAdditionalMessage(Level level, MiningBehaviour behaviour, Style style) {
        var stats = behaviour.stats;
        var claimedBlocks = behaviour.getClaimedDepositBlocks();
        boolean claimed = claimedBlocks != null && !claimedBlocks.isEmpty();
        return switch (style) {
            case SIMPLE -> Component.empty();
            case UPTIME -> CreateRNS.translatable(
                    "display_source.compact_miner_status.message.uptime",
                    MinerDisplaySourceFormatter.percentage(stats.uptimeTicks, stats.elapsedTicks(level)));
            case MINED_RESOURCES -> CreateRNS.translatable(
                    "display_source.compact_miner_status.message.mined_resources",
                    MinerDisplaySourceFormatter.formatResources(stats.minedResources));
            case REMAINING_RESOURCES -> claimed && stats.remainingResources == 0
                    ? CreateRNS.translatable(
                    "display_source.compact_miner_status.message.infinite_resources")
                    : CreateRNS.translatable(
                    "display_source.compact_miner_status.message.remaining_resources",
                    MinerDisplaySourceFormatter.formatResources(stats.remainingResources));
            case MINED_PERCENTAGE -> CreateRNS.translatable(
                    "display_source.compact_miner_status.message.mined_percentage",
                    MinerDisplaySourceFormatter.percentage(
                            stats.consumedResources, stats.consumedResources + stats.remainingResources));
        };
    }

    private enum Style {
        SIMPLE("simple"),
        UPTIME("uptime"),
        MINED_RESOURCES("mined_resources"),
        REMAINING_RESOURCES("remaining_resources"),
        MINED_PERCENTAGE("mined_percentage");

        private final String translationKey;

        Style(String translationKey) {
            this.translationKey = translationKey;
        }

        public static Style from(int ordinal) {
            return values()[Mth.clamp(ordinal, 0, values().length - 1)];
        }

        public MutableComponent getName() {
            return CreateRNS.translatable("display_source.compact_miner_status.style." + translationKey);
        }
    }
}

package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.behaviour.MiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.bmaster.createrns.infrastructure.ServerConfig;
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
        if (style == Style.SIMPLE ||
                ServerConfig.INFINITE_DEPOSITS.get() && style.hideWhenDepositsInfinite) return status;

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
        return switch (style) {
            case SIMPLE -> Component.empty();
            case UPTIME -> CreateRNS.translatable(
                    "display_source.compact_miner_status.message.uptime",
                    MinerDisplaySourceFormatter.formattedPercentage(
                            stats.getUptimeTicks(), stats.getElapsedTicks(level),
                            MinerDisplaySourceFormatter.NOT_COMPUTED));
            case MINED -> CreateRNS.translatable(
                    "display_source.compact_miner_status.message.mined",
                    MinerDisplaySourceFormatter.formatCount(stats.minedResources));
            case CONSUMED -> CreateRNS.translatable(
                    "display_source.compact_miner_status.message.consumed",
                    MinerDisplaySourceFormatter.formatCount(stats.consumedResources));
            case REMAINING -> CreateRNS.translatable(
                    "display_source.compact_miner_status.message.remaining",
                    MinerDisplaySourceFormatter.formatCount(stats.getTotalRemainingResources()));
        };
    }

    private enum Style {
        SIMPLE("simple", false),
        UPTIME("uptime", false),
        MINED("mined", false),
        CONSUMED("consumed", true),
        REMAINING("remaining", true);

        private final String translationKey;
        private final boolean hideWhenDepositsInfinite;

        Style(String translationKey, boolean hideWhenDepositsInfinite) {
            this.translationKey = translationKey;
            this.hideWhenDepositsInfinite = hideWhenDepositsInfinite;
        }

        public static Style from(int ordinal) {
            return values()[Mth.clamp(ordinal, 0, values().length - 1)];
        }

        public MutableComponent getName() {
            return CreateRNS.translatable("display_source.compact_miner_status.style." + translationKey);
        }
    }
}

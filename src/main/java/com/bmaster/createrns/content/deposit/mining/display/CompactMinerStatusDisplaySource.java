package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.behaviour.MiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.contraption.ContraptionMiningBehaviour;
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
import java.util.Locale;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CompactMinerStatusDisplaySource extends SingleLineDisplaySource {
    private static final long THOUSAND = 1_000;
    private static final long MILLION = 1_000_000;

    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof MinerBearingBlockEntity bearing)) return EMPTY_LINE;

        var level = bearing.getLevel();
        if (level == null) return EMPTY_LINE;

        var status = getStatus(bearing, bearing.miningBehaviour);
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

    private static MutableComponent getStatus(
            MinerBearingBlockEntity bearing, ContraptionMiningBehaviour behaviour
    ) {
        if (!bearing.isRunning() || bearing.getMovedContraption() == null) {
            return CreateRNS.translatable("display_source.compact_miner_status.status.not_assembled");
        }
        var claimedBlocks = behaviour.getClaimedDepositBlocks();
        if (claimedBlocks == null || claimedBlocks.isEmpty()) {
            return CreateRNS.translatable("display_source.compact_miner_status.status.out_of_resources");
        }
        if (bearing.getSpeed() != 0 && !bearing.isSpeedRequirementFulfilled()) {
            return CreateRNS.translatable("display_source.compact_miner_status.status.too_slow");
        }
        if (behaviour.isTransferring()) {
            return CreateRNS.translatable("display_source.compact_miner_status.status.transferring");
        }
        if (!behaviour.isMining()) {
            return CreateRNS.translatable("display_source.compact_miner_status.status.stalled");
        }
        return CreateRNS.translatable("display_source.compact_miner_status.status.mining");
    }

    private static MutableComponent getAdditionalMessage(Level level, MiningBehaviour behaviour, Style style) {
        var stats = behaviour.stats;
        return switch (style) {
            case SIMPLE -> Component.empty();
            case UPTIME -> CreateRNS.translatable(
                    "display_source.compact_miner_status.message.uptime",
                    percentage(stats.uptimeTicks, stats.elapsedTicks(level)));
            case MINED_RESOURCES -> CreateRNS.translatable(
                    "display_source.compact_miner_status.message.mined_resources",
                    formatResources(stats.minedResources));
            case REMAINING_RESOURCES -> CreateRNS.translatable(
                    "display_source.compact_miner_status.message.remaining_resources",
                    formatResources(stats.remainingResources));
            case MINED_PERCENTAGE -> CreateRNS.translatable(
                    "display_source.compact_miner_status.message.mined_percentage",
                    percentage(stats.consumedResources, stats.consumedResources + stats.remainingResources));
        };
    }

    private static MutableComponent formatResources(long resources) {
        if (resources < THOUSAND) return Component.literal(Long.toString(resources));

        double scaled = resources / (double) (resources < MILLION ? THOUSAND : MILLION);
        scaled = Math.floor(scaled * 10) / 10;
        String suffix = resources < MILLION ? "k" : "m";
        return Component.literal(String.format(Locale.ROOT, "%.1f%s", scaled, suffix));
    }

    private static int percentage(long current, long total) {
        if (total == 0) return 0;
        return Mth.clamp((int) Math.round(current * 100d / total), 0, 100);
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

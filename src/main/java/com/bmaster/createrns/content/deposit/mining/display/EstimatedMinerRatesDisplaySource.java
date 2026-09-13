package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EstimatedMinerRatesDisplaySource extends TwoColumnDisplaySource {
    @Override
    protected int rightColumnWidth() {
        return 12;
    }

    @Override
    protected List<TwoColumnRow> provideRows(DisplayLinkContext context, DisplayTargetStats targetStats) {
        if (!(context.getSourceBlockEntity() instanceof MinerBearingBlockEntity bearing)) return List.of();

        var behaviour = bearing.miningBehaviour;

        var process = behaviour.getProcess();
        if (process == null) return List.of();

        var style = Style.from(context.sourceConfig().getInt("style"));

        var increment = behaviour.isMining() ? behaviour.getCurrentProgressIncrement() : 0;
        var rates = process.getEstimatedRates(increment);
        var entries = rates.entrySet().stream()
                .limit(targetStats.maxRows())
                .toList();
        if (entries.isEmpty()) return List.of();

        var lines = new ArrayList<TwoColumnRow>(entries.size());

        int nRows = Math.min(targetStats.maxRows(), entries.size());
        for (int i = 0; i < nRows; ++i) {
            var e = entries.get(i);
            var key = e.getKey().getDescription();
            var value = CreateRNS.translatable("mining.per_hour",
                    String.format(Locale.ROOT, "%.1f", e.getValue()));
            lines.add(new TwoColumnRow(key, value));
        }
        return lines;
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
        if (!isFirstLine) return;

        builder.addSelectionScrollInput(0, 137, (input, label) -> input
                .forOptions(Arrays.stream(Style.values())
                        .map(Style::getName)
                        .toList())
                .titled(CreateRNS.translatable("display_source.estimated_miner_rates.style")), "style");
    }

    private enum Style {
        DEFAULT("default");

        private final String translationKey;

        Style(String translationKey) {
            this.translationKey = translationKey;
        }

        public static Style from(int ordinal) {
            return values()[Mth.clamp(ordinal, 0, values().length - 1)];
        }

        public MutableComponent getName() {
            return CreateRNS.translatable("display_source.estimated_miner_rates.style." + translationKey);
        }
    }
}

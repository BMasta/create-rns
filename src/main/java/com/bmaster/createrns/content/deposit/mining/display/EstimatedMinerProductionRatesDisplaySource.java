package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EstimatedMinerProductionRatesDisplaySource extends DisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats targetStats) {
        if (!(context.getSourceBlockEntity() instanceof MinerBearingBlockEntity bearing)) return EMPTY;

        var behaviour = bearing.miningBehaviour;

        var process = behaviour.getProcess();
        if (process == null) return EMPTY;

        var style = Style.from(context.sourceConfig().getInt("style"));
        int maxEntries = targetStats.maxRows() / style.rowsPerEntry();
        if (maxEntries == 0) return EMPTY;

        var increment = behaviour.isMining() ? behaviour.getCurrentProgressIncrement() : 0;
        var rates = process.getEstimatedRates(increment);
        var entries = rates.object2FloatEntrySet().stream()
                .sorted(EstimatedMinerProductionRatesDisplaySource::compareRates)
                .limit(maxEntries)
                .toList();
        if (entries.isEmpty()) return EMPTY;

        var lines = new ArrayList<MutableComponent>(entries.size() * style.rowsPerEntry());
        for (var entry : entries) {
            lines.add(formatEntry(entry, style.itemNames()));
            if (style.skipLine()) lines.add(EMPTY_LINE);
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
                .titled(CreateRNS.translatable("display_source.estimated_miner_production_rates.style")), "style");
    }

    private static int compareRates(Object2FloatMap.Entry<Item> first, Object2FloatMap.Entry<Item> second) {
        float firstRate = first.getFloatValue();
        float secondRate = second.getFloatValue();
        if (firstRate != secondRate) return -Float.compare(firstRate, secondRate);

        var firstId = ForgeRegistries.ITEMS.getKey(first.getKey());
        var secondId = ForgeRegistries.ITEMS.getKey(second.getKey());
        if (firstId == null) return secondId == null ? 0 : 1;
        if (secondId == null) return -1;
        return firstId.toString().compareToIgnoreCase(secondId.toString());
    }

    private static MutableComponent formatEntry(Object2FloatMap.Entry<Item> entry, boolean itemNames) {
        var line = Component.literal(String.format(Locale.ROOT, "%.1f", entry.getFloatValue()));
        if (itemNames) line.append(" ").append(entry.getKey().getDescription());
        return line.append(CreateRNS.translatable("mining.per_hour"));
    }

    private enum Style {
        ONE_PER_LINE("one_per_line", false, false),
        ONE_PER_LINE_WITH_NAMES("one_per_line_with_names", true, false),
        SKIP_LINE("skip_line", false, true),
        SKIP_LINE_WITH_NAMES("skip_line_with_names", true, true);

        private final String translationKey;
        private final boolean itemNames;
        private final boolean skipLine;

        Style(String translationKey, boolean itemNames, boolean skipLine) {
            this.translationKey = translationKey;
            this.itemNames = itemNames;
            this.skipLine = skipLine;
        }

        public static Style from(int ordinal) {
            return values()[Mth.clamp(ordinal, 0, values().length - 1)];
        }

        public MutableComponent getName() {
            return CreateRNS.translatable("display_source.estimated_miner_production_rates.style." + translationKey);
        }

        public boolean itemNames() {
            return itemNames;
        }

        public boolean skipLine() {
            return skipLine;
        }

        public int rowsPerEntry() {
            return skipLine ? 2 : 1;
        }
    }
}

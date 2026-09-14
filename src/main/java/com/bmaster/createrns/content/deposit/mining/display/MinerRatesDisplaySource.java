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
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MinerRatesDisplaySource extends TwoColumnDisplaySource {
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

        var type = Type.from(context.sourceConfig().getInt("type"));

        var rates = new ArrayList<>(type.getRates(bearing).sequencedEntrySet());
        int nRows = Math.min(targetStats.maxRows(), rates.size());
        var lines = new ArrayList<TwoColumnRow>(nRows);
        for (int i = 0; i < nRows; ++i) {
            var e = rates.get(i);
            var key = e.getKey().getDescription();
            var value = CreateRNS.translatable("mining.per_hour",
                    String.format(Locale.ROOT, "%.1f", e.getValue()));
            lines.add(new TwoColumnRow(key, value));
        }
        return lines;
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 3 * SharedConstants.TICKS_PER_SECOND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(
            DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine
    ) {
        if (!isFirstLine) return;

        builder.addSelectionScrollInput(0, 137, (input, label) -> input
                .forOptions(Arrays.stream(Type.values())
                        .map(Type::getName)
                        .toList())
                .titled(CreateRNS.translatable("display_source.miner_rates.type")), "type");
    }

    private enum Type {
        ESTIMATED("estimated", Type::getEstimatedRates),
        ACTUAL("actual", Type::getActualRates);

        private static LinkedHashMap<Item, Float> getEstimatedRates(MinerBearingBlockEntity bearing) {
            var behaviour =  bearing.miningBehaviour;
            var process = bearing.miningBehaviour.getProcess();
            if (process == null) return new LinkedHashMap<>();
            var stats = behaviour.stats;
            int increment = behaviour.isMining() ? behaviour.getCurrentProgressIncrement() : 0;
            return stats.getEstimatedRates(process, increment);
        }

        private static LinkedHashMap<Item, Float> getActualRates(MinerBearingBlockEntity bearing) {
            var process = bearing.miningBehaviour.getProcess();
            if (process == null) return new LinkedHashMap<>();
            return bearing.miningBehaviour.stats.getRates(process);
        }

        private final String translationKey;
        private final Function<MinerBearingBlockEntity, LinkedHashMap<Item, Float>> ratesGetter;

        Type(String translationKey,  Function<MinerBearingBlockEntity, LinkedHashMap<Item, Float>> ratesGetter) {
            this.translationKey = translationKey;
            this.ratesGetter = ratesGetter;
        }

        public static Type from(int ordinal) {
            return values()[Mth.clamp(ordinal, 0, values().length - 1)];
        }

        public MutableComponent getName() {
            return CreateRNS.translatable("display_source.miner_rates.type." + translationKey);
        }

        public LinkedHashMap<Item, Float> getRates(MinerBearingBlockEntity bearing) {
            return ratesGetter.apply(bearing);
        }
    }
}

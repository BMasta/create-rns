package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RemainingMinerResourcesDisplaySource extends TwoColumnDisplaySource {
    @Override
    protected int rightColumnWidth() {
        return 7;
    }

    @Override
    protected List<TwoColumnRow> provideRows(DisplayLinkContext context, DisplayTargetStats targetStats) {
        if (!(context.getSourceBlockEntity() instanceof MinerBearingBlockEntity bearing)) return List.of();
        var style = Style.from(context.sourceConfig().getInt("style"));
        boolean infinite = ServerConfig.INFINITE_DEPOSITS.get();

        var entries = bearing.miningBehaviour.stats.remainingResources.object2LongEntrySet().stream()
                .sorted((first, second) -> compareResources(first, second, infinite))
                .limit(targetStats.maxRows())
                .toList();
        if (entries.isEmpty()) return List.of();

        var lines = new ArrayList<TwoColumnRow>(entries.size());
        for (var e : entries) {
            var resources = e.getLongValue();
            var key = e.getKey().getName();
            var value = infinite || resources == 0
                    ? MinerDisplaySourceFormatter.INFINITE
                    : MinerDisplaySourceFormatter.formatCount(resources);
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
                .forOptions(Arrays.stream(Style.values())
                        .map(Style::getName)
                        .toList())
                .titled(CreateRNS.translatable("display_source.remaining_miner_resources.style")), "style");
    }

    private static int compareResources(
            Object2LongMap.Entry<Block> first, Object2LongMap.Entry<Block> second, boolean infinite
    ) {
        long firstResources = first.getLongValue();
        long secondResources = second.getLongValue();
        boolean firstInfinite = infinite || firstResources == 0;
        boolean secondInfinite = infinite || secondResources == 0;
        if (firstInfinite != secondInfinite) return firstInfinite ? 1 : -1;
        if (!firstInfinite && firstResources != secondResources) return -Long.compare(firstResources, secondResources);

        var firstId = ForgeRegistries.BLOCKS.getKey(first.getKey());
        var secondId = ForgeRegistries.BLOCKS.getKey(second.getKey());
        if (firstId == null) return secondId == null ? 0 : 1;
        if (secondId == null) return -1;
        return firstId.toString().compareToIgnoreCase(secondId.toString());
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
            return CreateRNS.translatable("display_source.remaining_miner_resources.style." + translationKey);
        }
    }
}

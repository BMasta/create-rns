package com.bmaster.createrns.content.deposit.mining.display;

import com.bmaster.createrns.CreateRNS;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayLayout;
import com.simibubi.create.content.trains.display.FlapDisplaySection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
abstract class TwoColumnDisplaySource extends DisplaySource {
    private static final String LAYOUT_KEY = CreateRNS.ID + ":two_column";

    protected int leftColumnWidth() {
        return -1;
    }

    protected int rightColumnWidth() {
        return -1;
    }

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats targetStats) {
        var entries = provideRows(context, targetStats);
        if (entries.isEmpty()) return EMPTY;

        return entries.stream()
                .map(e -> CreateRNS.translatable("display_source.key_value_format", e.left(), e.right()))
                .toList();
    }

    @Override
    public List<List<MutableComponent>> provideFlapDisplayText(
            DisplayLinkContext context, DisplayTargetStats targetStats
    ) {
        var entries = provideRows(context, targetStats);
        if (entries.isEmpty()) {
            context.flapDisplayContext = false;
            return List.of(List.of(EMPTY_LINE));
        }
        context.flapDisplayContext = true;
        return entries.stream()
                .map(twoColumnRow -> List.of(twoColumnRow.left().copy(), twoColumnRow.right().copy()))
                .toList();
    }

    @Override
    public void loadFlapDisplayLayout(
            DisplayLinkContext context, FlapDisplayBlockEntity flapDisplay, FlapDisplayLayout layout
    ) {
        if (!Boolean.TRUE.equals(context.flapDisplayContext)) {
            if (!layout.isLayout("Default")) layout.loadDefault(flapDisplay.getMaxCharCount());
            return;
        }

        int totalWidth = flapDisplay.getMaxCharCount(1);
        int lWidth = leftColumnWidth();
        int rWidth = rightColumnWidth();
        if ((lWidth < 0 && rWidth < 0) || (lWidth == 0 && rWidth == 0)) {
            // When neither width is specified or both are zero, divide the board in half
            lWidth = totalWidth / 2;
            rWidth = totalWidth - lWidth;
        } else if (lWidth < 0) {
            // When right width is specified, left width gets what is left
            if (rWidth > totalWidth) rWidth = totalWidth;
            lWidth = totalWidth - rWidth;
        } else if (rWidth < 0) {
            // When left width is specified, right width gets what is left
            if (lWidth > totalWidth) lWidth = totalWidth;
            rWidth = totalWidth - lWidth;
        } else {
            // When both widths are specified, treat them as a ratio and adjust to the total width
            lWidth = (int) (lWidth * ((float) totalWidth / (lWidth + rWidth)));
            rWidth = totalWidth - lWidth;
        }

        String layoutKey = LAYOUT_KEY + lWidth + "_" + rWidth;
        if (layout.isLayout(layoutKey)) return;

        var leftSection = new FlapDisplaySection(
                lWidth * FlapDisplaySection.MONOSPACE, "alphabet", false,
                lWidth > 0 && rWidth > 0);
        var rightSection = new FlapDisplaySection(
                rWidth * FlapDisplaySection.MONOSPACE, "alphabet", false, false)
                .rightAligned();
        layout.configure(layoutKey, List.of(leftSection, rightSection));
    }

    protected abstract List<TwoColumnRow> provideRows(DisplayLinkContext context, DisplayTargetStats targetStats);

    protected record TwoColumnRow(Component left, Component right) {
    }
}

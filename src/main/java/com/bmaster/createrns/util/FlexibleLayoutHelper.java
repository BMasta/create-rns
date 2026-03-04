package com.bmaster.createrns.util;

import net.createmod.catnip.layout.LayoutHelper;

public class FlexibleLayoutHelper implements LayoutHelper {
    public enum Anchor {
        START, CENTER, END
    }

    int itemCount;
    int rows;
    int width;
    int height;
    int spacing;

    int currentColumn = 0;
    int currentRow = 0;
    int[] rowCounts;
    int x = 0, y = 0;

    Anchor xAnchor;
    Anchor yAnchor;

    public FlexibleLayoutHelper(int itemCount, int rows, int width, int height, int spacing,
                                Anchor xAnchor, Anchor yAnchor) {
        this.itemCount = itemCount;
        this.rows = rows;
        this.width = width;
        this.height = height;
        this.spacing = spacing;
        this.xAnchor = xAnchor;
        this.yAnchor = yAnchor;

        rowCounts = new int[rows];
        int itemsPerRow = itemCount / rows;
        int itemDiff = itemCount - itemsPerRow * rows;
        for (int i = 0; i < rows; i++) {
            rowCounts[i] = itemsPerRow;
            if (itemDiff > 0) {
                rowCounts[i]++;
                itemDiff--;
            }
        }

        init();
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void next() {
        currentColumn++;
        if (currentColumn >= rowCounts[currentRow]) {
            // nextRow
            if (++currentRow >= rows) {
                x = 0;
                y = 0;
                return;
            }

            currentColumn = 0;
            prepareX();
            y += height + spacing;
            return;
        }

        x += width + spacing;
    }

    private void init() {
        prepareX();
        prepareY();
    }

    private void prepareX() {
        int rowWidth = rowCounts[currentRow] * width + (rowCounts[currentRow] - 1) * spacing;
        x = switch (xAnchor) {
            case START -> 0;
            case CENTER -> -(rowWidth / 2);
            case END -> -rowWidth;
        };
    }

    private void prepareY() {
        int totalHeight = rows * height + (rows > 1 ? ((rows - 1) * spacing) : 0);
        y = switch (yAnchor) {
            case START -> 0;
            case CENTER -> -(totalHeight / 2);
            case END -> -totalHeight;
        };
    }

    @Override
    public int getTotalWidth() {
        return rowCounts[0] * width + (rowCounts[0] - 1) * spacing;
    }

    @Override
    public int getTotalHeight() {
        return rows * height + (rows > 1 ? ((rows - 1) * spacing) : 0);
    }

}

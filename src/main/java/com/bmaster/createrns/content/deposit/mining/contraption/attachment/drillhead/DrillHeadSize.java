package com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.StringRepresentable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Locale;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public enum DrillHeadSize implements StringRepresentable {
    SMALL,
    MEDIUM,
    LARGE;

    public DrillHeadSize getNext() {
        return switch (this) {
            case SMALL -> MEDIUM;
            case MEDIUM, LARGE -> LARGE;
        };
    }

    public boolean canGrow() {
        return this != LARGE;
    }

    public int getDrillHeadCost() {
        return switch (this) {
            case SMALL -> 1;
            case MEDIUM -> 2;
            case LARGE -> 3;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

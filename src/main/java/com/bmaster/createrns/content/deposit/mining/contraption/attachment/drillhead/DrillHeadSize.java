package com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.StringRepresentable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Locale;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public enum DrillHeadSize implements StringRepresentable {
    SMALL,
    LARGE;

    @SuppressWarnings("SameReturnValue")
    public DrillHeadSize getNext() {
        return switch (this) {
            case SMALL, LARGE -> LARGE;
        };
    }

    public boolean canGrow() {
        return this != LARGE;
    }

    public int getRadiusBonus() {
        return switch (this) {
            case SMALL -> 0;
            case LARGE -> 1;
        };
    }

    public int getTipOffset() {
        return switch (this) {
            case SMALL -> 0;
            case LARGE -> 1;
        };
    }

    public int getDrillHeadCost() {
        return switch (this) {
            case SMALL -> 1;
            case LARGE -> 2;
        };
    }

    public float getModelScale() {
        return switch (this) {
            case SMALL -> 1f;
            case LARGE -> 2f;
        };
    }

    public float getModelOffset() {
        return switch (this) {
            case SMALL -> 0f;
            case LARGE -> 0.248f;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

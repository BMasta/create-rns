package com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.StringRepresentable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Locale;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public enum DrillHeadSize implements StringRepresentable {
    SMALL,
    MEDIUM;

    @SuppressWarnings("SameReturnValue")
    public DrillHeadSize getNext() {
        return switch (this) {
            case SMALL, MEDIUM -> MEDIUM;
        };
    }

    public boolean canGrow() {
        return this != MEDIUM;
    }

    public int getDrillHeadCost() {
        return switch (this) {
            case SMALL -> 1;
            case MEDIUM -> 2;
        };
    }

    public float getModelScale() {
        return switch (this) {
            case SMALL -> 1f;
            case MEDIUM -> 2f;
        };
    }

    public float getModelOffset() {
        return switch (this) {
            case SMALL -> 0f;
            case MEDIUM -> 0.248f;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

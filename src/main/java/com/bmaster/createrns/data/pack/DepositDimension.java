package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.content.deposit.worldgen.PlacementStrategy;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum DepositDimension implements StringRepresentable {
    OVERWORLD(Level.OVERWORLD, PlacementStrategy.OVERWORLD, "overworld"),
    NETHER(Level.NETHER, PlacementStrategy.NETHER, "nether");

    private final ResourceKey<Level> levelDimension;
    private final PlacementStrategy placement;
    private final String keyword;

    DepositDimension(ResourceKey<Level> levelDimension, PlacementStrategy placement, String keyword) {
        this.levelDimension = levelDimension;
        this.placement = placement;
        this.keyword = keyword;
    }

    public String prefix() {
        return (this == OVERWORLD) ? "" : keyword + "_";
    }

    public String suffix() {
        return (this == OVERWORLD) ? "" : "_" + keyword;
    }

    public PlacementStrategy placement() {
        return placement;
    }

    public ResourceKey<Level> levelDimension() {
        return levelDimension;
    }

    @Override
    public String getSerializedName() {
        return keyword;
    }
}

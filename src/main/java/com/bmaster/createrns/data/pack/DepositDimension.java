package com.bmaster.createrns.data.pack;

import com.bmaster.createrns.content.deposit.worldgen.PlacementStrategy;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum DepositDimension implements StringRepresentable {
    OVERWORLD(ResourceLocation.withDefaultNamespace("overworld"), PlacementStrategy.OVERWORLD, "overworld"),
    NETHER(ResourceLocation.withDefaultNamespace("the_nether"), PlacementStrategy.NETHER, "nether");

    private final ResourceLocation levelDimension;
    private final PlacementStrategy placement;
    private final String keyword;

    DepositDimension(ResourceLocation levelDimension, PlacementStrategy placement, String keyword) {
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

    public ResourceLocation levelDimension() {
        return levelDimension;
    }

    @Override
    public String getSerializedName() {
        return keyword;
    }
}

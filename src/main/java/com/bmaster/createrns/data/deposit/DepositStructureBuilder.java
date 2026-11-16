package com.bmaster.createrns.data.deposit;

import com.bmaster.createrns.data.deposit.DepositBlockBuilder.DepositBuildingContext;
import com.bmaster.createrns.data.deposit.DepositData.StructurePreset;
import com.bmaster.createrns.data.deposit.DepositData.WeightedTemplate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureBuilder {
    public static DepositStructureBuilder create(DepositBuildingContext ctx) {
        return new DepositStructureBuilder(ctx);
    }

    public ResourceKey<Level> dimension = Level.OVERWORLD;
    public int depth = 8;
    public int depthDeviation = 0;
    public int weight = 2;
    public final List<WeightedTemplate> templates = new ArrayList<>();
    public final List<String> scannerIconItemCandidates = new ArrayList<>();

    public DepositStructureBuilder dimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
        return this;
    }

    public DepositStructureBuilder depth(int depth) {
        this.depth = depth;
        return this;
    }

    public DepositStructureBuilder depthDeviation(int delta) {
        this.depthDeviation = delta;
        return this;
    }

    public DepositStructureBuilder weight(int weight) {
        if (weight <= 0) throw new IllegalArgumentException("Deposit weight must be positive");
        this.weight = weight;
        return this;
    }

    public DepositStructureBuilder nbt(ResourceLocation template, int weight) {
        if (weight <= 0) throw new IllegalArgumentException("Template weight must be positive");
        templates.add(new WeightedTemplate(template, weight));
        return this;
    }

    public DepositStructureBuilder preset(StructurePreset preset) {
        dimension(preset.dimension());
        depth(preset.depth());
        depthDeviation(preset.depthDeviation());
        weight(preset.weight());
        for (var template : preset.templates()) {
            nbt(template.template(), template.weight());
        }
        return this;
    }

    public DepositStructureBuilder scannerIcon(String candidateId) {
        scannerIconItemCandidates.add(candidateId);
        return this;
    }

    public DepositStructureBuilder scannerIconMetal(String material) {
        scannerIconItemCandidates.addAll(DepositData.metalScannerIconCandidates(material));
        return this;
    }

    public DepositStructureBuilder scannerIconGem(String dustKeyword) {
        scannerIconItemCandidates.addAll(DepositData.gemScannerIconCandidates(dustKeyword));
        return this;
    }

    public DepositStructureBuilder scannerIconDust(String material) {
        scannerIconItemCandidates.addAll(DepositData.dustScannerIconCandidates(material));
        return this;
    }

    public DepositStructureBuilder scannerIconCoal() {
        scannerIconItemCandidates.addAll(DepositData.coalScannerIconCandidates());
        return this;
    }

    private DepositStructureBuilder(DepositBuildingContext ctx) {
        ctx.structureBuilders.add(this);
    }
}

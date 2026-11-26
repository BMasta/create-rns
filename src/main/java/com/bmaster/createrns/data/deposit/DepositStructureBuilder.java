package com.bmaster.createrns.data.deposit;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.data.deposit.DepositBlockBuilder.DepositBuildingContext;
import com.bmaster.createrns.data.deposit.DepositResources.StructurePreset;
import com.bmaster.createrns.data.deposit.DepositResources.WeightedTemplate;
import com.bmaster.createrns.util.codec.ResourceOrTagLocation;
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
    public final List<ResourceOrTagLocation> scannerIconItemCandidates = new ArrayList<>();

    private final DepositBlockBuilder.DepositBuildingContext ctx;

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
        scannerIconItemCandidates.add(ResourceOrTagLocation.parse(candidateId));
        return this;
    }

    public DepositStructureBuilder scannerIconMetal(String material) {
        scannerIconItemCandidates.addAll(DepositResources.metalScannerIconCandidates(material));
        return this;
    }

    public DepositStructureBuilder scannerIconGem(String dustKeyword) {
        scannerIconItemCandidates.addAll(DepositResources.gemScannerIconCandidates(dustKeyword));
        return this;
    }

    public DepositStructureBuilder scannerIconDust(String material) {
        scannerIconItemCandidates.addAll(DepositResources.dustScannerIconCandidates(material));
        return this;
    }

    public DepositStructureBuilder scannerIconCoal() {
        scannerIconItemCandidates.addAll(DepositResources.coalScannerIconCandidates());
        return this;
    }

    public DepositStructureBuilder scannerIconDepositBlock() {
        scannerIconItemCandidates.add(ResourceOrTagLocation.ofResource(
                DepositResources.blockId(CreateRNS.ID, ctx.depositKeyword)));
        return this;
    }


    private DepositStructureBuilder(DepositBuildingContext ctx) {
        this.ctx = ctx;
        this.ctx.structureBuilders.add(this);
    }
}

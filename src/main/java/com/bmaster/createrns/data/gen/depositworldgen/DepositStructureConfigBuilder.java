package com.bmaster.createrns.data.gen.depositworldgen;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class DepositStructureConfigBuilder {
    private final String name;
    private ResourceLocation depositBlock = null;
    private List<DepositWorldgenProvider.NBT> nbts = new ArrayList<>();
    int depth = 8;
    int weight = 2;

    public static DepositStructureConfigBuilder create(String name) {
        return new DepositStructureConfigBuilder(name);
    }

    public DepositStructureConfigBuilder depositBlock(ResourceLocation depositBlock) {
        this.depositBlock = depositBlock;
        return this;
    }

    public DepositStructureConfigBuilder nbt(ResourceLocation loc, int weight) {
        assert weight > 0;
        nbts.add(new DepositWorldgenProvider.NBT(loc, weight));
        return this;
    }

    public DepositStructureConfigBuilder depth(int depth) {
        this.depth = depth;
        return this;
    }

    public DepositStructureConfigBuilder weight(int weight) {
        assert weight > 0;
        this.weight = weight;
        return this;
    }

    public void save() {
        assert name != null;
        assert depositBlock != null;
        assert !nbts.isEmpty();

        DepositWorldgenProvider.depConf.add(new DepositWorldgenProvider.Deposit(
                name, depositBlock, nbts, depth, weight
        ));
    }

    private DepositStructureConfigBuilder(String name) {
        this.name = name;
    }
}

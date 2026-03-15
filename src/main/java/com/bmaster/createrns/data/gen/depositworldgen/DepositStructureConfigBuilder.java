package com.bmaster.createrns.data.gen.depositworldgen;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositStructureConfigBuilder {
    public static final ResourceLocation DEP_SMALL =
            CreateRNS.asResource("ore_deposit_small");
    public static final ResourceLocation DEP_MEDIUM =
            CreateRNS.asResource("ore_deposit_medium");
    public static final ResourceLocation DEP_LARGE =
            CreateRNS.asResource("ore_deposit_large");

    private final String name;
    private final List<DepositWorldgenProvider.NBT> nbts = new ArrayList<>();
    private ResourceLocation depositBlock = null;
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

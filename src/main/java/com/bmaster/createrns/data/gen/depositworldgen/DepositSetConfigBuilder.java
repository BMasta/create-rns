package com.bmaster.createrns.data.gen.depositworldgen;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public class DepositSetConfigBuilder {
    public static final int DEFAULT_SALT = 591646342;
    public static final int DEFAULT_SPACING = 48;

    private int separation = 8;
    private int spacing = DEFAULT_SPACING;
    private int salt = DEFAULT_SALT;
    private final List<Either<TagKey<Biome>,ResourceLocation>> allowedBiomes = new ArrayList<>();

    public static DepositSetConfigBuilder create() {
        return new DepositSetConfigBuilder();
    }

    public DepositSetConfigBuilder separation(int separation) {
        this.separation = separation;
        return this;
    }

    public DepositSetConfigBuilder spacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    public DepositSetConfigBuilder salt(int salt) {
        this.salt = salt;
        return this;
    }

    public DepositSetConfigBuilder biome(TagKey<Biome> biomeTK) {
        this.allowedBiomes.add(Either.left(biomeTK));
        return this;
    }

    public DepositSetConfigBuilder biome(ResourceLocation biomeRL) {
        this.allowedBiomes.add(Either.right(biomeRL));
        return this;
    }

    public DepositSetConfigBuilder biome(String biomeStr) {
        if (biomeStr.startsWith("#")) {
            return biome(TagKey.create(Registries.BIOME, ResourceLocation.parse(biomeStr.substring(1))));
        } else {
            return biome(ResourceLocation.parse(biomeStr));
        }
    }

    public void save() {
        assert !allowedBiomes.isEmpty();
        DepositWorldgenProvider.setConf = new DepositWorldgenProvider.DepositSet(separation, spacing, salt, allowedBiomes);
    }
}

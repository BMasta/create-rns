package com.bmaster.createrns.compat.kubejs.deposit.structures;

import com.bmaster.createrns.compat.kubejs.util.JSMethod;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class CreateStructureKubeBuilder extends StructureKubeBuilder<CreateStructureKubeBuilder> {
    public CreateStructureKubeBuilder(ResourceLocation id) {
        super(id);
    }

    @Info("Sets the deposit block used by this deposit structure.")
    @JSMethod
    public CreateStructureKubeBuilder block(String blockId) {
        var parsed = ResourceLocation.parse(blockId);
        if (!BuiltInRegistries.BLOCK.containsKey(parsed)) {
            error("Unknown block: " + blockId, true);
            return this;
        }
        this.blockId = parsed;
        return this;
    }

    void setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    @Override
    protected CreateStructureKubeBuilder self() {
        return this;
    }
}

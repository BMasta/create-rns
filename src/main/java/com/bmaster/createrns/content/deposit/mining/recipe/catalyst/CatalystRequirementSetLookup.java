package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CatalystRequirementSetLookup {
    public static final Codec<ResourceLocation> ID_CODEC = Codec.STRING.comapFlatMap(
            CatalystRequirementSetLookup::parseId,
            ResourceLocation::toString);

    public static Holder<CatalystRequirementSet> get(RegistryAccess access, ResourceLocation id) {
        var registry = access.registryOrThrow(CatalystRequirementSet.REGISTRY_KEY);
        var key = ResourceKey.create(CatalystRequirementSet.REGISTRY_KEY, id);
        return registry.getHolder(key)
                .orElseThrow(() -> new RuntimeException("Catalyst \"" + id + "\" does not exist"));
    }

    public static ResourceLocation id(Holder<CatalystRequirementSet> holder) {
        return holder.unwrapKey()
                .orElseThrow(() -> new IllegalStateException("Unbound catalyst requirement set holder"))
                .location();
    }

    public static DataResult<ResourceLocation> parseId(String id) {
        if (!id.contains(":")) {
            return DataResult.error(() -> "Catalyst ids must be namespaced: " + id);
        }

        var parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            return DataResult.error(() -> "Invalid catalyst id: " + id);
        }

        return DataResult.success(parsed);
    }
}

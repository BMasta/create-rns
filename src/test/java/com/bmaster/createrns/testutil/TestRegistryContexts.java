package com.bmaster.createrns.testutil;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.CatalystRequirementSet;
import com.google.gson.JsonElement;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;

import java.util.stream.Stream;

public class TestRegistryContexts {
    private static final RegistryAccess.Frozen BUILTIN_ACCESS =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    public static DynamicOps<JsonElement> json() {
        return JsonOps.INSTANCE;
    }

    public static DynamicOps<JsonElement> builtinRegistries() {
        return RegistryOps.create(JsonOps.INSTANCE, builtinLookupProvider());
    }

    public static RegistryAccess.Frozen builtinAccess() {
        return BUILTIN_ACCESS;
    }

    public static HolderLookup.Provider lookupProvider(RegistrySetBuilder builder) {
        return builder.build(BUILTIN_ACCESS);
    }

    public static HolderLookup.Provider builtinLookupProvider() {
        return HolderLookup.Provider.create(BUILTIN_ACCESS.registries().map(entry -> entry.value().asTagAddingLookup()));
    }

    public static RegistryAccess catalystRegistryAccess(CatalystRequirementSet... sets) {
        var registry = new MappedRegistry<>(CatalystRequirementSet.REGISTRY_KEY, Lifecycle.stable());
        for (var set : sets) {
            var key = ResourceKey.create(CatalystRequirementSet.REGISTRY_KEY, CreateRNS.asResource(set.name));
            registry.register(key, set, Lifecycle.stable());
        }

        return new RegistryAccess.ImmutableRegistryAccess(Stream.concat(
                BUILTIN_ACCESS.registries(),
                Stream.of(new RegistryAccess.RegistryEntry<>(CatalystRequirementSet.REGISTRY_KEY, registry.freeze()))
        ));
    }
}

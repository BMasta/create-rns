package com.bmaster.createrns.capability.depositindex;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DepositSpecLookup {
    private static Map<Item, DepositSpec> yieldToSpec;
    private static List<Item> allYields;
    private static Set<ResourceKey<Structure>> allStructureKeys;

    public static DepositSpec getSpec(ServerLevel sl, Item yield) {
        if (yieldToSpec == null) build(sl);
        return yieldToSpec.get(yield);
    }

    public static void build(Level l) {
        var access = l.registryAccess();
        var regEntries = access.registryOrThrow(DepositSpec.REGISTRY_KEY).entrySet();

        yieldToSpec = new HashMap<>(regEntries.size());
        CreateRNS.LOGGER.info("Building DepositSpec lookup");
        regEntries.forEach(e -> {
            CreateRNS.LOGGER.info("Processing registry element {}: {}", e.getKey(), e.getValue());
            yieldToSpec.put(e.getValue().yield(), e.getValue());
        });

        allYields = yieldToSpec.keySet().stream().sorted().toList();
        allStructureKeys = yieldToSpec.values().stream()
                .flatMap(spec -> spec.structures().stream())
                .map(hs -> hs.unwrapKey().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        CreateRNS.LOGGER.info("All deposit structure keys: {}", allStructureKeys);
    }

    public static List<Item> getAllYields(Level l) {
        if (allYields == null) build(l);
        return allYields;
    }

    public static Predicate<Structure> isDeposit(ServerLevel sl) {
        if (allStructureKeys == null) build(sl);
        var reg = sl.registryAccess().registryOrThrow(Registries.STRUCTURE);

        return (Structure checkedStructure) -> {
            boolean contains = reg.getResourceKey(checkedStructure).filter(allStructureKeys::contains).isPresent();
            CreateRNS.LOGGER.info("Structure '{}' is a deposit? {}", checkedStructure, contains);
            return contains;
        };
    }
}

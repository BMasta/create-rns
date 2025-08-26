package com.bmaster.createrns.capability.depositindex;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DepositSpecLookup {
    private static Map<Item, DepositSpec> yieldToSpec;
    private static Map<Block, DepositSpec> depoBlockToSpec;
    private static List<Item> allYields;
    private static Set<ResourceKey<Structure>> allStructureKeys;

    public static DepositSpec getSpec(Level l, Item yield) {
        if (yieldToSpec == null) build(l);
        return yieldToSpec.get(yield);
    }

    public static DepositSpec getSpec(Level l, Block depositBlock) {
        if (depoBlockToSpec == null) build(l);
        return depoBlockToSpec.get(depositBlock);
    }

    public static void build(Level l) {
        var access = l.registryAccess();
        var regEntries = access.registryOrThrow(DepositSpec.REGISTRY_KEY).entrySet();

        depoBlockToSpec = new HashMap<>(regEntries.size());
        regEntries.forEach(e -> {
            var spec = e.getValue();
            var depoBlock = spec.depositBlock();
            if (ForgeRegistries.BLOCKS.getKey(depoBlock) != null) {
                if (depoBlockToSpec.containsKey(depoBlock)) {
                    throw new KeyAlreadyExistsException("Found multiple deposit specs with the same deposit block");
                }
                depoBlockToSpec.put(depoBlock, spec);
            }
        });

        yieldToSpec = new HashMap<>(regEntries.size());
        regEntries.forEach(e -> {
            var spec = e.getValue();
            var yield = spec.yield();
            if (ForgeRegistries.ITEMS.getKey(yield) != null) {
                if (yieldToSpec.containsKey(yield)) {
                    throw new KeyAlreadyExistsException("Found multiple deposit specs with the same yield");
                }
                yieldToSpec.put(yield, spec);
            }
        });

        allYields = yieldToSpec.keySet().stream()
                .sorted(Comparator.comparing(i -> {
                    var rl = ForgeRegistries.ITEMS.getKey(i);
                    if (rl == null)
                        throw new IllegalStateException("This never happens, but my IDE won't shut up about it");
                    return rl;
                })).toList();

        allStructureKeys = yieldToSpec.values().stream()
                .map(hs -> hs.structure().unwrapKey().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static List<Item> getAllYields(Level l) {
        if (allYields == null) build(l);
        return allYields;
    }

    public static Predicate<Structure> isDeposit(ServerLevel sl) {
        if (allStructureKeys == null) build(sl);
        var reg = sl.registryAccess().registryOrThrow(Registries.STRUCTURE);

        return (Structure checkedStructure) ->
                reg.getResourceKey(checkedStructure).filter(allStructureKeys::contains).isPresent();
    }
}

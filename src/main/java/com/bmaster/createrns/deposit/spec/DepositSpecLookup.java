package com.bmaster.createrns.deposit.spec;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DepositSpecLookup {
    private static Map<Item, DepositSpec> scannerIconToSpec;
    private static Map<Block, DepositSpec> depoBlockToSpec;
    private static List<Item> allIcons;
    private static Set<ResourceKey<Structure>> allStructureKeys;

    public static DepositSpec getSpec(RegistryAccess access, Item scannerIconItem) {
        if (scannerIconToSpec == null) build(access);
        return scannerIconToSpec.get(scannerIconItem);
    }

    public static DepositSpec getSpec(RegistryAccess access, Block depositBlock) {
        if (depoBlockToSpec == null) build(access);
        return depoBlockToSpec.get(depositBlock);
    }

    public static void build(RegistryAccess access) {
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

        scannerIconToSpec = new HashMap<>(regEntries.size());
        regEntries.forEach(e -> {
            var spec = e.getValue();
            var scannerIcon = spec.scannerIconItem();
            if (ForgeRegistries.ITEMS.getKey(scannerIcon) != null) {
                if (scannerIconToSpec.containsKey(scannerIcon)) {
                    throw new KeyAlreadyExistsException("Found multiple deposit specs with the same scanner icon");
                }
                scannerIconToSpec.put(scannerIcon, spec);
            }
        });

        allIcons = scannerIconToSpec.keySet().stream()
                .sorted(Comparator.comparing(i -> {
                    var rl = ForgeRegistries.ITEMS.getKey(i);
                    if (rl == null)
                        throw new IllegalStateException("This never happens, but my IDE won't shut up about it");
                    return rl;
                })).toList();

        allStructureKeys = scannerIconToSpec.values().stream()
                .map(hs -> hs.structure().unwrapKey().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static List<Item> getAllScannerIcons(RegistryAccess access) {
        if (allIcons == null) build(access);
        return allIcons;
    }

    public static Predicate<Structure> isDeposit(RegistryAccess access) {
        if (allStructureKeys == null) build(access);
        var reg = access.registryOrThrow(Registries.STRUCTURE);

        return (Structure checkedStructure) ->
                reg.getResourceKey(checkedStructure).filter(allStructureKeys::contains).isPresent();
    }
}

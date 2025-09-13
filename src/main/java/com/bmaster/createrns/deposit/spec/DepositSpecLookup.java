package com.bmaster.createrns.deposit.spec;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DepositSpecLookup {
    private static Map<Item, DepositSpec> scannerIconToSpec;
    private static List<Item> allIcons;
    private static Set<ResourceKey<Structure>> allStructureKeys;

    public static void build(RegistryAccess access) {
        var regEntries = access.registryOrThrow(DepositSpec.REGISTRY_KEY).entrySet();

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
                .map(hs -> ResourceKey.create(Registries.STRUCTURE, hs.structure()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static List<Item> getAllScannerIcons() {
        if (allIcons == null) throw new IllegalStateException("Deposit spec lookup is not built");
        return allIcons;
    }

    public static ResourceKey<Structure> getStructureKey(Item scannerIconItem) {
        if (scannerIconToSpec == null) throw new IllegalStateException("Deposit spec lookup is not built");
        return ResourceKey.create(Registries.STRUCTURE, scannerIconToSpec.get(scannerIconItem).structure());
    }

    public static Predicate<Structure> isDeposit(RegistryAccess access) {
        if (allStructureKeys == null) throw new IllegalStateException("Deposit spec lookup is not built");
        var reg = access.registryOrThrow(Registries.STRUCTURE);

        return (Structure checkedStructure) ->
                reg.getResourceKey(checkedStructure).filter(allStructureKeys::contains).isPresent();
    }
}

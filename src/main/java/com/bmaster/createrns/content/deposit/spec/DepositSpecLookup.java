package com.bmaster.createrns.content.deposit.spec;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositSpecLookup {
    private static Map<Item, DepositSpec> scannerIconToSpec;
    private static Map<ResourceKey<Structure>, DepositSpec> structureKeyToSpec;
    private static List<Item> allIcons;
    private static Set<ResourceKey<Structure>> allStructureKeys;

    public static void build(RegistryAccess access) {
        var regEntries = access.registryOrThrow(DepositSpec.REGISTRY_KEY).entrySet();

        scannerIconToSpec = new HashMap<>(regEntries.size());
        structureKeyToSpec = new HashMap<>(regEntries.size());
        regEntries.forEach(e -> {
            var spec = e.getValue();
            var scannerIcon = spec.scannerIconItem();
            if (BuiltInRegistries.ITEM.getKeyOrNull(scannerIcon) != null) {
                if (scannerIconToSpec.containsKey(scannerIcon)) {
                    throw new KeyAlreadyExistsException("Found multiple deposit specs with the same scanner icon");
                }
                scannerIconToSpec.put(scannerIcon, spec);
            }

            var structureKey = spec.structureKey();
            if (structureKeyToSpec.containsKey(structureKey)) {
                throw new KeyAlreadyExistsException("Found multiple deposit specs with the same structure");
            }
            structureKeyToSpec.put(structureKey, spec);
        });

        allIcons = scannerIconToSpec.keySet().stream()
                .sorted(Comparator.comparing(BuiltInRegistries.ITEM::getKey)).toList();

        allStructureKeys = structureKeyToSpec.keySet().stream().collect(Collectors.toUnmodifiableSet());
    }

    public static ResourceKey<Structure> getStructureKey(RegistryAccess access, Item scannerIconItem) {
        if (scannerIconToSpec == null) build(access);
        return scannerIconToSpec.get(scannerIconItem).structureKey();
    }

    public static MutableComponent getDepositName(RegistryAccess access, Item scannerIconItem) {
        return getDepositName(getStructureKey(access, scannerIconItem));
    }

    public static @Nullable ItemStack getMapIconItem(RegistryAccess access, ResourceKey<Structure> structureKey) {
        if (structureKeyToSpec == null) build(access);
        var spec = structureKeyToSpec.get(structureKey);
        return spec == null ? null : spec.mapIconItem();
    }

    public static MutableComponent getDepositName(ResourceKey<Structure> depKey) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return Component.empty();
        var dRL = depKey.location();
        return Component.translatable(dRL.getNamespace() + ".structure." + dRL.getPath());
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

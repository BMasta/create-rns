package com.bmaster.createrns.content.deposit.spec;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DepositSpecLookup {
    private static Map<Item, Map<ResourceKey<Level>, DepositSpec>> scannerIconToDimToSpec;
    private static Map<ResourceKey<Structure>, DepositSpec> structureKeyToSpec;
    private static Map<ResourceKey<Level>, List<Item>> dimToScannerIcons;

    public static void build(RegistryAccess access) {
        var regEntries = access.registryOrThrow(DepositSpec.REGISTRY_KEY).entrySet();

        scannerIconToDimToSpec = new HashMap<>();
        structureKeyToSpec = new HashMap<>();
        dimToScannerIcons = new HashMap<>();
        regEntries.forEach(e -> {
            var spec = e.getValue();
            spec.initialize(access);

            var scannerIcon = spec.getScannerIcon();
            if (scannerIcon == null) return;

            var dimSpecs = scannerIconToDimToSpec.computeIfAbsent(scannerIcon, ignored -> new HashMap<>());
            if (dimSpecs.containsKey(spec.dimension)) {
                throw new KeyAlreadyExistsException("Found deposit spec with the same scanner icon: " + scannerIcon +
                        " (" + spec.dimension.location() + ")");
            }
            dimSpecs.put(spec.dimension, spec);

            var structureKey = spec.structureKey();
            if (structureKeyToSpec.containsKey(structureKey)) {
                throw new KeyAlreadyExistsException("Found deposit spec with the same structure: " + spec.structure);
            }
            structureKeyToSpec.put(structureKey, spec);
            dimToScannerIcons.computeIfAbsent(spec.dimension, ignored -> new ArrayList<>())
                    .add(scannerIcon);
        });
    }

    public static @Nullable ResourceKey<Structure> getStructureKey(Level l, Item scannerIconItem) {
        if (scannerIconToDimToSpec == null) build(l.registryAccess());
        var dimSpecs = scannerIconToDimToSpec.get(scannerIconItem);
        if (dimSpecs == null) return null;
        var spec = dimSpecs.get(l.dimension());
        if (spec == null) return null;
        return spec.structureKey();
    }

    public static MutableComponent getDepositName(Level l, Item scannerIconItem) {
        var key = getStructureKey(l, scannerIconItem);
        if (key == null) return Component.empty();
        return getDepositName(key);
    }

    public static MutableComponent getDepositName(ResourceKey<Structure> depKey) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return Component.empty();
        var dRL = depKey.location();
        return Component.translatable(dRL.getNamespace() + ".structure." + dRL.getPath());
    }

    public static @Nullable ItemStack getMapIcon(RegistryAccess access, ResourceKey<Structure> structureKey) {
        if (structureKeyToSpec == null) build(access);
        var spec = structureKeyToSpec.get(structureKey);
        return spec == null ? null : spec.getMapIcon();
    }

    public static List<Item> getScannerIcons(Level l) {
        if (dimToScannerIcons == null) build(l.registryAccess());
        return dimToScannerIcons.get(l.dimension());
    }
}

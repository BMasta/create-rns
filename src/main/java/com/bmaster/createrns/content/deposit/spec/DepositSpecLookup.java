package com.bmaster.createrns.content.deposit.spec;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
    private static Set<ResourceKey<Structure>> allStructureKeys;
    // Server
    private @Nullable static Map<ResourceKey<Level>, ArrayList<Item>> dimToIcons = null;
    // Client
    private static Map<ResourceKey<Level>, List<Item>> dimToIconsCached = Map.of();

    public static void build(RegistryAccess access) {
        var regEntries = access.registryOrThrow(DepositSpec.REGISTRY_KEY).entrySet();

        scannerIconToSpec = new HashMap<>(regEntries.size());
        structureKeyToSpec = new HashMap<>(regEntries.size());
        regEntries.forEach(e -> {
            var spec = e.getValue();
            spec.initialize(access);

            var scannerIcon = spec.getIcon();
            if (scannerIcon != null && BuiltInRegistries.ITEM.getKeyOrNull(scannerIcon) != null) {
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

        allStructureKeys = structureKeyToSpec.keySet().stream().collect(Collectors.toUnmodifiableSet());
    }

    public static @Nullable ResourceKey<Structure> getStructureKey(RegistryAccess access, Item scannerIconItem) {
        if (scannerIconToSpec == null) build(access);
        var spec = scannerIconToSpec.get(scannerIconItem);
        if (spec == null) return null;
        return scannerIconToSpec.get(scannerIconItem).structureKey();
    }

    public static MutableComponent getDepositName(RegistryAccess access, Item scannerIconItem) {
        var key = getStructureKey(access, scannerIconItem);
        if (key == null) return Component.empty();
        return getDepositName(key);
    }

    public static @Nullable ItemStack getMapIcon(RegistryAccess access, ResourceKey<Structure> structureKey) {
        if (structureKeyToSpec == null) build(access);
        var spec = structureKeyToSpec.get(structureKey);
        return spec == null ? null : spec.getMapIcon();
    }

    public static MutableComponent getDepositName(ResourceKey<Structure> depKey) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return Component.empty();
        var dRL = depKey.location();
        return Component.translatable(dRL.getNamespace() + ".structure." + dRL.getPath());
    }

    public static Map<ResourceKey<Level>, ArrayList<Item>> getScannerIcons(MinecraftServer server) {
        if (dimToIcons != null) return dimToIcons;

        if (allStructureKeys == null) build(server.registryAccess());
        dimToIcons = new Object2ObjectOpenHashMap<>();
        for (var sl : server.getAllLevels()) {
            dimToIcons.put(sl.dimension(), allStructureKeys.stream()
                    .filter(k -> {
                        var structure = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(k).orElse(null);
                        if (structure == null) return false;
                        return !sl.getChunkSource().getGeneratorState().getPlacementsForStructure(structure).isEmpty();
                    })
                    .map(k -> structureKeyToSpec.get(k).getIcon())
                    .collect(Collectors.toCollection(ArrayList::new)));
        }
        return dimToIcons;
    }

    public static List<Item> getScannerIcons(ClientLevel cl) {
        return dimToIconsCached.getOrDefault(cl.dimension(), Collections.emptyList());
    }

    public static void setScannerIcons(Map<ResourceKey<Level>, ? extends List<Item>> icons) {
        dimToIconsCached = icons.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
    }

    public static Predicate<Structure> isDeposit(RegistryAccess access) {
        if (allStructureKeys == null) build(access);
        var reg = access.registryOrThrow(Registries.STRUCTURE);

        return (Structure checkedStructure) ->
                reg.getResourceKey(checkedStructure).filter(allStructureKeys::contains).isPresent();
    }
}

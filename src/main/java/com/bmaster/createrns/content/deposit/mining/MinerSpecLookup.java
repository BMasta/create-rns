package com.bmaster.createrns.content.deposit.mining;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.HashMap;
import java.util.Map;

public class MinerSpecLookup {
    private static Map<Block, MinerSpec> minerToSpec;

    public static void build(RegistryAccess access) {
        var regEntries = access.registryOrThrow(MinerSpec.REGISTRY_KEY).entrySet();

        minerToSpec = new HashMap<>(regEntries.size());
        for (var e : regEntries) {
            var spec = e.getValue();
            var miner = spec.minerBlock();
            if (BuiltInRegistries.BLOCK.getKeyOrNull(miner) != null) {
                if (minerToSpec.containsKey(miner)) {
                    throw new KeyAlreadyExistsException("Found multiple miner specs with the same miner block");
                }
                minerToSpec.put(miner, spec);
            }
        }
    }

    public static MinerSpec get(RegistryAccess access, Block miner) {
        if (minerToSpec == null) build(access);
        return minerToSpec.get(miner);
    }
}

package com.bmaster.createrns.mining.miner;

import net.minecraft.core.RegistryAccess;
import net.minecraftforge.registries.ForgeRegistries;

import javax.management.openmbean.InvalidKeyException;
import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.HashMap;
import java.util.Map;

public class MinerSpecLookup {
    private static Map<MinerBlock, MinerSpec> minerToSpec;

    public static void build(RegistryAccess access) {
        var regEntries = access.registryOrThrow(MinerSpec.REGISTRY_KEY).entrySet();

        minerToSpec = new HashMap<>(regEntries.size());
        regEntries.forEach(e -> {
            var spec = e.getValue();
            var miner = spec.minerBlock();
            if (ForgeRegistries.BLOCKS.getKey(miner) != null) {
                if (!(miner instanceof MinerBlock minerFrFr)) {
                    throw new InvalidKeyException("Block %s in a miner spec is not a miner block");
                }
                if (minerToSpec.containsKey(miner)) {
                    throw new KeyAlreadyExistsException("Found multiple miner specs with the same miner block");
                }
                minerToSpec.put(minerFrFr, spec);
            }
        });
    }

    public static MinerSpec get(RegistryAccess access, MinerBlock miner) {
        if (minerToSpec == null) build(access);
        return minerToSpec.get(miner);
    }
}

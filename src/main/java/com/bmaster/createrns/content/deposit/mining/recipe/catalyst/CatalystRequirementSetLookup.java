package com.bmaster.createrns.content.deposit.mining.recipe.catalyst;

import net.minecraft.core.RegistryAccess;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.HashMap;
import java.util.Map;

public class CatalystRequirementSetLookup {
    private static Map<String, CatalystRequirementSet> nameToSet;

    public static void build(RegistryAccess access) {
        var regEntries = access.registryOrThrow(CatalystRequirementSet.REGISTRY_KEY).entrySet();

        nameToSet = new HashMap<>(regEntries.size());
        for (var e : regEntries) {
            var set = e.getValue();
            var name = set.name;
            if (nameToSet.containsKey(name)) {
                throw new KeyAlreadyExistsException("Found multiple catalyst definitions with the same name");
            }
            nameToSet.put(name, set);
        }
    }

    public static CatalystRequirementSet get(RegistryAccess access, String name) {
        if (nameToSet == null) build(access);
        return nameToSet.get(name);
    }
}


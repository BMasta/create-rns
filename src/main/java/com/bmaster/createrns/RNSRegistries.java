package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.spec.DepositSpec;
import com.bmaster.createrns.data.DumpSafeOverrides;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class RNSRegistries {
    public static final ResourceKey<Registry<DepositSpec>> DEPOSIT_SPEC =
            ResourceKey.createRegistryKey(DumpSafeOverrides.DEPOSIT_SPEC);
}

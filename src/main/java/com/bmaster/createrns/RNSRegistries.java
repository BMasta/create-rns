package com.bmaster.createrns;

import com.bmaster.createrns.content.deposit.spec.DepositSpec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class RNSRegistries {
    public static final ResourceKey<Registry<DepositSpec>> DEPOSIT_SPEC =
            ResourceKey.createRegistryKey(CreateRNS.asResource("deposit_spec"));
}

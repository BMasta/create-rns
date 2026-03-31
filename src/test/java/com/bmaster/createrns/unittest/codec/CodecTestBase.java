package com.bmaster.createrns.unittest.codec;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;

abstract class CodecTestBase {
    private static boolean bootstrapped = false;

    @BeforeAll
    static void bootstrapMinecraft() {
        if (bootstrapped) return;

        SharedConstants.tryDetectVersion();
        try {
            var isBootstrapped = Bootstrap.class.getDeclaredField("isBootstrapped");
            isBootstrapped.setAccessible(true);
            isBootstrapped.setBoolean(null, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to initialize Minecraft bootstrap for unit tests", e);
        }
        BuiltInRegistries.bootStrap();
        bootstrapped = true;
    }
}

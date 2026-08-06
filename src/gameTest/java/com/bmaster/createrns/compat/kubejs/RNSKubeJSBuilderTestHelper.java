package com.bmaster.createrns.compat.kubejs;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTestHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSBuilderTestHelper {
    private final GameTestHelper raw;

    public RNSKubeJSBuilderTestHelper(GameTestHelper raw) {
        this.raw = raw;
    }

    public GameTestHelper raw() {
        return raw;
    }

    public void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError(message);
    }

    public void assertValueEqual(Object actual, Object expected, String label) {
        if (Objects.equals(actual, expected)) return;

        throw new AssertionError(label + ": expected <" + expected + "> but got <" + actual + ">");
    }
}

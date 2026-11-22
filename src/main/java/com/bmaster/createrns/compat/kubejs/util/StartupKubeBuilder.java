package com.bmaster.createrns.compat.kubejs.util;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class StartupKubeBuilder extends StartupJSObject {
    private @Nullable Boolean valid = null;

    protected StartupKubeBuilder(String eventName, String creationMethods) {
        super(eventName, creationMethods);
    }

    /// Call only after configuration finishes. Validation runs at most once.
    public final boolean isValid() {
        if (valid == null) {
            try {
                valid = true;
                assertValid();
            } catch (RuntimeException | Error exception) {
                valid = false;
                throw exception;
            }
        }
        return valid;
    }

    public final void invalidate() {
        valid = false;
    }

    public final void error(String message, boolean fatal) {
        if (fatal) invalidate();
        KubeStartupError.add(message, errorSource());
    }

    protected abstract void assertValid();
}

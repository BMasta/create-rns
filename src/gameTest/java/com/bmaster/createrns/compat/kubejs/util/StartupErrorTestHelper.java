package com.bmaster.createrns.compat.kubejs.util;

import java.util.ArrayList;
import java.util.List;

/** Captures errors from standalone builder calls outside a processor. */
public final class StartupErrorTestHelper {
    public static List<KubeStartupError.SourcedError> capture(Runnable action) {
        var errors = new ArrayList<KubeStartupError.SourcedError>();
        return KubeStartupError.capture(errors, () -> {
            action.run();
            return List.copyOf(errors);
        });
    }

    private StartupErrorTestHelper() {
    }
}

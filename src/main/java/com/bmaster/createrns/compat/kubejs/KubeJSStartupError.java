package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.rhino.Context;

final class KubeJSStartupError {
    static KubeRuntimeException exception(
            Context cx, String eventName, String builderMethods, String methodName, Throwable cause
    ) {
        var sourceLine = KubeJSSourceLine.startupMethodCall(
                SourceLine.of(cx), eventName, builderMethods, methodName);
        return exception(sourceLine, cause);
    }

    static KubeRuntimeException exception(SourceLine sourceLine, Throwable cause) {
        return new KubeRuntimeException(cause.getMessage(), cause).source(sourceLine);
    }

    static SourceLine builderStart(Context cx, String eventName, String builderMethods) {
        return KubeJSSourceLine.startupBuilderStart(SourceLine.of(cx), eventName, builderMethods);
    }

    static void report(String message, SourceLine sourceLine) {
        ConsoleJS.STARTUP.error(message, sourceLine, null, null);
    }

    private KubeJSStartupError() {
    }
}

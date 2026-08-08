package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.rhino.Context;

import java.util.function.Supplier;

abstract class SourcedStartupKubeBuilder {
    private final SourceLine creationSourceLine;
    private final String eventName;
    private final String builderMethods;
    private boolean invalid;

    protected SourcedStartupKubeBuilder(SourceLine creationSourceLine, String eventName, String builderMethods) {
        this.creationSourceLine = creationSourceLine;
        this.eventName = eventName;
        this.builderMethods = builderMethods;
    }

    protected final <T> T sourced(Context cx, String methodName, Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException cause) {
            invalid = true;
            throw KubeJSStartupError.exception(cx, eventName, builderMethods, methodName, cause);
        }
    }

    protected final <T> T sourcedAtCreation(Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException cause) {
            invalid = true;
            throw KubeJSStartupError.exception(creationSourceLine, cause);
        }
    }

    protected final boolean reportDeferredError(String message) {
        return reportDeferredError(message, creationSourceLine);
    }

    protected final boolean reportDeferredError(String message, SourceLine sourceLine) {
        if (sourceLine.isUnknown()) throw new IllegalStateException(message);

        invalid = true;
        KubeJSStartupError.report(message, sourceLine);
        return false;
    }

    protected final SourceLine methodSource(Context cx, String methodName) {
        return KubeJSSourceLine.startupMethodCall(
                SourceLine.of(cx), eventName, builderMethods, methodName);
    }

    protected final boolean hasKnownCreationSource() {
        return !creationSourceLine.isUnknown();
    }

    protected final SourceLine creationSourceLine() {
        return creationSourceLine;
    }

    final boolean isInvalid() {
        return invalid;
    }
}

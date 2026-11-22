package com.bmaster.createrns.compat.kubejs.util;

import dev.latvian.mods.kubejs.event.EventExceptionHandler;
import dev.latvian.mods.kubejs.script.SourceLine;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public final class KubeStartupError {
    // The thread processing the events stores its errors here. Once the event processing is finished,
    // the errors are copied to the ProcessedEvents snapshot where they can be queried by any thread.
    private static final ThreadLocal<List<SourcedError>> PENDING_ERRORS = new ThreadLocal<>();

    public static EventExceptionHandler eventHandler(String eventName) {
        return (event, listener, error) -> {
            add(new SourcedError("Error in '" + eventName + "'", SourceLine.UNKNOWN, error));
            return null;
        };
    }

    public static void add(String message, SourceLine sourceLine) {
        add(new SourcedError(message, sourceLine, null));
    }

    private static void add(SourcedError sourcedError) {
        var capture = PENDING_ERRORS.get();
        if (capture == null) {
            throw new IllegalStateException("Startup diagnostic outside event processing: " + sourcedError.message(),
                    sourcedError.error());
        }
        capture.add(sourcedError);
    }

    public static <T> T capture(List<SourcedError> errors, Supplier<T> action) {
        var previous = PENDING_ERRORS.get();
        PENDING_ERRORS.set(errors);
        try {
            return action.get();
        } finally {
            if (previous == null) PENDING_ERRORS.remove();
            else PENDING_ERRORS.set(previous);
        }
    }

    private KubeStartupError() {
    }

    public record SourcedError(String message, SourceLine source, @Nullable Throwable error) {}
}

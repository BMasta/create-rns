package com.bmaster.createrns.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJSPaths;
import dev.latvian.mods.kubejs.script.SourceLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

final class KubeJSSourceLine {
    private static final Pattern RECIPE_CALL = Pattern.compile(
            "\\b[A-Za-z_$][\\w$]*\\s*\\.\\s*recipes\\s*\\.\\s*create_rns\\s*\\.\\s*mining\\s*\\(");
    private static final Map<Path, CachedSource> SOURCE_CACHE = new ConcurrentHashMap<>();

    static SourceLine expressionStart(SourceLine coarse) {
        var source = readSource(coarse);
        return source == null ? coarse : expressionStart(coarse, source.sanitized());
    }

    static SourceLine methodCall(SourceLine coarse, String methodName) {
        var source = readSource(coarse);
        return source == null ? coarse : methodCall(coarse, source.sanitized(), methodName);
    }

    static SourceLine startupBuilderStart(SourceLine coarse, String eventName, String builderMethods) {
        var source = readSource(coarse);
        return source == null ? coarse : startupBuilderStart(
                coarse, source.sanitized(), eventName, builderMethods);
    }

    static SourceLine startupMethodCall(
            SourceLine coarse, String eventName, String builderMethods, String methodName
    ) {
        var source = readSource(coarse);
        return source == null ? coarse : startupMethodCall(
                coarse, source.sanitized(), eventName, builderMethods, methodName);
    }

    static SourceLine startupBuilderStart(
            SourceLine coarse, String script, String eventName, String builderMethods
    ) {
        var builderStart = findStartupBuilder(script, coarse.line(), eventName, builderMethods);
        return builderStart < 0 ? coarse : SourceLine.of(coarse.source(), lineAt(script, builderStart));
    }

    static SourceLine startupMethodCall(
            SourceLine coarse, String script, String eventName, String builderMethods, String methodName
    ) {
        var builderStart = findStartupBuilder(script, coarse.line(), eventName, builderMethods);
        if (builderStart < 0) return coarse;

        var builderPattern = methodPattern(builderMethods);
        var nextBuilder = builderPattern.matcher(script);
        var builderEnd = nextBuilder.find(builderStart + 1) ? nextBuilder.start() : script.length();
        var methodCall = Pattern.compile("\\.\\s*" + Pattern.quote(methodName) + "\\s*\\(").matcher(script);
        var coarseStart = startOfLine(script, coarse.line());
        if (methodCall.find(Math.max(builderStart, coarseStart)) && methodCall.start() < builderEnd) {
            return SourceLine.of(coarse.source(), lineAt(script, methodCall.start()));
        }

        methodCall = Pattern.compile("\\.\\s*" + Pattern.quote(methodName) + "\\s*\\(").matcher(script);
        if (methodCall.find(builderStart) && methodCall.start() < builderEnd) {
            return SourceLine.of(coarse.source(), lineAt(script, methodCall.start()));
        }

        return coarse;
    }

    static SourceLine expressionStart(SourceLine coarse, String script) {
        var recipeCall = findRecipeCall(script, coarse.line());
        return recipeCall < 0 ? coarse : SourceLine.of(coarse.source(), lineAt(script, recipeCall));
    }

    static SourceLine methodCall(SourceLine coarse, String script, String methodName) {
        var recipeCall = findRecipeCall(script, coarse.line());
        if (recipeCall < 0) return coarse;

        var methodPattern = Pattern.compile("\\.\\s*" + Pattern.quote(methodName) + "\\s*\\(");
        var methodCall = methodPattern.matcher(script);
        var coarseStart = startOfLine(script, coarse.line());
        var coarseEnd = endOfLine(script, coarse.line());

        var nextYield = Pattern.compile("\\.\\s*yield\\s*\\(").matcher(script);
        var localEnd = nextYield.find(coarseEnd) ? nextYield.start() : script.length();
        if (methodCall.find(coarseStart) && methodCall.start() < localEnd) {
            return SourceLine.of(coarse.source(), lineAt(script, methodCall.start()));
        }

        methodCall = methodPattern.matcher(script);
        var result = -1;
        while (methodCall.find(recipeCall) && methodCall.start() < coarseEnd) {
            result = methodCall.start();
            recipeCall = methodCall.end();
        }

        return result < 0 ? coarse : SourceLine.of(coarse.source(), lineAt(script, result));
    }

    private static CachedSource readSource(SourceLine sourceLine) {
        if (sourceLine.isUnknown()) return null;

        var path = resolvePath(sourceLine.source());
        if (path == null) return null;

        try {
            var modified = Files.getLastModifiedTime(path).toMillis();
            var size = Files.size(path);
            var cached = SOURCE_CACHE.get(path);
            if (cached != null && cached.modified() == modified && cached.size() == size) return cached;

            var source = new CachedSource(modified, size, sanitize(Files.readString(path)));
            SOURCE_CACHE.put(path, source);
            return source;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static Path resolvePath(String source) {
        var separator = source.indexOf(':');
        if (separator < 0) return null;

        var namespace = source.substring(0, separator);
        Path root;
        if (namespace.equals(KubeJSPaths.SERVER_SCRIPTS.getFileName().toString())) {
            root = KubeJSPaths.SERVER_SCRIPTS;
        } else if (namespace.equals(KubeJSPaths.LOCAL_SERVER_SCRIPTS.getFileName().toString())) {
            root = KubeJSPaths.LOCAL_SERVER_SCRIPTS;
        } else if (namespace.equals(KubeJSPaths.STARTUP_SCRIPTS.getFileName().toString())) {
            root = KubeJSPaths.STARTUP_SCRIPTS;
        } else if (namespace.equals(KubeJSPaths.LOCAL_STARTUP_SCRIPTS.getFileName().toString())) {
            root = KubeJSPaths.LOCAL_STARTUP_SCRIPTS;
        } else {
            return null;
        }

        var path = root.resolve(source.substring(separator + 1)).normalize().toAbsolutePath();
        var normalizedRoot = root.normalize().toAbsolutePath();
        return path.startsWith(normalizedRoot) ? path : null;
    }

    private static int findRecipeCall(String script, int coarseLine) {
        var coarseEnd = endOfLine(script, coarseLine);
        var matcher = RECIPE_CALL.matcher(script);
        var result = -1;
        while (matcher.find() && matcher.start() < coarseEnd) result = matcher.start();
        return result;
    }

    private static int findStartupBuilder(
            String script, int coarseLine, String eventName, String builderMethods
    ) {
        var eventPattern = Pattern.compile("\\bStartupEvents\\s*\\.\\s*" + Pattern.quote(eventName) + "\\s*\\(");
        var coarseEnd = endOfLine(script, coarseLine);
        var eventCall = eventPattern.matcher(script);
        var eventStart = -1;
        while (eventCall.find() && eventCall.start() < coarseEnd) eventStart = eventCall.start();
        if (eventStart < 0) return -1;

        var nextEvent = Pattern.compile("\\bStartupEvents\\s*\\.").matcher(script);
        var eventEnd = nextEvent.find(eventStart + 1) ? nextEvent.start() : script.length();
        var builderCall = methodPattern(builderMethods).matcher(script);
        var firstAfterCoarse = -1;
        var lastBeforeCoarse = -1;
        while (builderCall.find(eventStart) && builderCall.start() < eventEnd) {
            if (builderCall.start() < coarseEnd) {
                lastBeforeCoarse = builderCall.start();
            } else if (firstAfterCoarse < 0) {
                firstAfterCoarse = builderCall.start();
            }
            eventStart = builderCall.end();
        }

        return lastBeforeCoarse >= 0 ? lastBeforeCoarse : firstAfterCoarse;
    }

    private static Pattern methodPattern(String methodNames) {
        return Pattern.compile("\\.\\s*(?:" + methodNames + ")\\s*\\(");
    }

    private static int endOfLine(String script, int line) {
        if (line <= 0) return script.length();

        var currentLine = 1;
        for (var index = 0; index < script.length(); index++) {
            if (script.charAt(index) != '\n') continue;
            if (currentLine++ == line) return index;
        }

        return script.length();
    }

    private static int startOfLine(String script, int line) {
        if (line <= 1) return 0;

        var currentLine = 1;
        for (var index = 0; index < script.length(); index++) {
            if (script.charAt(index) != '\n') continue;
            if (++currentLine == line) return index + 1;
        }

        return script.length();
    }

    private static int lineAt(String script, int position) {
        var line = 1;
        for (var index = 0; index < position; index++) {
            if (script.charAt(index) == '\n') line++;
        }
        return line;
    }

    private static String sanitize(String script) {
        var sanitized = new StringBuilder(script.length());
        var state = State.CODE;
        var escaped = false;

        for (var index = 0; index < script.length(); index++) {
            var character = script.charAt(index);
            var next = index + 1 < script.length() ? script.charAt(index + 1) : '\0';

            if (character == '\n') {
                sanitized.append(character);
                if (state == State.LINE_COMMENT) state = State.CODE;
                escaped = false;
                continue;
            }

            if (state == State.CODE) {
                if (character == '/' && next == '/') {
                    sanitized.append("  ");
                    index++;
                    state = State.LINE_COMMENT;
                } else if (character == '/' && next == '*') {
                    sanitized.append("  ");
                    index++;
                    state = State.BLOCK_COMMENT;
                } else if (character == '\'') {
                    sanitized.append(' ');
                    state = State.SINGLE_QUOTE;
                } else if (character == '"') {
                    sanitized.append(' ');
                    state = State.DOUBLE_QUOTE;
                } else if (character == '`') {
                    sanitized.append(' ');
                    state = State.TEMPLATE;
                } else {
                    sanitized.append(character);
                }
                continue;
            }

            if (state == State.BLOCK_COMMENT && character == '*' && next == '/') {
                sanitized.append("  ");
                index++;
                state = State.CODE;
                continue;
            }

            sanitized.append(' ');
            if (state == State.LINE_COMMENT || state == State.BLOCK_COMMENT) continue;

            if (!escaped && (state == State.SINGLE_QUOTE && character == '\''
                    || state == State.DOUBLE_QUOTE && character == '"'
                    || state == State.TEMPLATE && character == '`')) {
                state = State.CODE;
            }
            escaped = !escaped && character == '\\';
        }

        return sanitized.toString();
    }

    private enum State {
        CODE,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        TEMPLATE,
        LINE_COMMENT,
        BLOCK_COMMENT
    }

    private record CachedSource(long modified, long size, String sanitized) {
    }

    private KubeJSSourceLine() {
    }
}

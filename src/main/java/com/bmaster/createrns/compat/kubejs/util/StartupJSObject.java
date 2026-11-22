package com.bmaster.createrns.compat.kubejs.util;

import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.rhino.*;
import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapper;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/// Shared JavaScript boundary for our startup events and builders. Java callers use the methods directly.
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class StartupJSObject implements CustomJavaToJsWrapper {
    private static final ThreadLocal<SourceLine> ACTIVE_CALL_SOURCE = new ThreadLocal<>();
    private static final ClassValue<List<Method>> JS_METHODS = new ClassValue<>() {
        @Override
        protected List<Method> computeValue(Class<?> type) {
            // getMethods selects the overriding implementation, so an override must opt in again.
            return Arrays.stream(type.getMethods())
                    .filter(method -> method.isAnnotationPresent(JSMethod.class))
                    .filter(method -> !Modifier.isStatic(method.getModifiers()) && !method.isBridge())
                    .sorted(Comparator.comparing(Method::toGenericString))
                    .toList();
        }
    };

    private final String eventName;
    private final String creationMethods;
    private SourceLine creationSource = SourceLine.UNKNOWN;

    protected StartupJSObject(String eventName, String creationMethods) {
        this.eventName = eventName;
        this.creationMethods = creationMethods;
    }

    public final SourceLine creationSource() {
        return creationSource;
    }

    protected final SourceLine errorSource() {
        var active = ACTIVE_CALL_SOURCE.get();
        return active == null || active.isUnknown() ? creationSource : active;
    }

    @Override
    public final Scriptable convertJavaToJs(Context cx, Scriptable scope, TypeInfo staticType) {
        return new JSObject(cx, scope, this, staticType);
    }

    private static final class JSObject extends NativeJavaObject {
        private final Map<String, BaseFunction> functions = new LinkedHashMap<>();
        private final Set<String> methodNames = new LinkedHashSet<>();

        private JSObject(Context cx, Scriptable scope, StartupJSObject object, TypeInfo type) {
            super(scope, object, type, cx);
            var groups = new LinkedHashMap<String, List<NativeJavaMethod>>();
            for (var method : JS_METHODS.get(object.getClass())) {
                var info = new CachedMethodInfo(cx.getCachedClassStorage(false).get(method.getDeclaringClass()), method);
                var delegate = new NativeJavaMethod(info, method.getName());
                groups.computeIfAbsent(method.getName(), ignored -> new ArrayList<>()).add(delegate);
                var signature = Arrays.stream(method.getParameterTypes())
                        .map(JavaMembers::javaSignature).collect(Collectors.joining(",", "(", ")"));
                functions.put(method.getName() + signature, new JSFunction(cx, scope, object.getClass(), delegate));
            }
            for (var entry : groups.entrySet()) {
                var overloads = entry.getValue();
                // These are private delegates, never Rhino's shared cached method groups.
                var delegate = new NativeJavaMethod((CachedMethodInfo) overloads.getFirst().methods[0].getInfo(),
                        entry.getKey());
                delegate.methods = overloads.stream().flatMap(method -> Arrays.stream(method.methods))
                        .toArray(MemberBox[]::new);
                functions.put(entry.getKey(), new JSFunction(cx, scope, object.getClass(), delegate));
                methodNames.add(entry.getKey());
            }
        }

        @Override
        protected void initMembers(Context cx, Scriptable scope) {
            // Do not expose reflected fields, bean properties, Object methods, or unannotated overloads.
        }

        @Override
        public Object get(Context cx, String name, Scriptable start) {
            return functions.containsKey(name) ? functions.get(name) : Scriptable.NOT_FOUND;
        }

        @Override
        public boolean has(Context cx, String name, Scriptable start) {
            return functions.containsKey(name);
        }

        @Override
        public Object[] getIds(Context cx) {
            return methodNames.toArray();
        }

        @Override
        public Object get(Context cx, int index, Scriptable start) {
            return Scriptable.NOT_FOUND;
        }

        @Override
        public Object get(Context cx, Symbol symbol, Scriptable start) {
            return Scriptable.NOT_FOUND;
        }

        @Override
        public boolean has(Context cx, Symbol symbol, Scriptable start) {
            return false;
        }

        @Override
        public void put(Context cx, String name, Scriptable start, Object value) {
            throw Context.reportRuntimeError("Startup builder members are read-only: " + name, cx);
        }

        @Override
        public void put(Context cx, int index, Scriptable start, Object value) {
            put(cx, Integer.toString(index), start, value);
        }

        @Override
        public void put(Context cx, Symbol symbol, Scriptable start, Object value) {
            put(cx, symbol.toString(), start, value);
        }

        @Override
        public void delete(Context cx, String name) {
            // Match native Java objects: script deletion cannot remove Java members.
        }
    }

    private static final class JSFunction extends BaseFunction {
        private final Class<?> receiverType;
        private final NativeJavaMethod delegate;

        private JSFunction(Context cx, Scriptable scope, Class<?> receiverType, NativeJavaMethod delegate) {
            super(scope, ScriptableObject.getFunctionPrototype(scope, cx));
            this.receiverType = receiverType;
            this.delegate = delegate;
        }

        @Override
        public String getFunctionName() {
            return delegate.getFunctionName();
        }

        @Override
        public int getArity() {
            return delegate.getArity();
        }

        @Override
        public int getLength() {
            return delegate.getLength();
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            for (var receiver = thisObj; receiver != null; receiver = receiver.getPrototype(cx)) {
                if (!(receiver instanceof Wrapper wrapper)
                        || !(wrapper.unwrap() instanceof StartupJSObject object)
                        || object.getClass() != receiverType) continue;

                var coarse = SourceLine.of(cx);
                var source = KubeSourceLine.startupMethodCall(
                        coarse, object.eventName, object.creationMethods, getFunctionName());
                var previous = ACTIVE_CALL_SOURCE.get();
                ACTIVE_CALL_SOURCE.set(source);
                try {
                    var result = delegate.call(cx, scope, thisObj, args);
                    if (!(object instanceof StartupKubeBuilder)
                            && Wrapper.unwrapped(result) instanceof StartupKubeBuilder builder) {
                        StartupJSObject created = builder;
                        if (created.creationSource.isUnknown()) {
                            created.creationSource = KubeSourceLine.startupBuilderStart(
                                    coarse, object.eventName, object.creationMethods);
                        }
                    }
                    return result;
                } catch (RuntimeException exception) {
                    if (object instanceof StartupKubeBuilder builder) builder.invalidate();
                    Throwable cause = exception;
                    while (cause instanceof WrappedException wrapped) cause = wrapped.getWrappedException();
                    var errorSource = object.errorSource();
                    var error = cause instanceof KubeRuntimeException kubeError
                            ? kubeError.source(errorSource) : new KubeRuntimeException(cause.getMessage(), cause).source(errorSource);
                    throw Context.throwAsScriptRuntimeEx(error, cx);
                } finally {
                    if (previous == null) ACTIVE_CALL_SOURCE.remove();
                    else ACTIVE_CALL_SOURCE.set(previous);
                }
            }
            throw Context.reportRuntimeError("Invalid receiver for " + getFunctionName(), cx);
        }
    }
}

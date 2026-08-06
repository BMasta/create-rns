package com.bmaster.createrns.compat.kubejs;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTestHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSKubeJSBuilderTestRunner {
    public static void runAll(GameTestHelper helper, Class<?> testClass) {
        var tests = Arrays.stream(testClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(RNSKubeJSBuilderTest.class))
                .sorted(Comparator.comparing(Method::getName))
                .toList();

        helper.assertTrue(!tests.isEmpty(), "Expected at least one @RNSKubeJSBuilderTest method in " + testClass.getName());

        Object instance = null;
        var failures = new ArrayList<BuilderTestFailure>();
        for (var method : tests) {
            validateSignature(helper, method);
            if (!Modifier.isStatic(method.getModifiers()) && instance == null) {
                instance = instantiate(testClass);
            }

            var failure = runSingle(helper, instance, method);
            if (failure != null) {
                failures.add(failure);
            }
        }

        if (!failures.isEmpty()) {
            throw aggregateFailure(testClass, failures);
        }

        helper.succeed();
    }

    private static void validateSignature(GameTestHelper helper, Method method) {
        helper.assertTrue(method.getParameterCount() == 1,
                "@RNSKubeJSBuilderTest method must accept exactly one parameter: " + method.getName());
        helper.assertTrue(method.getParameterTypes()[0] == RNSKubeJSBuilderTestContext.class,
                "@RNSKubeJSBuilderTest method must accept RNSKubeJSBuilderTestContext: " + method.getName());
    }

    private static Object instantiate(Class<?> testClass) {
        try {
            var ctor = testClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate builder test class " + testClass.getName(), e);
        }
    }

    private static BuilderTestFailure runSingle(GameTestHelper helper, Object instance, Method method) {
        try {
            method.setAccessible(true);
            var target = Modifier.isStatic(method.getModifiers()) ? null : instance;
            method.invoke(target, new RNSKubeJSBuilderTestContext(helper));
            return null;
        } catch (InvocationTargetException e) {
            return new BuilderTestFailure(method, e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to invoke builder test " + method.getName(), e);
        }
    }

    private static AssertionError aggregateFailure(Class<?> testClass, List<BuilderTestFailure> failures) {
        var message = new StringBuilder("Builder test failures in ")
                .append(testClass.getName())
                .append(" (")
                .append(failures.size())
                .append("): ");

        for (var i = 0; i < failures.size(); i++) {
            var failure = failures.get(i);
            if (i > 0) {
                message.append(" || ");
            }

            message.append(failure.method().getName())
                    .append(" => ")
                    .append(describeFailure(failure.cause()));
        }

        var aggregated = new AssertionError(message.toString());
        for (var failure : failures) {
            aggregated.addSuppressed(failure.cause());
        }
        return aggregated;
    }

    private static String describeFailure(Throwable cause) {
        var message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return cause.getClass().getSimpleName() + ": " + message;
    }

    private record BuilderTestFailure(Method method, Throwable cause) {
    }
}

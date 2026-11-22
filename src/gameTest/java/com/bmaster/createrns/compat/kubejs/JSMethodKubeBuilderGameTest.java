package com.bmaster.createrns.compat.kubejs;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.kubejs.deposit.structures.CreateStructureKubeBuilder;
import com.bmaster.createrns.compat.kubejs.util.JSMethod;
import com.bmaster.createrns.compat.kubejs.util.StartupErrorTestHelper;
import com.bmaster.createrns.compat.kubejs.util.StartupJSObject;
import com.bmaster.createrns.compat.kubejs.util.StartupKubeBuilder;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.ConsoleLine;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.rhino.*;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

import static com.bmaster.createrns.compat.kubejs.RNSKubeJSBuilderTestAssertions.assertThrows;

@GameTestHolder(CreateRNS.ID)
@PrefixGameTestTemplate(false)
public class JSMethodKubeBuilderGameTest {
    private static final String SCRIPT_NAME = "js_method_builder.js";
    private static final String FAILURE_MESSAGE = "Expected a positive value";
    private static final SourceLine INNER_SOURCE = new SourceLine("inner_callback.js", 7);

    @GameTest(template = "empty16x16")
    public void jsMethodBuilderScenarios(GameTestHelper helper) {
        RNSKubeJSBuilderTestRunner.runAll(helper, JSMethodKubeBuilderGameTest.class);
    }

    @RNSKubeJSBuilderTest
    private static void structureMethodsSupportChaining(RNSKubeJSBuilderTestContext context) {
        var cx = new ContextFactory().enter();
        var scope = cx.initStandardObjects();
        var builder = new CreateStructureKubeBuilder(CreateRNS.asResource("sourced_test"));
        scope.put(cx, "builder", scope, cx.javaToJS(builder, scope));

        var result = cx.evaluateString(scope, """
                builder.block('minecraft:stone').scannerIcon('minecraft:coal').preset('overworld_common')
                    .biome('#minecraft:is_overworld')
                    .nbt('create_rns:ore_deposit_small', 2)
                    .weight(3)
                    .height(-10);
                """, SCRIPT_NAME, 1, null);
        context.helper().assertTrue(Wrapper.unwrapped(result) == builder, "Chaining must return the same builder");
        context.helper().assertValueEqual(builder.structureSetWeight(), 3, "Configured structure weight");
        context.helper().assertTrue(builder.isValid(), "Valid JavaScript calls must not invalidate the builder");

        var issues = StartupErrorTestHelper.capture(() -> cx.evaluateString(scope, "builder.weight(0)",
                SCRIPT_NAME, 1, null));
        context.helper().assertValueEqual(issues.size(), 1, "Rejected weight reports once");
        context.helper().assertValueEqual(issues.getFirst().message(), "Structure set weight must be positive",
                "Structure validation error");
        context.helper().assertValueEqual(builder.structureSetWeight(), 3, "Rejected weight preserves the prior value");
        context.helper().assertFalse(builder.isValid(), "Invalid JavaScript calls must invalidate the builder");
    }

    @RNSKubeJSBuilderTest
    private static void blockSetterIsOnlyAvailableOnCreatedStructures(RNSKubeJSBuilderTestContext context) {
        var cx = new ContextFactory().enter();
        var scope = cx.initStandardObjects();
        scope.put(cx, "event", scope, cx.javaToJS(context.depositStructures(), scope));
        var result = cx.evaluateString(scope, """
                var created = event.create('create_rns:test_created_structure').height(-10);
                created.block('minecraft:stone');
                var tweaked = event.tweak('create_rns:deposit_iron').height(-10);
                !('block' in tweaked) && typeof tweaked['block(java.lang.String)'] === 'undefined';
                """, SCRIPT_NAME, 1, null);
        context.helper().assertValueEqual(result, true, "Only create() exposes the block setter after chaining");
    }

    @RNSKubeJSBuilderTest
    private static void overloadsAndHiddenMethodsRemainIntact(RNSKubeJSBuilderTestContext context) {
        var cx = new ContextFactory().enter();
        var scope = cx.initStandardObjects();
        var builder = new TestBuilder();
        scope.put(cx, "builder", scope, cx.javaToJS(builder, scope));
        cx.evaluateString(scope, "builder.value(4).value('hello')", SCRIPT_NAME, 1, null);
        context.helper().assertValueEqual(builder.value, "hello", "String overload");
        cx.evaluateString(scope, "builder['value(int)'](5)", SCRIPT_NAME, 1, null);
        context.helper().assertValueEqual(builder.value, "5", "Explicit numeric overload");

        var result = cx.evaluateString(scope, """
                !('hidden' in builder) && !('isValid' in builder) && !('convertJavaToJs' in builder)
                    && builder.value === builder.value
                """, SCRIPT_NAME, 1, null);
        context.helper().assertValueEqual(result, true, "Hidden methods and stable function identity");
        expectScriptError(cx, scope, "builder['value(int)'](0)");
        context.helper().assertFalse(builder.isValid(), "Explicit overloads must also be sourced");
    }

    @RNSKubeJSBuilderTest
    private static void capturedFunctionsUseInvocationSourceAndReceiver(RNSKubeJSBuilderTestContext context) {
        var cx = new ContextFactory().enter();
        var scope = cx.initStandardObjects();
        var first = new TestBuilder();
        var second = new TestBuilder();
        scope.put(cx, "first", scope, cx.javaToJS(first, scope));
        scope.put(cx, "second", scope, cx.javaToJS(second, scope));
        var error = expectScriptError(cx, scope, """
                var saved = first.value;

                saved.call(second, 0);
                """);
        context.helper().assertTrue(first.isValid(), "Property lookup must not invalidate the original builder");
        context.helper().assertFalse(second.isValid(), "Function.call must invalidate the invocation receiver");
        context.helper().assertValueEqual(error.getMessage(), FAILURE_MESSAGE, "Original validation message");
        context.helper().assertTrue(error.getCause() instanceof IllegalArgumentException, "Preserve original cause");
        var line = new ConsoleLine(ConsoleJS.STARTUP, 0, error.getMessage());
        error.apply(line);
        context.helper().assertValueEqual(line.sourceLines.iterator().next(), new SourceLine(SCRIPT_NAME, 3),
                "Error source must be the call line, not the property lookup line");
    }

    @RNSKubeJSBuilderTest
    private static void scriptCanCatchErrorsAndInnerSourceIsPreserved(RNSKubeJSBuilderTestContext context) {
        var cx = new ContextFactory().enter();
        var scope = cx.initStandardObjects();
        var builder = new TestBuilder();
        scope.put(cx, "builder", scope, cx.javaToJS(builder, scope));
        var caught = cx.evaluateString(scope, """
                var caught = false;
                try { builder.value(0); } catch (error) { caught = true; }
                caught;
                """, SCRIPT_NAME, 1, null);
        context.helper().assertValueEqual(caught, true, "Sourced errors must remain catchable in JavaScript");
        context.helper().assertFalse(builder.isValid(), "Catching the error must not restore the builder");

        var error = expectScriptError(cx, scope, "builder.failWithSource()");
        context.helper().assertTrue(error == builder.innerError, "Preserve the existing KubeJS exception");
        var line = new ConsoleLine(ConsoleJS.STARTUP, 0, error.getMessage());
        error.apply(line);
        context.helper().assertValueEqual(line.sourceLines.iterator().next(), INNER_SOURCE, "Preserve inner source");
    }

    @RNSKubeJSBuilderTest
    private static void directJavaCallsRemainUnwrapped(RNSKubeJSBuilderTestContext context) {
        var builder = new TestBuilder();
        try {
            builder.value(0);
            throw new AssertionError("Expected validation failure");
        } catch (IllegalArgumentException error) {
            context.helper().assertValueEqual(error.getMessage(), FAILURE_MESSAGE, "Direct Java error");
        }
        context.helper().assertTrue(builder.isValid(), "Direct Java calls must keep their existing behavior");
    }

    @RNSKubeJSBuilderTest
    private static void onlyAnnotatedOverloadsAndNoFieldsAreExposed(RNSKubeJSBuilderTestContext context) {
        var cx = new ContextFactory().enter();
        var scope = cx.initStandardObjects();
        var builder = new TestBuilder();
        scope.put(cx, "builder", scope, cx.javaToJS(builder, scope));
        var visible = cx.evaluateString(scope, """
                builder.only(2);
                var blocked = false;
                try { builder.secret = 'changed'; } catch (error) { blocked = true; }
                blocked && typeof builder.secret === 'undefined'
                    && typeof builder.hidden === 'undefined'
                    && typeof builder.getSecret === 'undefined'
                    && typeof builder.getClass === 'undefined'
                    && typeof builder['only(java.lang.String,int)'] === 'undefined'
                    && typeof builder[0] === 'undefined'
                    && Object.keys(builder).sort().join(',') === 'failWithSource,only,value';
                """, SCRIPT_NAME, 1, null);
        context.helper().assertValueEqual(visible, true, "Only annotated methods are readable and enumerable");
        context.helper().assertValueEqual(builder.secret, "unchanged", "Hidden fields cannot be written");
        expectScriptError(cx, scope, "builder.only('hidden', 2)");
        context.helper().assertFalse(builder.isValid(), "Overload resolution errors invalidate the builder too");
    }

    @RNSKubeJSBuilderTest
    private static void inheritedAnnotationsCannotExposeUnannotatedOverrides(RNSKubeJSBuilderTestContext context) {
        var cx = new ContextFactory().enter();
        var scope = cx.initStandardObjects();
        scope.put(cx, "parent", scope, cx.javaToJS(new TestBuilder(), scope));
        scope.put(cx, "child", scope, cx.javaToJS(new ChildBuilder(), scope));
        var result = cx.evaluateString(scope, """
                child.value(2);
                var rejected = false;
                try { parent.only.call(child, 2); } catch (error) { rejected = true; }
                rejected && typeof child.only === 'undefined' && typeof child.value === 'function';
                """, SCRIPT_NAME, 1, null);
        context.helper().assertValueEqual(result, true, "Overrides must explicitly opt in to the JavaScript API");
    }

    @RNSKubeJSBuilderTest
    private static void eventFactoriesCaptureCreationSourceAndDeferredErrorsUseIt(RNSKubeJSBuilderTestContext context) {
        var cx = new ContextFactory().enter();
        var scope = cx.initStandardObjects();
        scope.put(cx, "event", scope, cx.javaToJS(context.depositStructures(), scope));
        cx.evaluateString(scope, """
                var first = event.create('create_rns:first_source');
                first.block('minecraft:stone');
                first.scannerIcon('missing:scanner_item');
                first.nbt('create_rns:ore_deposit_small', 1);
                var second = event.create('create_rns:second_source');
                second.block('minecraft:stone');
                second.scannerIcon('minecraft:coal');
                second.nbt('create_rns:ore_deposit_small', 1);
                """, SCRIPT_NAME, 1, null);
        var issues = context.validationErrors();
        context.helper().assertValueEqual(issues.size(), 1, "One unresolved scanner icon definition");
        var issue = issues.getFirst();
        context.helper().assertTrue(issue.message().contains("None of the scanner icon items exist"),
                "Full-context scanner validation");
        context.helper().assertValueEqual(issue.source(), new SourceLine(SCRIPT_NAME, 1),
                "Deferred validation points at create(), not scannerIcon()");
        context.helper().assertValueEqual(context.depositStructures().created().get(1).creationSource(),
                new SourceLine(SCRIPT_NAME, 5), "Each builder retains its own creation source");
    }

    @RNSKubeJSBuilderTest
    private static void recordedErrorsUseActiveCallThenCreationLine(RNSKubeJSBuilderTestContext context) {
        var cx = new ContextFactory().enter();
        var scope = cx.initStandardObjects();
        var event = new ReportingEvent();
        scope.put(cx, "event", scope, cx.javaToJS(event, scope));

        var issues = StartupErrorTestHelper.capture(() -> {
            cx.evaluateString(scope, """
                    var builder = event.create();
                    builder.withCallback(() => {
                        builder.reportIssue();
                    });
                    try { builder.value(0); } catch (ignored) {}
                    """, SCRIPT_NAME, 1, null);
            event.builder.error("After script", false);
        });
        var helper = context.helper();
        helper.assertValueEqual(issues.size(), 3, "Expected inner, outer and deferred errors");
        helper.assertValueEqual(issues.get(0).source(), new SourceLine(SCRIPT_NAME, 3), "Nested JS call line");
        helper.assertValueEqual(issues.get(1).source(), new SourceLine(SCRIPT_NAME, 2), "Restored outer JS call line");
        helper.assertValueEqual(issues.get(2).source(), new SourceLine(SCRIPT_NAME, 1), "Creation line after throw");
    }

    @RNSKubeJSBuilderTest
    private static void rejectedCatalystSetterAllowsTheScriptToContinue(RNSKubeJSBuilderTestContext context) {
        var cx = new ContextFactory().enter();
        var scope = cx.initStandardObjects();
        scope.put(cx, "event", scope, cx.javaToJS(context.catalysts(), scope));
        var issues = StartupErrorTestHelper.capture(() -> cx.evaluateString(scope, """
                var builder = event.create('create_rns:bad_item').fluid('water', 1);
                builder.representativeItem('missing:not_an_item').displayName('Invalid catalyst');
                event.create('create_rns:bad_attachment').attachment(null);
                event.create('create_rns:good_item').fluid('water', 1);
                """, SCRIPT_NAME, 1, null));
        context.helper().assertValueEqual(issues.size(), 2, "Expected both rejected setters");
        var issue = issues.getFirst();
        context.helper().assertTrue(issue.message().contains("Unknown item"), "Method-specific registry error");
        context.helper().assertValueEqual(issue.source(), new SourceLine(SCRIPT_NAME, 2),
                "Immediate error attributed to representativeItem()");
        context.helper().assertTrue(issues.get(1).message().contains("Attachment must be a block id"),
                "A null attachment reports the expected input");
        context.helper().assertFalse(context.catalysts().created().getFirst().isValid(), "Failed catalyst excluded");
        context.helper().assertValueEqual(context.assembleData().size(), 1, "Unaffected catalyst still generates");
        context.helper().assertTrue(context.assembleLang().map().isEmpty(), "Failed chain emitted no display name");
    }

    @RNSKubeJSBuilderTest
    private static void repeatedDimensionFactoryPreservesFirstCreationLine(RNSKubeJSBuilderTestContext context) {
        var cx = new ContextFactory().enter();
        var scope = cx.initStandardObjects();
        scope.put(cx, "event", scope, cx.javaToJS(context.structureSet(), scope));
        cx.evaluateString(scope, """
                event.overworld().spacing(4);
                event.overworld().separation(4);
                """, SCRIPT_NAME, 1, null);
        var issue = context.validationErrors().getFirst();
        context.helper().assertValueEqual(issue.source(), new SourceLine(SCRIPT_NAME, 1),
                "Shared dimension builder retains its first factory call");
    }

    @RNSKubeJSBuilderTest
    private static void realBuilderOverloadsRetainRhinoArgumentConversion(RNSKubeJSBuilderTestContext context) {
        var cx = new ContextFactory().enter();
        var scope = cx.initStandardObjects();
        scope.put(cx, "catalysts", scope, cx.javaToJS(context.catalysts(), scope));
        scope.put(cx, "deposits", scope, cx.javaToJS(context.structureSet(), scope));
        var result = cx.evaluateString(scope, """
                catalysts.create('create_rns:js_arguments')
                    .attachment(['stone', 'cobblestone'], 2)
                    .attachment('andesite')
                    .fluid('water', 10);
                deposits.overworld()
                    .deposit('create_rns:deposit_iron', 7)
                    .deposit('create_rns:deposit_gold', false)
                    .deposit('create_rns:deposit_coal', null, true);
                typeof catalysts.created === 'undefined' && typeof deposits.selectedStructuresByDimension === 'undefined';
                """, SCRIPT_NAME, 1, null);
        context.helper().assertValueEqual(result, true, "Event helpers are hidden too");
        context.helper().assertTrue(context.validationErrors().isEmpty(), "Converted arguments are valid");
        var entries = context.structureSet().overworld().worldgenStructures();
        context.helper().assertValueEqual(entries.size(), 2, "Boolean overload disables only gold worldgen");
        context.helper().assertValueEqual(entries.getFirst().resolvedWeight(), 7, "Integer overload supplies weight");
        context.helper().assertValueEqual(context.catalysts().created().getFirst().attachmentBlocks().size(), 3,
                "Array and scalar attachment overloads both work");
    }

    @RNSKubeJSBuilderTest
    private static void validityIsLazyCachedAndCanBeInvalidated(RNSKubeJSBuilderTestContext context) {
        var helper = context.helper();
        var builder = new ValidationBuilder(ignored -> {});
        helper.assertValueEqual(builder.validationCalls, 0, "Construction must not validate");
        helper.assertTrue(builder.isValid() && builder.isValid(), "Successful result is cached");
        helper.assertValueEqual(builder.validationCalls, 1, "Validation runs once");
        builder.invalidate();
        helper.assertFalse(builder.isValid(), "Explicit invalidation overrides a cached valid result");
        helper.assertValueEqual(builder.validationCalls, 1, "Invalidation must not rerun validation");

        var alreadyInvalid = new ValidationBuilder(ignored -> {
            throw new AssertionError("An invalidated builder must not validate");
        });
        alreadyInvalid.invalidate();
        helper.assertFalse(alreadyInvalid.isValid(), "Invalidation before the first check is permanent");
        helper.assertValueEqual(alreadyInvalid.validationCalls, 0, "Already invalid builders skip validation");
    }

    @RNSKubeJSBuilderTest
    private static void fatalAndNonfatalValidationErrorsAreReportedOnce(RNSKubeJSBuilderTestContext context) {
        for (boolean fatal : new boolean[]{true, false}) {
            var builder = new ValidationBuilder(value -> value.error(FAILURE_MESSAGE, fatal));
            var errors = StartupErrorTestHelper.capture(() -> {
                context.helper().assertValueEqual(builder.isValid(), !fatal, "Fatal flag determines validity");
                context.helper().assertValueEqual(builder.isValid(), !fatal, "Cached validity is unchanged");
            });
            context.helper().assertValueEqual(builder.validationCalls, 1, "Validation executes once");
            context.helper().assertValueEqual(errors.size(), 1, "Validation diagnostic is not repeated");
            context.helper().assertValueEqual(errors.getFirst().message(), FAILURE_MESSAGE, "Original diagnostic");
        }
    }

    @RNSKubeJSBuilderTest
    private static void unexpectedValidationFailureDoesNotCacheSuccess(RNSKubeJSBuilderTestContext context) {
        var builder = new ValidationBuilder(ignored -> {
            throw new IllegalStateException(FAILURE_MESSAGE);
        });
        assertThrows(context.helper(), IllegalStateException.class, FAILURE_MESSAGE, builder::isValid);
        context.helper().assertFalse(builder.isValid(), "A thrown validation failure leaves the builder invalid");
        context.helper().assertValueEqual(builder.validationCalls, 1, "A failed validation is not retried");
    }

    private static KubeRuntimeException expectScriptError(Context cx, Scriptable scope, String script) {
        try {
            cx.evaluateString(scope, script, SCRIPT_NAME, 1, null);
        } catch (WrappedException error) {
            if (error.getWrappedException() instanceof KubeRuntimeException sourced) return sourced;
            throw new AssertionError("Expected a sourced KubeJS exception", error);
        }
        throw new AssertionError("Expected JavaScript evaluation to fail");
    }

    // Public so Rhino can invoke the fixture through its normal Java reflection path.
    public static class TestBuilder extends StartupKubeBuilder {
        public String secret = "unchanged";
        private final KubeRuntimeException innerError = new KubeRuntimeException(FAILURE_MESSAGE).source(INNER_SOURCE);
        private String value = "";

        public TestBuilder() {
            super("rnsDepositStructures", "create|tweak");
        }

        @JSMethod
        public TestBuilder value(int number) {
            if (number <= 0) throw new IllegalArgumentException(FAILURE_MESSAGE);
            value = Integer.toString(number);
            return this;
        }

        @JSMethod
        public TestBuilder value(String text) {
            value = text;
            return this;
        }

        @JSMethod
        public TestBuilder failWithSource() {
            throw innerError;
        }

        @Override
        protected void assertValid() {
        }

        @JSMethod
        public TestBuilder only(int number) {
            return value(number);
        }

        public TestBuilder only(String text, int number) {
            throw new AssertionError("Unannotated overload must not be called");
        }

        public String getSecret() {
            return secret;
        }

        public void hidden() {
            throw new AssertionError("Hidden methods must not be callable from JavaScript");
        }
    }

    public static class ReportingEvent extends StartupJSObject {
        private final ReportingBuilder builder = new ReportingBuilder();

        public ReportingEvent() {
            super("rnsDepositStructures", "create");
        }

        @JSMethod
        public ReportingBuilder create() {
            return builder;
        }
    }

    public static class ReportingBuilder extends TestBuilder {
        @JSMethod
        public void reportIssue() {
            error("Inside script", false);
        }

        @JSMethod
        public void withCallback(Runnable callback) {
            callback.run();
            reportIssue();
        }
    }

    public static class ChildBuilder extends TestBuilder {
        @Override
        public TestBuilder only(int number) {
            throw new AssertionError("Unannotated override must not be called");
        }
    }

    private static class ValidationBuilder extends StartupKubeBuilder {
        private final Consumer<ValidationBuilder> validator;
        private int validationCalls;

        private ValidationBuilder(Consumer<ValidationBuilder> validator) {
            super("rnsDepositStructures", "create|tweak");
            this.validator = validator;
        }

        @Override
        protected void assertValid() {
            validationCalls++;
            validator.accept(this);
        }
    }
}

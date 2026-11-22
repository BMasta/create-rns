package com.bmaster.createrns.compat.kubejs.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exposes a public instance method of {@link StartupJSObject} and attributes failures to its script call.
 * Each overload and overriding implementation must opt in separately. All other Java members stay hidden.
 * Builder calls that throw a runtime exception also invalidate the builder. Direct Java calls are unaffected.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JSMethod {
}

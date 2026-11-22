package com.bmaster.createrns.compat.kubejs.util;

import java.lang.annotation.*;

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

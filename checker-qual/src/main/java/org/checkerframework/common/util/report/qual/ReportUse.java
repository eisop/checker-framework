package org.checkerframework.common.util.report.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Report all uses of a type that has this annotation. Can also be used on a package.
 *
 * <p>When written on a package, {@code @ReportUse} applies to that package and its subpackages by
 * default. Set {@link #applyToSubpackages()} to false to limit it to the package itself; doing so
 * does not block an applicable {@code @ReportUse} on an enclosing package.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface ReportUse {

    /**
     * When used on a package, whether this annotation should also apply to subpackages.
     *
     * @return whether this annotation should be inherited by subpackages
     */
    boolean applyToSubpackages() default true;
}

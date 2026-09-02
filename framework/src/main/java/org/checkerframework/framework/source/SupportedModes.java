package org.checkerframework.framework.source;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to indicate which values for the {@code -Amode} option a checker supports.
 * Each mode enables a checker-defined group of options. The {@link SourceChecker#getSupportedModes}
 * method constructs its result from the value of this annotation and the modes supported by
 * subcheckers.
 *
 * @see SupportedOptions
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface SupportedModes {
    String[] value();
}

package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation on a SourceChecker subclass to provide additional stub files that should be used in
 * addition to {@code jdk.astub}. This allows larger compound checkers to separate the annotations
 * into multiple files, or to provide annotations for non-JDK classes.
 *
 * <p>A checker inherits the stub files of the nearest superclass that declares this annotation,
 * unless it declares its own {@code @StubFiles} annotation. Furthermore, stub files are shared
 * bidirectionally between compound checkers and their subcheckers.
 *
 * @checker_framework.manual #creating-a-checker-annotated-jdk Annotated JDK
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StubFiles {
    /**
     * Stub file names. These are basenames: they include the extension (usually ".astub"), but no
     * directory component.
     */
    String[] value();
}

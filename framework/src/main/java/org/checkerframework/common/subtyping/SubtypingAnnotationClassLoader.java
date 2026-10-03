package org.checkerframework.common.subtyping;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotationClassLoader;

/** The annotation class loader of the Subtyping Checker. */
public class SubtypingAnnotationClassLoader extends AnnotationClassLoader {

    /**
     * Creates a SubtypingAnnotationClassLoader.
     *
     * @param checker the checker whose qualifiers are loaded
     */
    public SubtypingAnnotationClassLoader(BaseTypeChecker checker) {
        super(checker);
    }
}

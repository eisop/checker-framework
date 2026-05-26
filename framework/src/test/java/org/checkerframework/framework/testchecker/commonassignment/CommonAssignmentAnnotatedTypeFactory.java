package org.checkerframework.framework.testchecker.commonassignment;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.commonassignment.quals.CommonBottom;
import org.checkerframework.framework.testchecker.commonassignment.quals.CommonInvalid;
import org.checkerframework.framework.testchecker.commonassignment.quals.CommonTop;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Annotated type factory for {@link CommonAssignmentChecker}. */
public class CommonAssignmentAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * Creates a {@link CommonAssignmentAnnotatedTypeFactory}.
     *
     * @param checker the associated checker
     */
    @SuppressWarnings("this-escape")
    public CommonAssignmentAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<Class<? extends Annotation>>(
                Arrays.asList(CommonTop.class, CommonBottom.class, CommonInvalid.class));
    }
}

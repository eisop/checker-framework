package org.checkerframework.framework.testchecker.nontopdefault;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDMiddle;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;

import javax.lang.model.element.AnnotationMirror;

/** The annotated type factory for the StubBound test checker. */
public class StubBoundAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    /**
     * Create a StubBoundAnnotatedTypeFactory.
     *
     * @param checker the checker to which this factory belongs
     */
    @SuppressWarnings("this-escape")
    public StubBoundAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defs) {
        super.addCheckedCodeDefaults(defs);
        AnnotationMirror middle = AnnotationBuilder.fromClass(elements, NTDMiddle.class);
        defs.addCheckedCodeDefault(middle, TypeUseLocation.UPPER_BOUND);
        defs.addUncheckedCodeDefault(middle, TypeUseLocation.UPPER_BOUND);
    }
}

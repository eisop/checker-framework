package org.checkerframework.framework.testchecker.standalonediamond;

import com.sun.source.tree.NewClassTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.type.TypeMirror;

/**
 * The visitor for the standalone diamond test checker. In addition to the usual checks, it reports
 * {@code nodevalue.underlying.type} for a diamond whose dataflow value has an underlying type other
 * than javac's type of the diamond.
 */
public class StandaloneDiamondVisitor
        extends BaseTypeVisitor<StandaloneDiamondAnnotatedTypeFactory> {

    /**
     * Creates a new StandaloneDiamondVisitor.
     *
     * @param checker the checker
     */
    public StandaloneDiamondVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void p) {
        if (TreeUtils.isDiamondTree(tree)) {
            CFValue value = atypeFactory.getInferredValueFor(tree);
            TypeMirror javacType = TreeUtils.typeOf(tree);
            if (value != null) {
                if (!types.isSameType(value.getUnderlyingType(), javacType)) {
                    checker.reportError(
                            tree,
                            "nodevalue.underlying.type",
                            value.getUnderlyingType(),
                            javacType);
                }
                AnnotationMirrorSet qualifiers =
                        atypeFactory.getAnnotatedType(tree).getAnnotations();
                if (!AnnotationUtils.areSame(value.getAnnotations(), qualifiers)) {
                    checker.reportError(
                            tree, "nodevalue.qualifier", value.getAnnotations(), qualifiers);
                }
            }
        }
        return super.visitNewClass(tree, p);
    }
}

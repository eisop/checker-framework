package org.checkerframework.framework.testchecker.h1h2checker;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Invalid;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;

import javax.lang.model.element.AnnotationMirror;

public class H1H2Visitor extends BaseTypeVisitor<H1H2AnnotatedTypeFactory> {

    public H1H2Visitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new H1H2TypeValidator(checker, this, atypeFactory);
    }

    @Override
    protected boolean commonAssignmentCheck(
            Tree varTree,
            ExpressionTree valueExpTree,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        boolean superResult =
                super.commonAssignmentCheck(varTree, valueExpTree, errorKey, extraArgs);
        AnnotationMirror h1Invalid = AnnotationBuilder.fromClass(elements, H1Invalid.class);
        if (superResult
                && varTree.toString().contains("commonAssignment")
                && AnnotatedTypes.containsModifier(
                        atypeFactory.getAnnotatedType(varTree), h1Invalid)) {
            checker.reportWarning(varTree, "h1h2checker.commonassignment.parent.succeeded");
        }
        return superResult;
    }

    private final class H1H2TypeValidator extends BaseTypeValidator {

        H1H2TypeValidator(
                BaseTypeChecker checker,
                BaseTypeVisitor<?> visitor,
                AnnotatedTypeFactory atypeFactory) {
            super(checker, visitor, atypeFactory);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Tree p) {
            AnnotationMirror h1Invalid = AnnotationBuilder.fromClass(elements, H1Invalid.class);
            if (AnnotatedTypes.containsModifier(type, h1Invalid)) {
                reportInvalidType(type, p);
            }
            return super.visitDeclared(type, p);
        }
    }
}

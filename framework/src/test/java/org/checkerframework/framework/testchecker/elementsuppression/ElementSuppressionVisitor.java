package org.checkerframework.framework.testchecker.elementsuppression;

import com.sun.source.tree.ClassTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.Element;

/** Reports a diagnostic on selected class elements to test element-based suppression. */
public class ElementSuppressionVisitor
        extends BaseTypeVisitor<org.checkerframework.common.basetype.BaseAnnotatedTypeFactory> {

    /**
     * Creates a new ElementSuppressionVisitor.
     *
     * @param checker the checker
     */
    public ElementSuppressionVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public void processClassTree(ClassTree node) {
        Element elt = TreeUtils.elementFromDeclaration(node);
        if (elt != null && elt.getSimpleName().toString().startsWith("ReportOnMe")) {
            // report a custom error on the Element itself!
            // "type.invalid" is a built-in error key we can misuse for testing.
            checker.reportError(elt, "type.invalid", "mock type", "mock message");
        }
        super.processClassTree(node);
    }
}

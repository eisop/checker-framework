package org.checkerframework.framework.testchecker.elementsuppression;

import org.checkerframework.common.basetype.BaseTypeChecker;

import javax.lang.model.element.Element;

/** A test checker for suppression of diagnostics reported on elements. */
public class ElementSuppressionChecker extends BaseTypeChecker {

    /**
     * Also suppresses every diagnostic within a declaration whose name starts with
     * "SuppressedByOverride", to test that an override of this overload applies to a diagnostic
     * reported on a tree, too.
     */
    @Override
    public boolean shouldSuppressWarnings(Element elt, String errKey) {
        for (Element e = elt; e != null; e = e.getEnclosingElement()) {
            if (e.getSimpleName().toString().startsWith("SuppressedByOverride")) {
                return true;
            }
        }
        return super.shouldSuppressWarnings(elt, errKey);
    }
}

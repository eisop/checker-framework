package org.checkerframework.checker.pico;

import static org.checkerframework.checker.pico.PICOAnnotationMirrorHolder.MUTABLE;
import static org.checkerframework.checker.pico.PICOAnnotationMirrorHolder.READONLY;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;

public class ObjectIdentityMethodEnforcer extends TreePathScanner<Void, Void> {

    private PICONoInitAnnotatedTypeFactory typeFactory;
    private BaseTypeChecker checker;

    private ObjectIdentityMethodEnforcer(
            PICONoInitAnnotatedTypeFactory typeFactory, BaseTypeChecker checker) {
        this.typeFactory = typeFactory;
        this.checker = checker;
    }

    // Main entry
    public static void check(
            TreePath statement,
            PICONoInitAnnotatedTypeFactory typeFactory,
            BaseTypeChecker checker) {
        if (statement == null) return;
        ObjectIdentityMethodEnforcer asfchecker =
                new ObjectIdentityMethodEnforcer(typeFactory, checker);
        asfchecker.scan(statement, null);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
        Element elt = TreeUtils.elementFromUse(node);
        checkMethod(node, elt);
        return super.visitMethodInvocation(node, aVoid);
    }

    private void checkMethod(MethodInvocationTree node, Element elt) {
        assert elt instanceof ExecutableElement;
        if (ElementUtils.isStatic(elt)) {
            return; // Doesn't check static method invocation because it doesn't access instance
            // field
        }
        if (!PICOTypeUtil.isObjectIdentityMethod((ExecutableElement) elt, typeFactory)) {
            // Report warning since invoked method is not only dependent on abstract state fields,
            // but we
            // don't know whether this method invocation's result flows into the hashcode or not.
            checker.reportWarning(node, "object.identity.method.invocation.invalid", elt);
        }
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void aVoid) {
        Element elt = TreeUtils.elementFromUse(node);
        checkField(node, elt);
        return super.visitIdentifier(node, aVoid);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void aVoid) {
        Element elt = TreeUtils.elementFromUse(node);
        checkField(node, elt);
        return super.visitMemberSelect(node, aVoid);
    }

    private void checkField(Tree node, Element elt) {
        if (elt == null) return;
        if (elt.getSimpleName().contentEquals("this")
                || elt.getSimpleName().contentEquals("super")) {
            return;
        }
        if (elt.getKind() == ElementKind.FIELD) {
            if (ElementUtils.isStatic(elt)) {
                checker.reportWarning(node, "object.identity.static.field.access.forbidden", elt);
            } else {
                if (!isInAbstractState(elt, typeFactory)) {
                    // Report warning since accessed field is not within abstract state
                    checker.reportWarning(node, "object.identity.field.access.invalid", elt);
                }
            }
        }
    }

    // Deeply test if a field is in abstract state or not. For composite types: array component,
    // type arguments, upper bound of type parameter uses are also checked.
    private boolean isInAbstractState(Element elt, PICONoInitAnnotatedTypeFactory typeFactory) {
        boolean in = true;
        if (PICOTypeUtil.isAssignableField(elt, typeFactory)) {
            in = false;
        } else if (AnnotatedTypes.containsModifier(typeFactory.getAnnotatedType(elt), MUTABLE)) {
            in = false;
        } else if (AnnotatedTypes.containsModifier(typeFactory.getAnnotatedType(elt), READONLY)) {
            in = false;
        }
        return in;
    }
}

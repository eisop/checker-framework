package org.checkerframework.checker.lock;

import com.sun.source.tree.NewArrayTree;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

// Note:
// For any binary operation whose LHS or RHS can be a non-boolean type, and whose resulting
// type is necessarily boolean, the resulting annotation on the boolean type must be
// @GuardedBy({}).

// There is no need to enforce that the annotation on the result of &&, ||, etc.  is
// @GuardedBy({}) since for such operators, both operands are of type @GuardedBy({}) boolean
// to begin with.

// A boolean or String is always @GuardedBy({}). LockVisitor determines whether
// the LHS and RHS of this operation can be legally dereferenced.
public class LockTreeAnnotator extends TreeAnnotator {

    public LockTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
    }

    @Override
    public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror type) {
        if (!type.isAnnotatedInHierarchy(((LockAnnotatedTypeFactory) atypeFactory).NEWOBJECT)) {
            type.replaceAnnotation(((LockAnnotatedTypeFactory) atypeFactory).NEWOBJECT);
        }
        return super.visitNewArray(node, type);
    }
}

package org.checkerframework.checker.initialization;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.initialization.InitializationAnnotatedTypeFactory.InitializationError;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.flow.CFAbstractAnalysis.FieldInitialValue;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.ArraysPlume;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/* NO-AFU
   import org.checkerframework.common.wholeprograminference.WholeProgramInference;
*/

/**
 * The visitor for the freedom-before-commitment type-system. The freedom-before-commitment
 * type-system and this class are abstract and need to be combined with another type-system whose
 * safe initialization should be tracked. For an example, see the {@link NullnessChecker}.
 */
public class InitializationVisitor extends BaseTypeVisitor<InitializationAnnotatedTypeFactory> {

    // Error message keys
    private static final @CompilerMessageKey String COMMITMENT_INVALID_CAST =
            "initialization.invalid.cast";
    private static final @CompilerMessageKey String COMMITMENT_INVALID_FIELD_TYPE =
            "initialization.invalid.field.type";
    private static final @CompilerMessageKey String COMMITMENT_INVALID_CONSTRUCTOR_RETURN_TYPE =
            "initialization.invalid.constructor.return.type";
    private static final @CompilerMessageKey String
            COMMITMENT_INVALID_FIELD_WRITE_UNKNOWN_INITIALIZATION =
                    "initialization.invalid.field.write.unknown";
    private static final @CompilerMessageKey String COMMITMENT_INVALID_FIELD_WRITE_INITIALIZED =
            "initialization.invalid.field.write.initialized";

    /**
     * Create an InitializationVisitor.
     *
     * @param checker the initialization checker
     */
    public InitializationVisitor(BaseTypeChecker checker) {
        super(checker);
        initializedFields = new ArrayList<>();
    }

    @Override
    public void setRoot(CompilationUnitTree root) {
        // Clean up the cache of initialized fields once per compilation unit.
        // Alternatively, but harder to determine, this could be done once per top-level class.
        initializedFields.clear();
        super.setRoot(root);
    }

    @Override
    protected void checkConstructorInvocation(
            AnnotatedDeclaredType dt, AnnotatedExecutableType constructor, NewClassTree src) {
        // Receiver annotations for constructors are forbidden, therefore no check is necessary.
        // TODO: nested constructors can have receivers!
    }

    @Override
    protected void checkConstructorResult(
            AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
        // Nothing to check
    }

    @Override
    protected void checkThisOrSuperConstructorCall(
            MethodInvocationTree superCall, @CompilerMessageKey String errorKey) {
        // Nothing to check
    }

    @Override
    protected void commonAssignmentCheck(
            Tree varTree,
            ExpressionTree valueExp,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        // field write of the form x.f = y
        if (TreeUtils.isFieldAccess(varTree)) {
            // cast is safe: a field access can only be an IdentifierTree or MemberSelectTree
            ExpressionTree lhs = (ExpressionTree) varTree;
            ExpressionTree y = valueExp;
            VariableElement el = TreeUtils.variableElementFromUse(lhs);
            AnnotatedTypeMirror xType = atypeFactory.getReceiverType(lhs);
            AnnotatedTypeMirror yType = atypeFactory.getAnnotatedType(y);
            // the special FBC rules do not apply if there is an explicit
            // UnknownInitialization annotation
            AnnotationMirrorSet fieldAnnotations =
                    atypeFactory.getAnnotatedType(el).getAnnotations();
            if (!AnnotationUtils.containsSameByName(
                    fieldAnnotations, atypeFactory.UNKNOWN_INITIALIZATION)) {
                if (!ElementUtils.isStatic(el)
                        && !(atypeFactory.isInitialized(yType)
                                || atypeFactory.isUnderInitialization(xType)
                                || atypeFactory.isFbcBottom(yType))) {
                    @CompilerMessageKey String err;
                    if (atypeFactory.isInitialized(xType)) {
                        err = COMMITMENT_INVALID_FIELD_WRITE_INITIALIZED;
                    } else {
                        err = COMMITMENT_INVALID_FIELD_WRITE_UNKNOWN_INITIALIZATION;
                    }
                    checker.reportError(varTree, err, varTree);
                    return; // prevent issuing another errow about subtyping
                }
            }
        }
        super.commonAssignmentCheck(varTree, valueExp, errorKey, extraArgs);
    }

    @Override
    protected void checkExceptionParameter(CatchTree node) {
        // TODO Issue 363
        // https://github.com/eisop/checker-framework/issues/363
    }

    @Override
    public Void visitTypeCast(TypeCastTree tree, Void p) {
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(tree.getExpression());
        AnnotatedTypeMirror castType = atypeFactory.getAnnotatedType(tree);
        AnnotationMirror exprAnno = null, castAnno = null;

        // find commitment annotation
        for (Class<? extends Annotation> a : atypeFactory.getSupportedTypeQualifiers()) {
            if (castType.hasAnnotation(a)) {
                assert castAnno == null;
                castAnno = castType.getAnnotation(a);
            }
            if (exprType.hasAnnotation(a)) {
                assert exprAnno == null;
                exprAnno = exprType.getAnnotation(a);
            }
        }

        // TODO: this is most certainly unsafe!! (and may be hiding some problems)
        // If we don't find a commitment annotation, then we just assume that
        // the subtyping is alright.
        // The case that has come up is with wildcards not getting a type for
        // some reason, even though the default is @Initialized.
        boolean isSubtype;
        if (exprAnno == null || castAnno == null) {
            isSubtype = true;
        } else {
            assert exprAnno != null && castAnno != null;
            isSubtype = atypeFactory.getQualifierHierarchy().isSubtype(exprAnno, castAnno);
        }

        if (!isSubtype) {
            checker.reportError(
                    tree,
                    COMMITMENT_INVALID_CAST,
                    atypeFactory.getAnnotationFormatter().formatAnnotationMirror(exprAnno),
                    atypeFactory.getAnnotationFormatter().formatAnnotationMirror(castAnno));
            return p; // suppress cast.unsafe warning
        }

        return super.visitTypeCast(tree, p);
    }

    protected final List<VariableTree> initializedFields;

    @Override
    public void processClassTree(ClassTree tree) {
        // go through all members and look for initializers.
        // save all fields that are initialized and do not report errors about
        // them later when checking constructors.
        for (Tree member : tree.getMembers()) {
            if (member.getKind() == Tree.Kind.BLOCK && !((BlockTree) member).isStatic()) {
                BlockTree block = (BlockTree) member;
                InitializationStore store = atypeFactory.getRegularExitStore(block);

                // Add field values for fields with an initializer.
                for (FieldInitialValue<CFValue> fieldInitialValue :
                        store.getAnalysis().getFieldInitialValues()) {
                    if (fieldInitialValue.initializer != null) {
                        store.addInitializedField(fieldInitialValue.fieldDecl.getField());
                    }
                }
                final List<VariableTree> init =
                        atypeFactory.getInitializedFields(store, getCurrentPath());
                initializedFields.addAll(init);
            }
        }

        super.processClassTree(tree);

        // Warn about uninitialized static fields.
        Tree.Kind nodeKind = tree.getKind();
        // Skip interfaces (and annotations, which are interfaces).  In an interface, every static
        // field must be initialized.  Java forbids uninitialized variables and static initalizer
        // blocks.
        if (nodeKind != Tree.Kind.INTERFACE && nodeKind != Tree.Kind.ANNOTATION_TYPE) {
            // See GenericAnnotatedTypeFactory.performFlowAnalysis for why we use
            // the regular exit store of the class here.
            InitializationStore store = atypeFactory.getRegularExitStore(tree);
            // Add field values for fields with an initializer.
            for (FieldInitialValue<CFValue> fieldInitialValue :
                    store.getAnalysis().getFieldInitialValues()) {
                if (fieldInitialValue.initializer != null) {
                    store.addInitializedField(fieldInitialValue.fieldDecl.getField());
                }
            }

            List<AnnotationMirror> receiverAnnotations = Collections.emptyList();
            checkFieldsInitialized(tree, true, store, receiverAnnotations);
        }
    }

    @Override
    public Void visitMethod(MethodTree tree, Void p) {
        if (TreeUtils.isConstructor(tree)) {
            Collection<? extends AnnotationMirror> returnTypeAnnotations =
                    AnnotationUtils.getExplicitAnnotationsOnConstructorResult(tree);
            // check for invalid constructor return type
            for (Class<? extends Annotation> c : atypeFactory.getSupportedTypeQualifiers()) {
                for (AnnotationMirror a : returnTypeAnnotations) {
                    if (atypeFactory.areSameByClass(a, c)) {
                        checker.reportError(tree, COMMITMENT_INVALID_CONSTRUCTOR_RETURN_TYPE, tree);
                        break;
                    }
                }
            }

            // Check that all fields have been initialized at the end of the constructor.
            boolean isStatic = false;

            InitializationStore store = atypeFactory.getRegularExitStore(tree);
            List<? extends AnnotationMirror> receiverAnnotations = getAllReceiverAnnotations(tree);
            checkFieldsInitialized(tree, isStatic, store, receiverAnnotations);
        }
        return super.visitMethod(tree, p);
    }

    /** The assignment/variable/method invocation tree currently being checked. */
    protected Tree commonAssignmentTree;

    @Override
    public Void visitVariable(VariableTree tree, Void p) {
        Tree oldCommonAssignmentTree = commonAssignmentTree;
        commonAssignmentTree = tree;
        // is this a field (and not a local variable)?
        if (TreeUtils.elementFromDeclaration(tree).getKind().isField()) {
            Set<AnnotationMirror> annotationMirrors =
                    atypeFactory.getAnnotatedType(tree).getExplicitAnnotations();
            // Fields cannot have commitment annotations.
            for (Class<? extends Annotation> c : atypeFactory.getSupportedTypeQualifiers()) {
                for (AnnotationMirror a : annotationMirrors) {
                    if (atypeFactory.isUnknownInitialization(a)) {
                        continue; // unknown initialization is allowed
                    }
                    if (atypeFactory.areSameByClass(a, c)) {
                        checker.reportError(tree, COMMITMENT_INVALID_FIELD_TYPE, tree);
                        break;
                    }
                }
            }
        }
        super.visitVariable(tree, p);
        commonAssignmentTree = oldCommonAssignmentTree;
        return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        Tree oldCommonAssignmentTree = commonAssignmentTree;
        commonAssignmentTree = node;
        super.visitAssignment(node, p);
        commonAssignmentTree = oldCommonAssignmentTree;
        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        Tree oldCommonAssignmentTree = commonAssignmentTree;
        commonAssignmentTree = node;
        super.visitMethodInvocation(node, p);
        commonAssignmentTree = oldCommonAssignmentTree;
        return null;
    }

    @Override
    protected void reportCommonAssignmentError(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        FoundRequired pair = FoundRequired.of(valueType, varType);
        String valueTypeString = pair.found;
        String varTypeString = pair.required;

        InitializationStore store = atypeFactory.getStoreBefore(commonAssignmentTree);

        // If possible, don't report an error directly and let the parent checker call
        // #reportInitializionErrors later.

        // We can't check if all necessary fields are initialized without a store.
        if (store == null) {
            super.reportCommonAssignmentError(varType, valueType, valueTree, errorKey, extraArgs);
            return;
        }

        // We only track field initialization for the current receiver.
        if (!valueTree.toString().equals("this")) {
            super.reportCommonAssignmentError(varType, valueType, valueTree, errorKey, extraArgs);
            return;
        }

        // If the required type is Initialized, we always need to report an error.
        if (varType.getAnnotation(Initialized.class) != null) {
            super.reportCommonAssignmentError(varType, valueType, valueTree, errorKey, extraArgs);
            return;
        }

        List<VariableTree> uninitializedFields =
                atypeFactory.getUninitializedFields(
                        store, getCurrentPath(), false, Collections.emptyList());
        uninitializedFields.removeAll(initializedFields);
        atypeFactory.initializationErrors.put(
                commonAssignmentTree,
                new InitializationError(
                        commonAssignmentTree,
                        uninitializedFields,
                        errorKey,
                        ArraysPlume.concatenate(extraArgs, valueTypeString, varTypeString),
                        true,
                        false));
    }

    @Override
    protected void reportMethodInvocabilityError(
            MethodInvocationTree node, AnnotatedTypeMirror found, AnnotatedTypeMirror expected) {
        if (!TreeUtils.isSelfAccess(node)) {
            super.reportMethodInvocabilityError(node, found, expected);
            return;
        }

        AnnotationMirror unknownInit = expected.getAnnotation(UnknownInitialization.class);
        AnnotationMirror underInit = expected.getAnnotation(UnderInitialization.class);
        TypeMirror frame;
        if (unknownInit != null) {
            frame = atypeFactory.getTypeFrameFromAnnotation(unknownInit);
        } else if (underInit != null) {
            frame = atypeFactory.getTypeFrameFromAnnotation(underInit);
        } else {
            super.reportMethodInvocabilityError(node, found, expected);
            return;
        }

        Type classType = ((JCTree) classTree).type;
        if (!atypeFactory.getProcessingEnv().getTypeUtils().isSubtype(frame, classType)) {
            super.reportMethodInvocabilityError(node, found, expected);
            return;
        }

        List<VariableTree> uninitializedFields =
                atypeFactory.getUninitializedFields(
                        atypeFactory.getStoreBefore(node),
                        getCurrentPath(),
                        false,
                        Collections.emptyList());
        uninitializedFields.removeAll(initializedFields);

        atypeFactory.initializationErrors.put(
                node,
                new InitializationError(
                        node,
                        uninitializedFields,
                        "method.invocation.invalid",
                        new Object[] {found.toString(), expected.toString()},
                        true,
                        false));
    }

    /**
     * Returns the full list of annotations on the receiver.
     *
     * @param tree a method declaration
     * @return all the annotations on the method's receiver
     */
    private List<? extends AnnotationMirror> getAllReceiverAnnotations(MethodTree tree) {
        // TODO: get access to a Types instance and use it to get receiver type
        // Or, extend ExecutableElement with such a method.
        // Note that we cannot use the receiver type from AnnotatedExecutableType, because that
        // would only have the nullness annotations; here we want to see all annotations on the
        // receiver.
        List<? extends AnnotationMirror> rcvannos = null;
        if (TreeUtils.isConstructor(tree)) {
            com.sun.tools.javac.code.Symbol meth =
                    (com.sun.tools.javac.code.Symbol) TreeUtils.elementFromDeclaration(tree);
            rcvannos = meth.getRawTypeAttributes();
            if (rcvannos == null) {
                rcvannos = Collections.emptyList();
            }
        }
        return rcvannos;
    }

    /**
     * Checks that all fields (all static fields if {@code staticFields} is true) are initialized in
     * the given store.
     *
     * @param tree a {@link ClassTree} if {@code staticFields} is true; a {@link MethodTree} for a
     *     constructor if {@code staticFields} is false. This is where errors are reported, if they
     *     are not reported at the fields themselves
     * @param staticFields whether to check static fields or instance fields
     * @param store the store
     * @param receiverAnnotations the annotations on the receiver
     */
    protected void checkFieldsInitialized(
            Tree tree,
            boolean staticFields,
            InitializationStore store,
            List<? extends AnnotationMirror> receiverAnnotations) {
        // If the store is null, then the constructor cannot terminate successfully
        if (store == null) {
            return;
        }

        // Compact canonical record constructors do not generate visible assignments in the source,
        // but by definition they assign to all the record's fields so we don't need to
        // check for uninitialized fields in them:
        if (tree.getKind() == Tree.Kind.METHOD
                && TreeUtils.isCompactCanonicalRecordConstructor((MethodTree) tree)) {
            return;
        }

        List<VariableTree> uninitializedFields =
                atypeFactory.getUninitializedFields(
                        store, getCurrentPath(), staticFields, receiverAnnotations);
        uninitializedFields.removeAll(initializedFields);

        // Errors are issued at the field declaration if the field is static or if the constructor
        // is the default constructor.
        // Errors are issued at the constructor declaration if the field is non-static and the
        // constructor is non-default.
        boolean errorAtField = staticFields || TreeUtils.isSynthetic((MethodTree) tree);

        String errorMsg =
                (staticFields
                        ? "initialization.static.field.uninitialized"
                        : errorAtField
                                ? "initialization.field.uninitialized"
                                : "initialization.fields.uninitialized");

        atypeFactory.initializationErrors.put(
                tree,
                new InitializationError(
                        tree, uninitializedFields, errorMsg, null, false, errorAtField));
    }
}

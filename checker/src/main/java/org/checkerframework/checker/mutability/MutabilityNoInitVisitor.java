package org.checkerframework.checker.mutability;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.basetype.TypeValidator;
import org.checkerframework.framework.type.AnnotatedTypeFactory.ParameterizedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/** The visitor for the mutability type system. */
public class MutabilityNoInitVisitor extends BaseTypeVisitor<MutabilityNoInitAnnotatedTypeFactory> {
    /** Unary operators that mutate their operand. */
    private static final Set<Tree.Kind> SIDE_EFFECTING_UNARY_OPERATORS =
            Set.of(
                    Tree.Kind.POSTFIX_INCREMENT,
                    Tree.Kind.PREFIX_INCREMENT,
                    Tree.Kind.POSTFIX_DECREMENT,
                    Tree.Kind.PREFIX_DECREMENT);

    /**
     * Create a new MutabilityNoInitVisitor.
     *
     * @param checker the checker
     */
    public MutabilityNoInitVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected TypeValidator createTypeValidator() {
        return new MutabilityValidator(checker, this, atypeFactory);
    }

    @Override
    protected void checkConstructorResult(
            AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {}

    // Validate that a mutability qualifier use conforms to the type declaration bound.
    // declarationType comes from AnnotatedTypeFactory#getAnnotatedType(Element), whose result must
    // remain consistent with MutabilityNoInitAnnotatedTypeFactory's class-bound helpers.
    @Override
    public boolean isValidUse(
            AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {

        // FIXME workaround for poly anno, remove after fix substitutable poly and add poly vp rules
        if (useType.hasAnnotation(atypeFactory.POLY_MUTABLE)
                || useType.hasAnnotation(atypeFactory.LOST)) {
            return true;
        }

        AnnotationMirror declared = declarationType.getAnnotationInHierarchy(atypeFactory.READONLY);
        AnnotationMirror used = useType.getAnnotationInHierarchy(atypeFactory.READONLY);

        return isAdaptedSubtype(used, declared);
    }

    @Override
    public boolean isValidUse(AnnotatedArrayType type, Tree tree) {
        // Array declaration bounds are receiver-dependent, so every explicit array qualifier is
        // valid except bottom.
        AnnotationMirror used = type.getAnnotationInHierarchy(atypeFactory.READONLY);
        return !AnnotationUtils.areSame(used, atypeFactory.BOTTOM);
    }

    /**
     * Tests whether the left-hand qualifier is valid against the right-hand declaration bound after
     * viewpoint adaptation.
     *
     * @param lhs the qualifier on the type use
     * @param rhs the qualifier on the declaration bound
     * @return true if the adapted declaration bound is a subtype of the use qualifier
     */
    private boolean isAdaptedSubtype(AnnotationMirror lhs, AnnotationMirror rhs) {
        MutabilityViewpointAdapter vpa = atypeFactory.getViewpointAdapter();
        AnnotationMirror adapted = vpa.combineAnnotationWithAnnotation(lhs, rhs);
        return atypeFactory.getQualifierHierarchy().isSubtypeQualifiersOnly(adapted, lhs);
    }

    @Override
    protected boolean commonAssignmentCheck(
            Tree varTree,
            ExpressionTree valueExp,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        AnnotatedTypeMirror var = atypeFactory.getAnnotatedTypeLhs(varTree);
        assert var != null : "no variable found for tree: " + varTree;

        if (!validateType(varTree, var)) {
            return false;
        }

        if (varTree instanceof VariableTree) {
            VariableElement element = TreeUtils.elementFromDeclaration((VariableTree) varTree);
            if (element.getKind() == ElementKind.FIELD && !ElementUtils.isStatic(element)) {
                AnnotatedTypeMirror bound =
                        atypeFactory.getBoundTypeOfEnclosingTypeDeclaration(varTree);
                // var is shared by the element, so do not mutate it directly.
                AnnotatedTypeMirror varAdapted = var.shallowCopy(true);
                // Viewpoint adaptation mutates varAdapted to the enclosing declaration bound.
                atypeFactory.getViewpointAdapter().viewpointAdaptMember(bound, element, varAdapted);
                // Pass the adapted copy as the lhs type.
                return commonAssignmentCheck(varAdapted, valueExp, errorKey, extraArgs);
            }
        }

        return commonAssignmentCheck(var, valueExp, errorKey, extraArgs);
    }

    @Override
    protected void checkConstructorInvocation(
            AnnotatedDeclaredType invocation,
            AnnotatedExecutableType constructor,
            NewClassTree newClassTree) {
        // Forbid creation of @Readonly Object
        if (invocation.hasAnnotation(atypeFactory.READONLY)) {
            checker.reportError(
                    newClassTree,
                    "constructor.invocation.invalid",
                    constructor.toString(),
                    invocation.getEffectiveAnnotationInHierarchy(atypeFactory.READONLY),
                    constructor.getReturnType().getAnnotationInHierarchy(atypeFactory.READONLY));
            return;
        }
        if (invocation.hasAnnotation(atypeFactory.POLY_MUTABLE)) {
            return;
        }
        super.checkConstructorInvocation(invocation, constructor, newClassTree);
    }

    @Override
    public void processMethodTree(String className, MethodTree tree) {
        AnnotatedExecutableType executableType = atypeFactory.getAnnotatedType(tree);
        // Report an error if the constructor return type is @Readonly or @PolyMutable. Validity is
        // also checked in BaseTypeValidator.
        if (TreeUtils.isConstructor(tree)) {
            AnnotatedDeclaredType constructorReturnType =
                    (AnnotatedDeclaredType) executableType.getReturnType();
            if (constructorReturnType.hasAnnotation(atypeFactory.READONLY)
                    || constructorReturnType.hasAnnotation(atypeFactory.POLY_MUTABLE)) {
                checker.reportError(tree, "constructor.return.invalid", constructorReturnType);
            }
        }

        flexibleOverrideChecker(tree);

        super.processMethodTree(className, tree);
    }

    /**
     * Checks flexible overriding. Mutability permits an override when the overriding return type is
     * a subtype of the viewpoint-adapted overridden return type.
     *
     * @param node the method node
     */
    private void flexibleOverrideChecker(MethodTree node) {
        // TODO: This duplicates BaseTypeVisitor's override loop because mutability checker needs
        // the overridden
        // ExecutableElement for viewpoint adaptation.
        ExecutableElement methodElement = TreeUtils.elementFromDeclaration(node);
        AnnotatedDeclaredType enclosingType =
                (AnnotatedDeclaredType)
                        atypeFactory.getAnnotatedType(methodElement.getEnclosingElement());

        Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
                AnnotatedTypes.overriddenMethods(elements, atypeFactory, methodElement);
        for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair :
                overriddenMethods.entrySet()) {
            AnnotatedDeclaredType overriddenType = pair.getKey();
            AnnotatedExecutableType overriddenMethod =
                    AnnotatedTypes.asMemberOf(types, atypeFactory, enclosingType, pair.getValue());
            // Viewpoint adapt the overridden method to the current enclosing type.
            atypeFactory
                    .getViewpointAdapter()
                    .viewpointAdaptMethod(enclosingType, pair.getValue(), overriddenMethod);
            AnnotatedExecutableType overrider = atypeFactory.getAnnotatedType(node);
            if (!super.checkOverride(
                    node, overrider, enclosingType, overriddenMethod, overriddenType)) {
                // Stop at the first mismatch; this makes a difference only if
                // -Awarns is passed, in which case multiple warnings might be raised on
                // the same method, not adding any value. See Issue 373.
                break;
            }
        }
    }

    /**
     * Disables method overriding checks in BaseTypeVisitor. The override check is implemented in
     * {@link #flexibleOverrideChecker(MethodTree)} method.
     */
    @Override
    protected boolean checkOverride(
            MethodTree overriderTree,
            AnnotatedDeclaredType overridingType,
            AnnotatedExecutableType overridden,
            AnnotatedDeclaredType overriddenType) {
        return true;
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        ExpressionTree variable = node.getVariable();
        // Field-write checks use the receiver type, including flow refinement. The later assignment
        // subtype check still uses the standard left-hand-side type.
        checkAssignment(node, variable);
        return super.visitAssignment(node, p);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        ExpressionTree variable = node.getVariable();
        checkAssignment(node, variable);
        return super.visitCompoundAssignment(node, p);
    }

    @Override
    public Void visitUnary(UnaryTree node, Void p) {
        if (SIDE_EFFECTING_UNARY_OPERATORS.contains(node.getKind())) {
            ExpressionTree variable = node.getExpression();
            checkAssignment(node, variable);
        }
        return super.visitUnary(node, p);
    }

    /**
     * Checks whether a field or array assignment is allowed. Assignments in constructors and
     * initializer blocks are permitted.
     *
     * @param tree the assignment node
     * @param variable the variable in the assignment
     */
    private void checkAssignment(Tree tree, ExpressionTree variable) {
        AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(variable);
        MethodTree enclosingMethod = TreePathUtil.enclosingMethod(getCurrentPath());
        if (enclosingMethod != null) {
            List<? extends AnnotationMirror> receiverAnnotations =
                    getAllReceiverAnnotation(enclosingMethod);
            for (AnnotationMirror anno : receiverAnnotations) {
                if (AnnotationUtils.areSame(anno, atypeFactory.UNDER_INITALIZATION)) {
                    // Receiver under initialization permits assignment.
                    return;
                }
            }
            if (TreeUtils.isConstructor(enclosingMethod)) {
                // Constructors may initialize fields.
                return;
            }
        }
        if (TreePathUtil.isTopLevelAssignmentInInitializerBlock(getCurrentPath())) {
            // Initializer blocks may initialize fields.
            return;
        }
        // Implicit-this field assignments have no receiver tree, but they still have a receiver
        // type, so use receiverType to decide whether to enforce write permissions.
        if (receiverType != null && !allowWrite(receiverType, variable)) {
            reportFieldOrArrayWriteError(tree, variable, receiverType);
        }
    }

    /**
     * Returns the raw receiver annotations on a method.
     *
     * @param tree the method tree
     * @return the list of receiver annotations
     */
    private List<? extends AnnotationMirror> getAllReceiverAnnotation(MethodTree tree) {
        com.sun.tools.javac.code.Symbol meth =
                (com.sun.tools.javac.code.Symbol) TreeUtils.elementFromDeclaration(tree);
        return meth.getRawTypeAttributes();
    }

    /**
     * Returns whether the receiver type permits writing to the selected field or array.
     *
     * @param receiverType the receiver type
     * @param variable the variable in the assignment
     * @return true if the receiver type allows writing, false otherwise
     */
    private boolean allowWrite(AnnotatedTypeMirror receiverType, ExpressionTree variable) {
        if (receiverType.hasAnnotation(atypeFactory.MUTABLE)) {
            return true;
        } else return atypeFactory.isAssigningAssignableField(variable);
    }

    /**
     * Reports a field or array write error.
     *
     * @param tree the node to report the error
     * @param variable the variable in the assignment
     * @param receiverType the receiver type
     */
    private void reportFieldOrArrayWriteError(
            Tree tree, ExpressionTree variable, AnnotatedTypeMirror receiverType) {
        if (variable instanceof MemberSelectTree) {
            checker.reportError(
                    TreeUtils.getReceiverTree(variable), "illegal.field.write", receiverType);
        } else if (variable instanceof IdentifierTree) {
            checker.reportError(tree, "illegal.field.write", receiverType);
        } else if (variable instanceof ArrayAccessTree) {
            checker.reportError(
                    ((ArrayAccessTree) variable).getExpression(),
                    "illegal.array.write",
                    receiverType);
        } else {
            throw new BugInCF("Unknown assignment variable at: ", tree);
        }
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        VariableElement element = TreeUtils.elementFromDeclaration(node);
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(element);
        if (element.getKind() == ElementKind.FIELD) {
            if (type.hasAnnotation(atypeFactory.POLY_MUTABLE)) {
                checker.reportError(node, "field.polymutable.forbidden", element);
            }
        }
        return super.visitVariable(node, p);
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, Void p) {
        checkNewArrayCreation(tree);
        return super.visitNewArray(tree, p);
    }

    /**
     * Checks that a new array has an allowed mutability qualifier.
     *
     * @param tree the tree to check
     */
    private void checkNewArrayCreation(Tree tree) {
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
        if (!(type.hasAnnotation(atypeFactory.IMMUTABLE)
                || type.hasAnnotation(atypeFactory.MUTABLE)
                || type.hasAnnotation(atypeFactory.RECEIVER_DEPENDENT_MUTABLE)
                || type.hasAnnotation(atypeFactory.POLY_MUTABLE))) {
            checker.reportError(tree, "array.new.invalid", type);
        }
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        super.visitMethodInvocation(node, p);
        ParameterizedExecutableType mfuPair = atypeFactory.methodFromUse(node);
        AnnotatedExecutableType invokedMethod = mfuPair.executableType;
        ExecutableElement invokedMethodElement = invokedMethod.getElement();
        // Non-super invocations are already checked by BaseTypeVisitor. Super constructor calls
        // need this explicit invocability check.
        if (!ElementUtils.isStatic(invokedMethodElement)
                && TreeUtils.isSuperConstructorCall(node)) {
            checkMethodInvocability(invokedMethod, node);
        }
        return null;
    }

    @Override
    protected AnnotationMirrorSet getExceptionParameterLowerBoundAnnotations() {
        AnnotationMirrorSet result = new AnnotationMirrorSet();
        result.add(atypeFactory.getQualifierHierarchy().getBottomAnnotation(atypeFactory.BOTTOM));
        return result;
    }

    @Override
    protected AnnotationMirrorSet getThrowUpperBoundAnnotations() {
        AnnotationMirrorSet result = new AnnotationMirrorSet();
        result.add(atypeFactory.getQualifierHierarchy().getTopAnnotation(atypeFactory.READONLY));
        return result;
    }

    @Override
    public void processClassTree(ClassTree tree) {
        TypeElement typeElement = TreeUtils.elementFromDeclaration(tree);
        // Anonymous classes are validated through their creation expressions.
        if (TypesUtils.isAnonymous(TreeUtils.typeOf(tree))) {
            super.processClassTree(tree);
            return;
        }
        AnnotatedTypeMirror bound = atypeFactory.getAnnotatedType(typeElement);
        if (!atypeFactory.isValidClassBound(bound)) {
            validateType(tree, bound);
            return;
        }

        // In immutable or receiver-dependent-mutable classes, fields whose declared type bound is
        // mutable must have an explicit mutability qualifier to avoid implicit shallow
        // immutability.
        if (bound.hasAnnotation(atypeFactory.IMMUTABLE)
                || bound.hasAnnotation(atypeFactory.RECEIVER_DEPENDENT_MUTABLE)) {
            for (Tree member : tree.getMembers()) {
                if (member instanceof VariableTree) {
                    Element ele = TreeUtils.elementFromTree(member);
                    assert ele != null;
                    // fromElement does not apply defaults, so it exposes whether the source had an
                    // explicit mutability qualifier.
                    AnnotatedTypeMirror noDefaultMirror = atypeFactory.fromElement(ele);
                    TypeMirror ty = ele.asType();
                    if (ty.getKind() == TypeKind.TYPEVAR) {
                        ty = TypesUtils.upperBound(ty);
                    }
                    if (AnnotationUtils.containsSameByName(
                                    atypeFactory.getTypeDeclarationBounds(ty), atypeFactory.MUTABLE)
                            && !noDefaultMirror.hasAnnotationInHierarchy(atypeFactory.READONLY)) {
                        checker.reportError(member, "implicit.shallow.immutable");
                    }
                }
            }
        }
        super.processClassTree(tree);
    }

    /**
     * Checks that a this/super constructor call is valid in the mutability hierarchy. The invoked
     * constructor return type, adapted to the invoking constructor return type, must be a supertype
     * of the invoking constructor return type.
     *
     * @param superCall the super invocation, e.g., "super()"
     * @param errorKey the error key, e.g., "super.invocation.invalid"
     */
    @Override
    protected void checkThisOrSuperConstructorCall(
            MethodInvocationTree superCall, @CompilerMessageKey String errorKey) {
        MethodTree enclosingMethod = methodTree;
        AnnotatedTypeMirror superType = atypeFactory.getAnnotatedType(superCall);
        AnnotatedExecutableType constructorType = atypeFactory.getAnnotatedType(enclosingMethod);
        AnnotationMirror superTypeMirror =
                superType.getAnnotationInHierarchy(atypeFactory.READONLY);
        AnnotationMirror constructorTypeMirror =
                constructorType.getReturnType().getAnnotationInHierarchy(atypeFactory.READONLY);
        if (!atypeFactory
                .getQualifierHierarchy()
                .isSubtypeQualifiersOnly(constructorTypeMirror, superTypeMirror)) {
            checker.reportError(
                    superCall, errorKey, constructorTypeMirror, superCall, superTypeMirror);
        }
        super.checkThisOrSuperConstructorCall(superCall, errorKey);
    }

    @Override
    protected boolean isTypeCastSafe(AnnotatedTypeMirror castType, AnnotatedTypeMirror exprType) {
        QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();

        final TypeKind castTypeKind = castType.getKind();
        if (castTypeKind == TypeKind.DECLARED) {
            // Don't issue an error if the mutability annotations are equivalent to the qualifier
            // upper bound of the type.
            // The mutability checker only needs the mutability hierarchy for this cast-safety
            // check.
            AnnotatedDeclaredType castDeclared = (AnnotatedDeclaredType) castType;
            AnnotationMirror bound =
                    qualifierHierarchy.findAnnotationInHierarchy(
                            atypeFactory.getTypeDeclarationBounds(castDeclared.getUnderlyingType()),
                            atypeFactory.READONLY);
            assert bound != null;

            if (AnnotationUtils.areSame(
                    castDeclared.getAnnotationInHierarchy(atypeFactory.READONLY), bound)) {
                return true;
            }
        }

        return super.isTypeCastSafe(castType, exprType);
    }
}

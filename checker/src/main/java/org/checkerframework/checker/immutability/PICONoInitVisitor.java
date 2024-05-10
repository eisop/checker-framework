package org.checkerframework.checker.immutability;

import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.BOTTOM;
import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.IMMUTABLE;
import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.MUTABLE;
import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.POLY_MUTABLE;
import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.READONLY;
import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;
import static org.checkerframework.javacutil.TreePathUtil.isTopLevelAssignmentInInitializerBlock;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.basetype.TypeValidator;
import org.checkerframework.framework.type.AnnotatedTypeFactory.ParameterizedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
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

import java.util.HashMap;
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

public class PICONoInitVisitor extends BaseTypeVisitor<PICONoInitAnnotatedTypeFactory> {

    private final boolean shouldOutputFbcError;
    final Map<String, Integer> fbcViolatedMethods;

    public PICONoInitVisitor(BaseTypeChecker checker) {
        super(checker);
        shouldOutputFbcError = checker.hasOption("printFbcErrors");
        fbcViolatedMethods = shouldOutputFbcError ? new HashMap<>() : null;
    }

    @Override
    protected TypeValidator createTypeValidator() {
        return new PICOValidator(checker, this, atypeFactory);
    }

    @Override
    protected void checkConstructorResult(
            AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {}

    // This method is for validating usage of mutability qualifier is conformable to element
    // declaration,
    // Ugly thing here is that declarationType is not the result of calling the other method -
    // PICOTypeUtil#getBoundTypeOfTypeDeclaration. Instead it's the result of calling
    // ATF#getAnnotatedType(Element).
    // Why it works is that PICOTypeUtil#getBoundTypeOfTypeDeclaration and
    // ATF#getAnnotatedType(Element) has
    // the same effect most of the time except on java.lang.Object. We need to be careful when
    // modifying
    // PICOTypeUtil#getBoundTypeOfTypeDeclaration so that it has the same behaviour as
    // ATF#getAnnotatedType(Element)
    // (at least for types other than java.lang.Object)
    @Override
    public boolean isValidUse(
            AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {

        // FIXME workaround for poly anno, remove after fix substitutable poly and add poly vp rules
        if (useType.hasAnnotation(POLY_MUTABLE)) {
            return true;
        }

        AnnotationMirror declared = declarationType.getAnnotationInHierarchy(READONLY);
        AnnotationMirror used = useType.getAnnotationInHierarchy(READONLY);

        return isAdaptedSubtype(used, declared);
    }

    @Override
    public boolean isValidUse(AnnotatedTypeMirror.AnnotatedArrayType type, Tree tree) {
        // You don't need adapted subtype if the decl bound is guaranteed to be RDM.
        // That simply means that any use is valid except bottom.
        AnnotationMirror used = type.getAnnotationInHierarchy(READONLY);
        return !AnnotationUtils.areSame(used, BOTTOM);
    }

    private boolean isAdaptedSubtype(AnnotationMirror lhs, AnnotationMirror rhs) {
        ExtendedViewpointAdapter vpa =
                ((ViewpointAdapterGettable) atypeFactory).getViewpointAdapter();
        AnnotationMirror adapted = vpa.rawCombineAnnotationWithAnnotation(lhs, rhs);
        return atypeFactory.getQualifierHierarchy().isSubtypeQualifiersOnly(adapted, lhs);
    }

    @Override
    protected boolean commonAssignmentCheck(
            Tree varTree, ExpressionTree valueExp, String errorKey, Object... extraArgs) {
        AnnotatedTypeMirror var = atypeFactory.getAnnotatedTypeLhs(varTree);
        assert var != null : "no variable found for tree: " + varTree;

        if (!validateType(varTree, var)) {
            return false;
        }

        if (varTree instanceof VariableTree) {
            VariableElement element = TreeUtils.elementFromDeclaration((VariableTree) varTree);
            if (element.getKind() == ElementKind.FIELD && !ElementUtils.isStatic(element)) {
                AnnotatedDeclaredType bound =
                        PICOTypeUtil.getBoundTypeOfEnclosingTypeDeclaration(varTree, atypeFactory);
                // var is singleton, so shouldn't modify var directly. Otherwise, the variable
                // tree's type will be
                // altered permanently, and other clients who access this type will see the change,
                // too.
                AnnotatedTypeMirror varAdapted = var.shallowCopy(true);
                // Viewpoint adapt varAdapted to the bound.
                // PICOInferenceAnnotatedTypeFactory#viewpointAdaptMember()
                // mutates varAdapted, so after the below method is called, varAdapted is the result
                // adapted to bound
                atypeFactory.getViewpointAdapter().viewpointAdaptMember(bound, element, varAdapted);
                // Pass varAdapted here as lhs type.
                // Caution: cannot pass var directly. Modifying type in PICOInferenceTreeAnnotator#
                // visitVariable() will cause wrong type to be gotton here, as on inference side,
                // atm is uniquely determined by each element.
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
        // TODO Is the copied code really needed?
        /*Copied Code Start*/
        AnnotatedDeclaredType returnType = (AnnotatedDeclaredType) constructor.getReturnType();
        // When an interface is used as the identifier in an anonymous class (e.g. new Comparable()
        // {})
        // the constructor method will be Object.init() {} which has an Object return type
        // When TypeHierarchy attempts to convert it to the supertype (e.g. Comparable) it will
        // return
        // null from asSuper and return false for the check.  Instead, copy the primary annotations
        // to the declared type and then do a subtyping check
        if (invocation.getUnderlyingType().asElement().getKind().isInterface()
                && TypesUtils.isObject(returnType.getUnderlyingType())) {
            final AnnotatedDeclaredType retAsDt = invocation.deepCopy();
            retAsDt.replaceAnnotations(returnType.getAnnotations());
            returnType = retAsDt;
        } else if (newClassTree.getClassBody() != null) {
            // An anonymous class invokes the constructor of its super class, so the underlying
            // types of invocation and returnType are not the same.  Call asSuper so they are the
            // same and the is subtype tests below work correctly
            invocation = AnnotatedTypes.asSuper(atypeFactory, invocation, returnType);
        }
        /*Copied Code End*/

        // The immutability return qualifier of the constructor (returnType) must be supertype of
        // the constructor invocation immutability qualifier(invocation).
        if (!atypeFactory
                .getQualifierHierarchy()
                .isSubtypeQualifiersOnly(
                        invocation.getAnnotationInHierarchy(READONLY),
                        returnType.getAnnotationInHierarchy(READONLY))) {
            checker.reportError(
                    newClassTree, "constructor.invocation.invalid", invocation, returnType);
        }
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        AnnotatedExecutableType executableType = atypeFactory.getAnnotatedType(node);
        AnnotatedDeclaredType bound =
                PICOTypeUtil.getBoundTypeOfEnclosingTypeDeclaration(node, atypeFactory);

        if (TreeUtils.isConstructor(node)) {
            AnnotatedDeclaredType constructorReturnType =
                    (AnnotatedDeclaredType) executableType.getReturnType();
            if (constructorReturnType.hasAnnotation(READONLY)
                    || constructorReturnType.hasAnnotation(POLY_MUTABLE)) {
                checker.reportError(node, "constructor.return.invalid", constructorReturnType);
                return super.visitMethod(node, p);
            }
            // if no explicit anno it must inherit from class decl so identical
            // => if not the same must not inherited from class decl
            // => no need to check the source of the anno

        } else {
            AnnotatedDeclaredType declareReceiverType = executableType.getReceiverType();
            if (declareReceiverType != null) {
                if (bound != null
                        && !bound.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)
                        && !atypeFactory
                                .getQualifierHierarchy()
                                .isSubtypeQualifiersOnly(
                                        declareReceiverType.getAnnotationInHierarchy(READONLY),
                                        bound.getAnnotationInHierarchy(READONLY))
                        // Below three are allowed on declared receiver types of instance methods in
                        // either @Mutable class or @Immutable class
                        && !declareReceiverType.hasAnnotation(READONLY)
                        && !declareReceiverType.hasAnnotation(POLY_MUTABLE)) {
                    checker.reportError(node, "method.receiver.incompatible", declareReceiverType);
                }
            }
        }

        flexibleOverrideChecker(node);

        // ObjectIdentityMethod check
        if (PICOTypeUtil.isObjectIdentityMethod(node, atypeFactory)) {
            ObjectIdentityMethodEnforcer.check(
                    atypeFactory.getPath(node.getBody()), atypeFactory, checker);
        }
        return super.visitMethod(node, p);
    }

    private void flexibleOverrideChecker(MethodTree node) {
        // Method overriding checks
        // TODO Copied from super, hence has lots of duplicate code with super. We need to
        // change the signature of checkOverride() method to also pass ExecutableElement for
        // viewpoint adaptation.
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
            // Viewpoint adapt super method executable type to current class bound(is this always
            // class bound?)
            // to allow flexible overriding
            atypeFactory
                    .getViewpointAdapter()
                    .viewpointAdaptMethod(enclosingType, pair.getValue(), overriddenMethod);
            AnnotatedExecutableType overrider = atypeFactory.getAnnotatedType(node);
            if (!checkOverride(node, overrider, enclosingType, overriddenMethod, overriddenType)) {
                // Stop at the first mismatch; this makes a difference only if
                // -Awarns is passed, in which case multiple warnings might be raised on
                // the same method, not adding any value. See Issue 373.
                break;
            }
        }
    }

    // Disables method overriding checks in BaseTypeVisitor
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
        // TODO Question Here, receiver type uses flow refinement. But in commonAssignmentCheck to
        // compute lhs type
        // , it doesn't. This causes inconsistencies when enforcing immutability and doing subtype
        // check. I overrode
        // getAnnotatedTypeLhs() to also use flow sensitive refinement, but came across with
        // "private access" problem
        // on field "computingAnnotatedTypeMirrorOfLHS"
        checkMutation(node, variable);
        return super.visitAssignment(node, p);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        ExpressionTree variable = node.getVariable();
        checkMutation(node, variable);
        return super.visitCompoundAssignment(node, p);
    }

    @Override
    public Void visitUnary(UnaryTree node, Void p) {
        if (PICOTypeUtil.isSideEffectingUnaryTree(node)) {
            ExpressionTree variable = node.getExpression();
            checkMutation(node, variable);
        }
        return super.visitUnary(node, p);
    }

    private void checkMutation(Tree node, ExpressionTree variable) {
        AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(variable);
        MethodTree enclosingMethod = TreePathUtil.enclosingMethod(getCurrentPath());
        if (enclosingMethod != null && TreeUtils.isConstructor(enclosingMethod)) {
            // If the enclosing method is constructor, we don't need to check the receiver type
            return;
        }
        if (isTopLevelAssignmentInInitializerBlock(getCurrentPath())) {
            // If the assignment is in initializer block, we don't need to check the receiver type
            return;
        }
        // Cannot use receiverTree = TreeUtils.getReceiverTree(variable) to determine if it's
        // field assignment or not. Because for field assignment with implicit "this", receiverTree
        // is null but receiverType is non-null. We still need to check this case.
        if (receiverType != null && !allowWrite(receiverType, variable)) {
            reportFieldOrArrayWriteError(node, variable, receiverType);
        }
    }

    private boolean allowWrite(AnnotatedTypeMirror receiverType, ExpressionTree variable) {
        // One pico side, if only receiver is mutable, we allow assigning/reassigning. Because if
        // the field
        // is declared as final, Java compiler will catch that, and we couldn't have reached this
        // point
        if (PICOTypeUtil.isAssigningAssignableField(variable, atypeFactory)) {
            return isAllowedAssignableField(receiverType, variable);
        } else if (receiverType.hasAnnotation(MUTABLE)) {
            // If the receiver is mutable, we allow assigning/reassigning
            return true;
            //        } else if (TreeUtils.elementFromUse(variable)) {
            //            // If the field is not initialized, we allow assigning/reassigning
            //            return true;
        }

        return false;
    }

    private boolean isAllowedAssignableField(
            AnnotatedTypeMirror receiverType, ExpressionTree node) {
        Element fieldElement = TreeUtils.elementFromUse(node);
        Set<AnnotationMirror> bounds =
                atypeFactory.getTypeDeclarationBounds(TreeUtils.typeOf(node));
        AnnotatedTypeMirror fieldType = atypeFactory.getAnnotatedType(fieldElement);
        if (fieldElement == null) return false;
        // Forbid the case that might break type soundness. See ForbidAssignmentCase.java:21
        // the second and third predicates ensure that the field is actually rdm (since sometimes we
        // replace implicitly mutable with rdm to protect transitive immutability).
        return !(receiverType.hasAnnotation(READONLY)
                && AnnotationUtils.containsSameByName(bounds, RECEIVER_DEPENDANT_MUTABLE)
                && fieldType.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE));
    }

    private void reportFieldOrArrayWriteError(
            Tree node, ExpressionTree variable, AnnotatedTypeMirror receiverType) {
        if (variable.getKind() == Kind.MEMBER_SELECT) {
            checker.reportError(
                    TreeUtils.getReceiverTree(variable), "illegal.field.write", receiverType);
        } else if (variable.getKind() == Kind.IDENTIFIER) {
            checker.reportError(node, "illegal.field.write", receiverType);
        } else if (variable.getKind() == Kind.ARRAY_ACCESS) {
            checker.reportError(
                    ((ArrayAccessTree) variable).getExpression(),
                    "illegal.array.write",
                    receiverType);
        } else {
            throw new BugInCF("Unknown assignment variable at: ", node);
        }
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        VariableElement element = TreeUtils.elementFromDeclaration(node);
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(element);
        if (element.getKind() == ElementKind.FIELD) {
            if (type.hasAnnotation(POLY_MUTABLE)) {
                checker.reportError(node, "field.polymutable.forbidden", element);
            }
        }
        // When to check:
        // bound == Immutable, OR
        // not FIELD, OR
        // top anno not RDM
        // TODO use base cf check methods
        AnnotationMirror declAnno =
                atypeFactory.getTypeDeclarationBoundForMutability(type.getUnderlyingType());
        if ((declAnno != null && AnnotationUtils.areSameByName(declAnno, IMMUTABLE))
                || element.getKind() != ElementKind.FIELD
                || !type.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
            checkAndReportInvalidAnnotationOnUse(type, node);
        }
        return super.visitVariable(node, p);
    }

    private void checkAndReportInvalidAnnotationOnUse(AnnotatedTypeMirror type, Tree node) {
        AnnotationMirror useAnno = type.getAnnotationInHierarchy(READONLY);
        // FIXME rm after poly vp
        if (useAnno != null && AnnotationUtils.areSame(useAnno, POLY_MUTABLE)) {
            return;
        }
        if (useAnno != null
                && !PICOTypeUtil.isImplicitlyImmutableType(type)
                && type.getKind()
                        != TypeKind.ARRAY) { // TODO: annotate the use instead of using this
            AnnotationMirror defaultAnno = MUTABLE;
            for (AnnotationMirror anno :
                    atypeFactory.getTypeDeclarationBounds(type.getUnderlyingType())) {
                if (atypeFactory.getQualifierHierarchy().isSubtypeQualifiersOnly(anno, READONLY)
                        && !AnnotationUtils.areSame(anno, READONLY)) {
                    defaultAnno = anno;
                }
            }
            if (!isAdaptedSubtype(useAnno, defaultAnno)) {
                checker.reportError(node, "type.invalid.annotations.on.use", defaultAnno, useAnno);
            }
        }
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        checkNewInstanceCreation(node);
        return super.visitNewClass(node, p);
    }

    @Override
    public Void visitNewArray(NewArrayTree node, Void p) {
        checkNewInstanceCreation(node);
        return super.visitNewArray(node, p);
    }

    private void checkNewInstanceCreation(Tree node) {
        // Ensure only @Mutable/@Immutable/@ReceiverDependantMutable/@PolyMutable are used on new
        // instance creation
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node);
        if (!(type.hasAnnotation(IMMUTABLE)
                || type.hasAnnotation(MUTABLE)
                || type.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)
                || type.hasAnnotation(POLY_MUTABLE))) {
            checker.reportError(node, "pico.new.invalid", type);
        }
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        super.visitMethodInvocation(node, p);
        ParameterizedExecutableType mfuPair = atypeFactory.methodFromUse(node);
        AnnotatedExecutableType invokedMethod = mfuPair.executableType;
        ExecutableElement invokedMethodElement = invokedMethod.getElement();
        // Only check invocability if it's super call, as non-super call is already checked
        // by super implementation(of course in both cases, invocability is not checked when
        // invoking static methods)
        if (!ElementUtils.isStatic(invokedMethodElement)
                && TreeUtils.isSuperConstructorCall(node)) {
            checkMethodInvocability(invokedMethod, node);
        }
        return null;
    }

    @Override
    protected AnnotationMirrorSet getExceptionParameterLowerBoundAnnotations() {
        AnnotationMirrorSet result = new AnnotationMirrorSet();
        result.add(atypeFactory.getQualifierHierarchy().getBottomAnnotation(BOTTOM));
        return result;
    }

    @Override
    protected AnnotationMirrorSet getThrowUpperBoundAnnotations() {
        AnnotationMirrorSet result = new AnnotationMirrorSet();
        result.add(atypeFactory.getQualifierHierarchy().getTopAnnotation(READONLY));
        return result;
    }

    @Override
    public void processClassTree(ClassTree node) {
        TypeElement typeElement = TreeUtils.elementFromDeclaration(node);
        // TODO Don't process anonymous class. I'm not even sure if whether
        // processClassTree(ClassTree) is called on anonymous class tree
        if (typeElement.toString().contains("anonymous")) {
            super.processClassTree(node);
            return;
        }

        AnnotatedDeclaredType bound =
                PICOTypeUtil.getBoundTypeOfTypeDeclaration(typeElement, atypeFactory);
        // Has to be either @Mutable, @ReceiverDependantMutable or @Immutable, nothing else
        if (!bound.hasAnnotation(MUTABLE)
                && !bound.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)
                && !bound.hasAnnotation(IMMUTABLE)) {
            checker.reportError(node, "class.bound.invalid", bound);
            return; // Doesn't process the class tree anymore
        }

        // Issue warnings on implicit shallow immutable:
        // Condition:
        // * Class decl == Immutable or RDM     * move rdm default error here. see 3.6.3 last part.
        // liansun
        // * Member is field
        // * Member's declared bound == Mutable
        // * Member's use anno == null
        if (bound.hasAnnotation(IMMUTABLE) || bound.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
            for (Tree member : node.getMembers()) {
                if (member.getKind() == Kind.VARIABLE) {
                    Element ele = TreeUtils.elementFromTree(member);
                    assert ele != null;
                    // fromElement will not apply defaults, if no explicit anno exists in code,
                    // mirror have no anno
                    AnnotatedTypeMirror noDefaultMirror = atypeFactory.fromElement(ele);
                    TypeMirror ty = ele.asType();
                    if (ty.getKind() == TypeKind.TYPEVAR) {
                        ty = TypesUtils.upperBound(ty);
                    }
                    if (AnnotationUtils.containsSameByName(
                                    atypeFactory.getTypeDeclarationBounds(ty), MUTABLE)
                            && !noDefaultMirror.hasAnnotationInHierarchy(READONLY)) {
                        checker.reportError(member, "implicit.shallow.immutable");
                    }
                }
            }
        }
        super.processClassTree(node);
    }

    /**
     * The invoked constructor’s return type adapted to the invoking constructor’s return type must
     * be a supertype of the invoking constructor’s return type. Since InitializationChecker does
     * not apply any type rules at here, only READONLY hierarchy is checked.
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
        AnnotationMirror superTypeMirror = superType.getAnnotationInHierarchy(READONLY);
        AnnotationMirror constructorTypeMirror =
                constructorType.getReturnType().getAnnotationInHierarchy(READONLY);
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
            // upper bound
            // of the type.
            // BaseTypeVisitor#isTypeCastSafe is not used, to be consistent with inference which
            // only have mutability qualifiers
            // if inference is supporting FBC in the future, this overridden method can be removed.
            AnnotatedDeclaredType castDeclared = (AnnotatedDeclaredType) castType;

            AnnotationMirror bound =
                    qualifierHierarchy.findAnnotationInHierarchy(
                            atypeFactory.getTypeDeclarationBounds(castDeclared.getUnderlyingType()),
                            READONLY);
            assert bound != null;

            if (AnnotationUtils.areSame(castDeclared.getAnnotationInHierarchy(READONLY), bound)) {
                return true;
            }
        }

        return super.isTypeCastSafe(castType, exprType);
    }

    @Override
    protected boolean commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            String errorKey,
            Object... extraArgs) {
        // TODO: WORKAROUND: anonymous class handling
        if (TypesUtils.isAnonymous(valueType.getUnderlyingType())) {
            AnnotatedTypeMirror newValueType = varType.deepCopy();
            newValueType.replaceAnnotation(valueType.getAnnotationInHierarchy(READONLY));
            valueType = newValueType;
        }
        return super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);
    }
}

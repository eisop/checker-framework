package org.checkerframework.checker.immutability;

import static org.checkerframework.checker.immutability.PICOAnnotationMirrorHolder.*;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import org.checkerframework.checker.immutability.qual.Bottom;
import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.PolyMutable;
import org.checkerframework.checker.immutability.qual.Readonly;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;
import org.checkerframework.checker.initialization.InitializationFieldAccessTreeAnnotator;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.*;
import org.checkerframework.javacutil.*;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * AnnotatedTypeFactory for PICO. In addition to getting atms, it also propagates and applies
 * mutability qualifiers correctly depending on AST locations(e.g. fields, binary trees) or
 * methods(toString(), hashCode(), clone(), equals(Object o)) using TreeAnnotators and
 * TypeAnnotators. It also applies implicits to method receiver that is not so by default in super
 * implementation.
 */
// TODO Use @Immutable for classes that extends those predefined immutable classess like String or
// Number
// and explicitly annotated classes with @Immutable on its declaration
public class PICONoInitAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<
                PICONoInitValue, PICONoInitStore, PICONoInitTransfer, PICONoInitAnalysis>
        implements ViewpointAdapterGettable {

    public PICONoInitAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
        // PICO aliasing is not implemented correctly
        // remove for now
        //        addAliasedAnnotation(org.jmlspecs.annotation.Readonly.class, READONLY);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(
                        Readonly.class,
                        Mutable.class,
                        PolyMutable.class,
                        ReceiverDependantMutable.class,
                        Immutable.class,
                        Bottom.class));
    }

    @Override
    protected ViewpointAdapter createViewpointAdapter() {
        return new PICOViewpointAdapter(this);
    }

    /** Annotators are executed by the added order. Same for Type Annotator */
    @Override
    protected TreeAnnotator createTreeAnnotator() {
        List<TreeAnnotator> annotators = new ArrayList<>(5);
        annotators.add(new InitializationFieldAccessTreeAnnotator(this));
        annotators.add(new PICOPropagationTreeAnnotator(this));
        annotators.add(new LiteralTreeAnnotator(this));
        annotators.add(new PICOSuperClauseAnnotator(this));
        annotators.add(new PICOTreeAnnotator(this));
        return new ListTreeAnnotator(annotators);
    }

    // TODO Refactor super class to remove this duplicate code
    @Override
    protected TypeAnnotator createTypeAnnotator() {
        /*Copied code start*/
        List<TypeAnnotator> typeAnnotators = new ArrayList<>();
        RelevantJavaTypes relevantJavaTypes =
                checker.getClass().getAnnotation(RelevantJavaTypes.class);
        if (relevantJavaTypes != null) {
            //            Class<?>[] classes = relevantJavaTypes.value();
            // Must be first in order to annotated all irrelevant types that are not explicilty
            // annotated.
            typeAnnotators.add(new IrrelevantTypeAnnotator(this));
        }
        typeAnnotators.add(new PropagationTypeAnnotator(this));
        /*Copied code ends*/
        // Adding order is important here. Because internally type annotators are using
        // addMissingAnnotations() method, so if one annotator already applied the annotations, the
        // others won't apply twice
        // at the same location
        typeAnnotators.add(new PICOTypeAnnotator(this));
        typeAnnotators.add(new PICODefaultForTypeAnnotator(this));
        typeAnnotators.add(new PICOEnumDefaultAnnotator(this));
        return new ListTypeAnnotator(typeAnnotators);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy() {
        return new NoElementQualifierHierarchy(getSupportedTypeQualifiers(), elements, this);
    }

    @Override
    public ParameterizedExecutableType constructorFromUse(NewClassTree tree) {
        boolean hasExplicitAnnos = false;
        if (!getExplicitNewClassAnnos(tree).isEmpty()) {
            hasExplicitAnnos = true;
        }
        ParameterizedExecutableType mType = super.constructorFromUse(tree);
        AnnotatedExecutableType method = mType.executableType;
        if (!hasExplicitAnnos && method.getReturnType().hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
            method.getReturnType().replaceAnnotation(MUTABLE);
        }
        return mType;
    }

    //    /** Just to transfer the method from super class to package */
    //    @Override
    //    protected boolean isInitializationAnnotation(AnnotationMirror anno) {
    //        return super.isInitializationAnnotation(anno);
    //    }
    //
    //    @Override
    //    public AnnotationMirror getFieldInvariantAnnotation() {
    //        return IMMUTABLE;
    //    }
    //
    //        /** This affects what fields pico warns not initialized in constructors */
    //        @Override
    //        protected boolean hasFieldInvariantAnnotation(
    //                AnnotatedTypeMirror type, VariableElement fieldElement) {
    //            // This affects which fields should be guaranteed to be initialized:
    //            // Fields of any immutability should be initialized.
    //            return !PICOTypeUtil.isAssignableField(fieldElement, this);
    //        }

    /** Forbid applying top annotations to type variables if they are used on local variables */
    @Override
    public boolean getShouldDefaultTypeVarLocals() {
        return false;
    }

    /**
     * This covers the case when static fields are used and constructor is accessed as an
     * element(regarding applying @Immutable on type declaration to constructor return type).
     */
    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        PICOTypeUtil.addDefaultForField(this, type, elt);
        PICOTypeUtil.defaultConstructorReturnToClassBound(this, elt, type);
        //        PICOTypeUtil.applyImmutableToEnumAndEnumConstant(type);
        super.addComputedTypeAnnotations(elt, type);
    }

    /** This method gets lhs WITH flow sensitive refinement */
    // TODO Should refactor super class to avoid too much duplicate code.
    // This method is pretty hacky right now.
    //    @Override
    //    public AnnotatedTypeMirror getAnnotatedTypeLhs(Tree lhsTree) {
    //        boolean oldComputingAnnotatedTypeMirrorOfLHS = computingAnnotatedTypeMirrorOfLHS;
    //        computingAnnotatedTypeMirrorOfLHS = true;
    //
    //        AnnotatedTypeMirror result;
    //        boolean oldShouldCache = shouldCache;
    //        // Don't cache the result because getAnnotatedType(lhsTree) could
    //        // be called from elsewhere and would expect flow-sensitive type refinements.
    //        shouldCache = false;
    //        switch (lhsTree.getKind()) {
    //            case VARIABLE:
    //            case IDENTIFIER:
    //            case MEMBER_SELECT:
    //            case ARRAY_ACCESS:
    //                result = getAnnotatedType(lhsTree);
    //                break;
    //            default:
    //                if (TreeUtils.isTypeTree(lhsTree)) {
    //                    // lhsTree is a type tree at the pseudo assignment of a returned
    // expression to declared return type.
    //                    result = getAnnotatedType(lhsTree);
    //                } else {
    //                    throw new BugInCF(
    //                            "GenericAnnotatedTypeFactory: Unexpected tree passed to
    // getAnnotatedTypeLhs. "
    //                                    + "lhsTree: "
    //                                    + lhsTree
    //                                    + " Tree.Kind: "
    //                                    + lhsTree.getKind());
    //                }
    //        }
    //        shouldCache = oldShouldCache;
    //
    //        computingAnnotatedTypeMirrorOfLHS = oldComputingAnnotatedTypeMirrorOfLHS;
    //        return result;
    //    }

    //    /**Handles invoking static methods with polymutable on its declaration*/
    //    @Override
    //    public ParameterizedExecutableType methodFromUse(ExpressionTree tree, ExecutableElement
    // methodElt, AnnotatedTypeMirror receiverType) {
    //        ParameterizedExecutableType pair = super.methodFromUse(tree, methodElt, receiverType);
    //        // We want to replace polymutable with substitutablepolymutable when we invoke static
    // methods
    //        if (ElementUtils.isStatic(methodElt)) {
    //            AnnotatedExecutableType methodType = pair.executableType;
    //            AnnotatedTypeMirror returnType = methodType.getReturnType();
    //            if (returnType.hasAnnotation(POLY_MUTABLE)) {
    //                // Only substitute polymutable but not other qualifiers! Missing the if
    // statement
    //                // caused bugs before!
    //                returnType.replaceAnnotation(SUBSTITUTABLE_POLY_MUTABLE);
    //            }
    //            List<AnnotatedTypeMirror> parameterTypes = methodType.getParameterTypes();
    //            for (AnnotatedTypeMirror p : parameterTypes) {
    //                if (returnType.hasAnnotation(POLY_MUTABLE)) {
    //                    p.replaceAnnotation(SUBSTITUTABLE_POLY_MUTABLE);
    //                }
    //            }
    //        }
    //        return pair;
    //    }

    //    protected class PICOQualifierHierarchy extends MostlyNoElementQualifierHierarchy {
    //        protected PICOQualifierHierarchy(Collection<Class<? extends Annotation>>
    // qualifierClasses, Elements elements, GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
    //            super(qualifierClasses, elements, atypeFactory);
    //        }
    //
    //        //        public PICOQualifierHierarchy(MultiGraphFactory f, Object[] arg) {
    //        //            super(f, arg);
    //        //        }
    //
    //        //        @Override
    //        //        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror
    // superAnno) {
    //        //            if (isInitializationAnnotation(subAnno) ||
    //        // isInitializationAnnotation(superAnno)) {
    //        //                return this.isSubtypeInitialization(subAnno, superAnno);
    //        //            }
    //        //            return super.isSubtype(subAnno, superAnno);
    //        //        }
    //
    //        @Override
    //        protected boolean isSubtypeWithElements(
    //                AnnotationMirror subAnno,
    //                QualifierKind subKind,
    //                AnnotationMirror superAnno,
    //                QualifierKind superKind) {
    //            return super.isSubtypeQualifiersOnly(subAnno, superAnno);
    //        }
    //
    //        //        @Override
    //        //        public AnnotationMirror leastUpperBound(AnnotationMirror a1,
    // AnnotationMirror a2)
    //        // {
    //        //            if (isInitializationAnnotation(a1) || isInitializationAnnotation(a2)) {
    //        //                return this.leastUpperBoundInitialization(a1, a2);
    //        //            }
    //        //            return super.leastUpperBound(a1, a2);
    //        //        }
    //
    //        @Override
    //        protected AnnotationMirror leastUpperBoundWithElements(
    //                AnnotationMirror a1,
    //                QualifierKind q1,
    //                AnnotationMirror a2,
    //                QualifierKind q2,
    //                QualifierKind lub) {
    //            return super.leastUpperBoundQualifiersOnly(a1, a2);
    //        }
    //
    //        @Override
    //        protected AnnotationMirror greatestLowerBoundWithElements(
    //                AnnotationMirror annotationMirror,
    //                QualifierKind qualifierKind,
    //                AnnotationMirror annotationMirror1,
    //                QualifierKind qualifierKind1,
    //                QualifierKind qualifierKind2) {
    //            return super.greatestLowerBoundQualifiersOnly(annotationMirror,
    // annotationMirror1);
    //        }
    //    }

    /** Tree Annotators */
    public static class PICOPropagationTreeAnnotator extends PropagationTreeAnnotator {
        public PICOPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        //
        //        // TODO This is very ugly. Why is array component type from lhs propagates to
        // rhs?!
        @Override
        public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror componentType =
                    ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType();
            boolean noExplicitATM = false;
            if (!componentType.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
                noExplicitATM = true;
            }
            super.visitNewArray(tree, type);
            // if new explicit anno before, but RDM after, the RDM must come from the type
            // declaration bound
            // however, for type has declaration bound as RDM, its default use is mutable.
            if (noExplicitATM && componentType.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
                componentType.replaceAnnotation(MUTABLE);
            }
            return null;
        }

        //
        //        /**Add immutable to the result type of a binary operation if the result type is
        // implicitly immutable*/
        //        @Override
        //        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
        //            applyImmutableIfImplicitlyImmutable(type);// Usually there isn't existing
        // annotation on binary trees, but to be safe, run it first
        //            super.visitBinary(node, type);
        //            // NullnessPropagationTreeAnnotator says result type of binary tree is always
        // @Initialized. So replace it
        //            // with COMMITED here.
        //            applyCommitedIfSupported(atypeFactory, type);
        //            return null;
        //        }

        //        @Override
        //        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
        //            super.visitUnary(node, type);
        //            // Same reason as above
        //            applyCommitedIfSupported(atypeFactory, type);
        //            return null;
        //        }
        //
        @Override
        public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
            boolean hasExplicitAnnos = false;
            if (!type.getAnnotations().isEmpty()) {
                hasExplicitAnnos = true;
            }
            super.visitTypeCast(node, type);
            if (!hasExplicitAnnos && type.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
                type.replaceAnnotation(MUTABLE);
            }
            return null;
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            return null;
        }

        //        private void applyCommitedIfSupported(AnnotatedTypeFactory annotatedTypeFactory,
        // AnnotatedTypeMirror type) {
        //            if (annotatedTypeFactory.isSupportedQualifier(COMMITED)) {
        //                type.replaceAnnotation(COMMITED);
        //            }
        //        }
    }

    @Override
    public ExtendedViewpointAdapter getViewpointAdapter() {
        return (ExtendedViewpointAdapter) viewpointAdapter;
    }

    @Override
    protected AnnotationMirrorSet getDefaultTypeDeclarationBounds() {
        AnnotationMirrorSet frameworkDefault =
                new AnnotationMirrorSet(super.getDefaultTypeDeclarationBounds());
        return replaceAnnotationInHierarchy(frameworkDefault, MUTABLE);
    }

    @Override
    public AnnotationMirrorSet getTypeDeclarationBounds(TypeMirror type) {
        AnnotationMirror mut = getTypeDeclarationBoundForMutability(type);
        AnnotationMirrorSet frameworkDefault = super.getTypeDeclarationBounds(type);
        if (mut != null) {
            frameworkDefault = replaceAnnotationInHierarchy(frameworkDefault, mut);
        }
        return frameworkDefault;
    }

    private AnnotationMirrorSet replaceAnnotationInHierarchy(
            AnnotationMirrorSet set, AnnotationMirror mirror) {
        AnnotationMirrorSet result = new AnnotationMirrorSet(set);
        AnnotationMirror removeThis =
                getQualifierHierarchy().findAnnotationInSameHierarchy(set, mirror);
        result.remove(removeThis);
        result.add(mirror);
        return result;
    }

    public AnnotationMirror getTypeDeclarationBoundForMutability(TypeMirror type) {
        // copied from inference real type factory with minor modification
        // TODO too awkward. maybe overload isImplicitlyImmutableType
        if (PICOTypeUtil.isImplicitlyImmutableType(toAnnotatedType(type, false))) {
            return IMMUTABLE;
        }
        if (type.getKind() == TypeKind.ARRAY) {
            return RECEIVER_DEPENDANT_MUTABLE; // if decided to use vpa for array, return RDM.
        }

        // IMMUTABLE for enum w/o decl anno
        if (type instanceof DeclaredType) {
            Element ele = ((DeclaredType) type).asElement();
            if (ele.getKind() == ElementKind.ENUM) {
                if (!AnnotationUtils.containsSameByName(getDeclAnnotations(ele), MUTABLE)
                        && !AnnotationUtils.containsSameByName(
                                getDeclAnnotations(ele),
                                RECEIVER_DEPENDANT_MUTABLE)) { // no decl anno
                    return IMMUTABLE;
                }
            }
        }
        return null;
    }

    @Override
    public AnnotatedTypeMirror getTypeOfExtendsImplements(Tree clause) {
        // this is still needed with PICOSuperClauseAnnotator.
        // maybe just use getAnnotatedType
        // add default anno from class main qual, if no qual present
        AnnotatedTypeMirror fromTypeTree = super.getTypeOfExtendsImplements(clause);
        if (fromTypeTree.hasAnnotation(RECEIVER_DEPENDANT_MUTABLE)) {
            AnnotatedTypeMirror enclosing =
                    getAnnotatedType(TreePathUtil.enclosingClass(getPath(clause)));
            AnnotationMirror mainBound = enclosing.getAnnotationInHierarchy(READONLY);
            fromTypeTree.replaceAnnotation(mainBound);
        }
        return fromTypeTree;
    }

    /** Apply defaults for static fields with non-implicitly immutable types */
    public static class PICOTreeAnnotator extends TreeAnnotator {
        public PICOTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        // This adds @Immutable annotation to constructor return type if type declaration has
        // @Immutable when the
        // constructor is accessed as a tree.
        @Override
        public Void visitMethod(MethodTree node, AnnotatedTypeMirror p) {
            Element element = TreeUtils.elementFromDeclaration(node);
            // See:
            // https://github.com/opprop/checker-framework/blob/master/framework/src/org/checkerframework/framework/type/AnnotatedTypeFactory.java#L1593
            // for why constructor return is not applied class bound annotation
            PICOTypeUtil.defaultConstructorReturnToClassBound(atypeFactory, element, p);
            return super.visitMethod(node, p);
        }

        /** This covers the declaration of static fields */
        @Override
        public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            VariableElement element = TreeUtils.elementFromDeclaration(node);
            PICOTypeUtil.addDefaultForField(atypeFactory, annotatedTypeMirror, element);
            //            PICOTypeUtil.applyImmutableToEnumAndEnumConstant(annotatedTypeMirror);
            return super.visitVariable(node, annotatedTypeMirror);
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(IMMUTABLE);
            return null;
        }
    }

    /** Type Annotators */
    public static class PICOTypeAnnotator extends TypeAnnotator {

        public PICOTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        /**
         * Applies pre-knowledged defaults that are same with jdk.astub to toString, hashCode,
         * equals, clone Object methods
         */
        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            super.visitExecutable(t, p);

            // Only handle instance methods, not static methods
            if (!ElementUtils.isStatic(t.getElement())) {
                if (PICOTypeUtil.isMethodOrOverridingMethod(t, "toString()", atypeFactory)
                        || PICOTypeUtil.isMethodOrOverridingMethod(t, "hashCode()", atypeFactory)) {
                    assert t.getReceiverType() != null;
                    t.getReceiverType().replaceAnnotation(READONLY);
                } else if (PICOTypeUtil.isMethodOrOverridingMethod(
                        t, "equals(java.lang.Object)", atypeFactory)) {
                    assert t.getReceiverType() != null;
                    t.getReceiverType().replaceAnnotation(READONLY);
                    t.getParameterTypes().get(0).replaceAnnotation(READONLY);
                }
            }

            // Array decl methods
            // Array methods are implemented as JVM native method, so we cannot add that to stubs.
            // for now: default array in receiver, parameter and return type to RDM
            if (t.getReceiverType() != null) {
                if (PICOTypeUtil.isArrayType(t.getReceiverType(), atypeFactory)) {
                    switch (t.toString()) {
                        case "Object clone(Array this)":
                            // Receiver type will not be viewpoint adapted:
                            // SyntheticArrays.replaceReturnType() will rollback the viewpoint adapt
                            // result.
                            // Use readonly to allow all invocations.
                            if (!t.getReceiverType().hasAnnotationInHierarchy(READONLY))
                                t.getReceiverType().replaceAnnotation(READONLY);
                            // The return type will be fixed by SyntheticArrays anyway.
                            // Qualifiers added here will not have effect.
                            break;
                    }
                }
            }

            return null;
        }
    }

    @Override
    protected DefaultQualifierForUseTypeAnnotator createDefaultForUseTypeAnnotator() {
        return new PICOQualifierForUseTypeAnnotator(this);
    }

    // @DefaultQFU
    public static class PICOQualifierForUseTypeAnnotator
            extends DefaultQualifierForUseTypeAnnotator {

        public PICOQualifierForUseTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        @Override
        public Void visitDeclared(AnnotatedTypeMirror.AnnotatedDeclaredType type, Void aVoid) {

            Element element = type.getUnderlyingType().asElement();
            Set<AnnotationMirror> annosToApply = getDefaultAnnosForUses(element);

            if (annosToApply.contains(MUTABLE) || annosToApply.contains(IMMUTABLE)) {
                type.addMissingAnnotations(annosToApply);
            }

            // Below copied from super.super
            // TODO add a function to super.super visitor
            if (!type.getTypeArguments().isEmpty()) {
                // Only declared types with type arguments might be recursive.
                if (visitedNodes.containsKey(type)) {
                    return visitedNodes.get(type);
                }
                visitedNodes.put(type, null);
            }
            Void r = null;
            if (type.getEnclosingType() != null) {
                scan(type.getEnclosingType(), null);
            }
            r = scanAndReduce(type.getTypeArguments(), null, r);
            return r;
        }
    }

    public static class PICODefaultForTypeAnnotator extends DefaultForTypeAnnotator {

        public PICODefaultForTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        /** Also applies implicits to method receiver */
        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            // TODO The implementation before doesn't work after update. Previously, I sanned the
            // method receiver without null check. But even if I check nullness, scanning receiver
            // at first caused some tests to fail. Need to investigate the reason.
            super.visitExecutable(t, p);
            // Also scan the receiver to apply implicit annotation
            if (t.getReceiverType() != null) {
                return scanAndReduce(t.getReceiverType(), p, null);
            }
            return null;
        }

        @Override
        protected Void scan(AnnotatedTypeMirror type, Void p) {
            // If underlying type is enum or enum constant, appy @Immutable to type
            //            PICOTypeUtil.applyImmutableToEnumAndEnumConstant(type);
            return super.scan(type, p);
        }
    }

    // TODO Right now, instance method receiver cannot inherit bound annotation from class element,
    // and
    // this caused the inconsistency when accessing the type of receiver while visiting the method
    // and
    // while visiting the variable tree. Implicit annotation can be inserted to method receiver via
    // extending DefaultForTypeAnnotator; But InheritedFromClassAnnotator cannot be inheritted
    // because its
    // constructor is private and I can't override it to also inherit bound annotation from class
    // element
    // to the declared receiver type of instance methods. To view the details, look at
    // ImmutableClass1.java
    // testcase.
    // class PICOInheritedFromClassAnnotator extends InheritedFromClassAnnotator {}

    public static class PICOSuperClauseAnnotator extends TreeAnnotator {

        public PICOSuperClauseAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        public static boolean isSuperClause(TreePath path) {
            if (path == null) {
                return false;
            }
            return TreeUtils.isClassTree(path.getParentPath().getLeaf());
        }

        private void addDefaultFromMain(Tree tree, AnnotatedTypeMirror mirror) {
            TreePath path = atypeFactory.getPath(tree);

            // only annotates when:
            // 1. it's a super clause, AND
            // 2. atm OR tree is not annotated
            // Note: TreeUtils.typeOf returns no stub or default annotations, but in this scenario
            // they are not needed
            // Here only explicit annotation on super clause have effect because framework default
            // rule is overriden
            if (isSuperClause(path)
                    && (!mirror.hasAnnotationInHierarchy(READONLY)
                            || atypeFactory
                                            .getQualifierHierarchy()
                                            .findAnnotationInHierarchy(
                                                    TreeUtils.typeOf(tree).getAnnotationMirrors(),
                                                    READONLY)
                                    == null)) {
                AnnotatedTypeMirror enclosing =
                        atypeFactory.getAnnotatedType(TreePathUtil.enclosingClass(path));
                AnnotationMirror mainBound = enclosing.getAnnotationInHierarchy(READONLY);
                mirror.replaceAnnotation(mainBound);
                //                System.err.println("ANNOT: ADDED DEFAULT FOR: " + mirror);
            }
        }

        @Override
        public Void visitIdentifier(
                IdentifierTree identifierTree, AnnotatedTypeMirror annotatedTypeMirror) {
            // super clauses without type param use this
            addDefaultFromMain(identifierTree, annotatedTypeMirror);
            return super.visitIdentifier(identifierTree, annotatedTypeMirror);
        }

        @Override
        public Void visitParameterizedType(
                ParameterizedTypeTree parameterizedTypeTree,
                AnnotatedTypeMirror annotatedTypeMirror) {
            // super clauses with type param use this
            addDefaultFromMain(parameterizedTypeTree, annotatedTypeMirror);
            return super.visitParameterizedType(parameterizedTypeTree, annotatedTypeMirror);
        }
    }

    public static class PICOEnumDefaultAnnotator extends TypeAnnotator {
        // Defaulting only applies the same annotation to all class declarations
        // We need this to "only default enums" to immutable

        public PICOEnumDefaultAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        @Override
        public Void visitDeclared(AnnotatedTypeMirror.AnnotatedDeclaredType type, Void aVoid) {
            if (PICOTypeUtil.isEnumOrEnumConstant(type)) {
                type.addMissingAnnotations(Collections.singleton(IMMUTABLE));
            }
            return super.visitDeclared(type, aVoid);
        }
    }
}

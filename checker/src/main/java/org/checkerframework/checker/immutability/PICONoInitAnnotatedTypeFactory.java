package org.checkerframework.checker.immutability;

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
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.NoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.ViewpointAdapter;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.DefaultForTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.DefaultQualifierForUseTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.IrrelevantTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
// TODO Use @Immutable for classes that extends those predefined immutable classes like String or
// Number
// and explicitly annotated classes with @Immutable on its declaration
public class PICONoInitAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<
                PICONoInitValue, PICONoInitStore, PICONoInitTransfer, PICONoInitAnalysis>
        implements ViewpointAdapterGettable {

    public final AnnotationMirror READONLY = AnnotationBuilder.fromClass(elements, Readonly.class);
    public final AnnotationMirror MUTABLE = AnnotationBuilder.fromClass(elements, Mutable.class);
    public final AnnotationMirror POLY_MUTABLE =
            AnnotationBuilder.fromClass(elements, PolyMutable.class);
    public final AnnotationMirror RECEIVER_DEPENDANT_MUTABLE =
            AnnotationBuilder.fromClass(elements, ReceiverDependantMutable.class);
    public final AnnotationMirror IMMUTABLE =
            AnnotationBuilder.fromClass(elements, Immutable.class);
    public final AnnotationMirror BOTTOM = AnnotationBuilder.fromClass(elements, Bottom.class);

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
        if (!hasExplicitAnnos
                && method.getReturnType().hasAnnotation(ReceiverDependantMutable.class)) {
            method.getReturnType().replaceAnnotation(MUTABLE);
        }
        return mType;
    }

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
        super.addComputedTypeAnnotations(elt, type);
    }

    /** Tree Annotators */
    public class PICOPropagationTreeAnnotator extends PropagationTreeAnnotator {
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
            if (!componentType.hasAnnotation(ReceiverDependantMutable.class)) {
                noExplicitATM = true;
            }
            super.visitNewArray(tree, type);
            // if new explicit anno before, but RDM after, the RDM must come from the type
            // declaration bound
            // however, for type has declaration bound as RDM, its default use is mutable.
            if (noExplicitATM && componentType.hasAnnotation(ReceiverDependantMutable.class)) {
                componentType.replaceAnnotation(MUTABLE);
            }
            return null;
        }

        @Override
        public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
            boolean hasExplicitAnnos = false;
            if (!type.getAnnotations().isEmpty()) {
                hasExplicitAnnos = true;
            }
            super.visitTypeCast(node, type);
            if (!hasExplicitAnnos && type.hasAnnotation(ReceiverDependantMutable.class)) {
                type.replaceAnnotation(MUTABLE);
            }
            return null;
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            return null;
        }
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
    public class PICOTreeAnnotator extends TreeAnnotator {
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
            PICOTypeUtil.defaultConstructorReturnToClassBound(
                    (PICONoInitAnnotatedTypeFactory) atypeFactory, element, p);
            return super.visitMethod(node, p);
        }

        /** This covers the declaration of static fields */
        @Override
        public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            VariableElement element = TreeUtils.elementFromDeclaration(node);
            PICOTypeUtil.addDefaultForField(
                    (PICONoInitAnnotatedTypeFactory) atypeFactory, annotatedTypeMirror, element);
            return super.visitVariable(node, annotatedTypeMirror);
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(IMMUTABLE);
            return null;
        }
    }

    /** Type Annotators */
    public class PICOTypeAnnotator extends TypeAnnotator {

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
    public class PICOQualifierForUseTypeAnnotator extends DefaultQualifierForUseTypeAnnotator {

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

        public PICOSuperClauseAnnotator(PICONoInitAnnotatedTypeFactory atypeFactory) {
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
                    && (!mirror.hasAnnotationInHierarchy(
                                    ((PICONoInitAnnotatedTypeFactory) atypeFactory).READONLY)
                            || atypeFactory
                                            .getQualifierHierarchy()
                                            .findAnnotationInHierarchy(
                                                    TreeUtils.typeOf(tree).getAnnotationMirrors(),
                                                    ((PICONoInitAnnotatedTypeFactory) atypeFactory)
                                                            .READONLY)
                                    == null)) {
                AnnotatedTypeMirror enclosing =
                        atypeFactory.getAnnotatedType(TreePathUtil.enclosingClass(path));
                AnnotationMirror mainBound =
                        enclosing.getAnnotationInHierarchy(
                                ((PICONoInitAnnotatedTypeFactory) atypeFactory).READONLY);
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

    public class PICOEnumDefaultAnnotator extends TypeAnnotator {
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

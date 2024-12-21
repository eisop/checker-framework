package org.checkerframework.checker.pico;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import org.checkerframework.checker.initialization.InitializationFieldAccessTreeAnnotator;
import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.PICOBottom;
import org.checkerframework.checker.pico.qual.PICOLost;
import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.ViewpointAdapter;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.DefaultForTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.DefaultQualifierForUseTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
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
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
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
// Number and explicitly annotated classes with @Immutable on its declaration
public class PICONoInitAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<
                PICONoInitValue, PICONoInitStore, PICONoInitTransfer, PICONoInitAnalysis> {
    /** The @{@link Mutable} annotation. */
    protected final AnnotationMirror MUTABLE = AnnotationBuilder.fromClass(elements, Mutable.class);

    /** The @{@link Immutable} annotation. */
    protected final AnnotationMirror IMMUTABLE =
            AnnotationBuilder.fromClass(elements, Immutable.class);

    /** The @{@link Readonly} annotation. */
    protected final AnnotationMirror READONLY =
            AnnotationBuilder.fromClass(elements, Readonly.class);

    /** The @{@link ReceiverDependentMutable} annotation. */
    protected final AnnotationMirror RECEIVER_DEPENDENT_MUTABLE =
            AnnotationBuilder.fromClass(elements, ReceiverDependentMutable.class);

    /** The @{@link PolyMutable} annotation. */
    protected final AnnotationMirror POLY_MUTABLE =
            AnnotationBuilder.fromClass(elements, PolyMutable.class);

    /** The @{@link PICOLost} annotation. */
    protected final AnnotationMirror LOST = AnnotationBuilder.fromClass(elements, PICOLost.class);

    /** The @{@link PICOBottom} annotation. */
    protected final AnnotationMirror BOTTOM =
            AnnotationBuilder.fromClass(elements, PICOBottom.class);

    /**
     * Create a new PICONoInitAnnotatedTypeFactory.
     *
     * @param checker the BaseTypeChecker
     */
    public PICONoInitAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
        // PICO aliasing is not implemented correctly remove for now
        // addAliasedAnnotation(org.jmlspecs.annotation.Readonly.class, READONLY);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(
                        Readonly.class,
                        Mutable.class,
                        PolyMutable.class,
                        ReceiverDependentMutable.class,
                        Immutable.class,
                        PICOLost.class,
                        PICOBottom.class));
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

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        // Adding order is important here. Because internally type annotators are using
        // addMissingAnnotations() method, so if one annotator already applied the annotations, the
        // others won't apply twice at the same location
        return new ListTypeAnnotator(
                super.createTypeAnnotator(),
                new PICOTypeAnnotator(this),
                new PICODefaultForTypeAnnotator(this),
                new PICOEnumDefaultAnnotator(this));
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy() {
        return new PICOQualifierHierarchy(getSupportedTypeQualifiers(), elements, this);
    }

    @Override
    public ParameterizedExecutableType constructorFromUse(NewClassTree tree) {
        boolean hasExplicitAnnos = getExplicitNewClassAnnos(tree).isEmpty();
        ParameterizedExecutableType mType = super.constructorFromUse(tree);
        AnnotatedExecutableType method = mType.executableType;
        if (hasExplicitAnnos && method.getReturnType().hasAnnotation(RECEIVER_DEPENDENT_MUTABLE)) {
            method.getReturnType().replaceAnnotation(MUTABLE);
        }
        return mType;
    }

    /**
     * {@inheritDoc} Forbid applying top annotations to type variables if they are used on local
     * variables.
     */
    @Override
    public boolean getShouldDefaultTypeVarLocals() {
        return false;
    }

    /**
     * {@inheritDoc} This covers the case when static fields are used and constructor is accessed as
     * an element(regarding applying @Immutable on type declaration to constructor return type).
     */
    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        addDefaultForField(this, type, elt);
        defaultConstructorReturnToClassBound(this, elt, type);
        super.addComputedTypeAnnotations(elt, type);
    }

    /**
     * Get the PICO viewpoint adapter.
     *
     * @return PICOViewpointAdapter
     */
    public PICOViewpointAdapter getViewpointAdapter() {
        return (PICOViewpointAdapter) viewpointAdapter;
    }

    /**
     * {@inheritDoc} Changes the framework default to @Mutable
     *
     * @return Mutable default AnnotationMirrorSet
     */
    @Override
    protected AnnotationMirrorSet getDefaultTypeDeclarationBounds() {
        AnnotationMirrorSet frameworkDefault =
                new AnnotationMirrorSet(super.getDefaultTypeDeclarationBounds());
        //        if (checker.hasOption("immutableDefault")) {
        //            return replaceAnnotationInHierarchy(frameworkDefault, IMMUTABLE);
        //        }
        return replaceAnnotationInHierarchy(frameworkDefault, IMMUTABLE);
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

    /**
     * Replace the annotation in the hierarchy with the given AnnotationMirrorSet.
     *
     * @param set The AnnotationMirrorSet to replace the annotation in
     * @param mirror The AnnotationMirror to replace with
     * @return The replaced AnnotationMirrorSet
     */
    private AnnotationMirrorSet replaceAnnotationInHierarchy(
            AnnotationMirrorSet set, AnnotationMirror mirror) {
        AnnotationMirrorSet result = new AnnotationMirrorSet(set);
        AnnotationMirror removeThis =
                getQualifierHierarchy().findAnnotationInSameHierarchy(set, mirror);
        result.remove(removeThis);
        result.add(mirror);
        return result;
    }

    /**
     * Get the upperbound give a TypeMirror 1. If the type is implicitly immutable,
     * return @Immutable 2. If the type is an enum, return @Immutable if it has no explicit
     * annotation 3. If the type is an array, return @ReceiverDependentMutable 4. Otherwise, return
     * null
     *
     * @param type the type to get the upperbound for
     * @return the upperbound for the given type
     */
    public AnnotationMirror getTypeDeclarationBoundForMutability(TypeMirror type) {
        if (PICOTypeUtil.isImplicitlyImmutableType(toAnnotatedType(type, false))) {
            return IMMUTABLE;
        }
        if (type.getKind() == TypeKind.ARRAY) {
            return RECEIVER_DEPENDENT_MUTABLE; // if decided to use vpa for array, return RDM.
        }
        // IMMUTABLE for enum w/o decl anno
        if (type instanceof DeclaredType) {
            Element ele = ((DeclaredType) type).asElement();
            if (ele.getKind() == ElementKind.ENUM) {
                // TODO refine the logic here for enum
                if (!AnnotationUtils.containsSameByName(getDeclAnnotations(ele), MUTABLE)
                        && !AnnotationUtils.containsSameByName(
                                getDeclAnnotations(ele),
                                RECEIVER_DEPENDENT_MUTABLE)) { // no decl anno
                    return IMMUTABLE;
                }
            }
        }
        return null;
    }

    @Override
    public AnnotatedTypeMirror getTypeOfExtendsImplements(Tree clause) {
        // this is still needed with PICOSuperClauseAnnotator.
        // maybe just use getAnnotatedType add default anno from class main qual, if no qual present
        AnnotatedTypeMirror fromTypeTree = super.getTypeOfExtendsImplements(clause);
        if (fromTypeTree.hasAnnotation(RECEIVER_DEPENDENT_MUTABLE)) {
            ClassTree enclosingClass = TreePathUtil.enclosingClass(getPath(clause));
            // TODO This is a hack but fixed a few crash errors, look what will be the overall
            // solution.
            if (enclosingClass == null) {
                return fromTypeTree;
            } else {
                AnnotatedTypeMirror enclosing = getAnnotatedType(enclosingClass);
                AnnotationMirror mainBound = enclosing.getAnnotationInHierarchy(READONLY);
                fromTypeTree.replaceAnnotation(mainBound);
            }
        }
        return fromTypeTree;
    }

    /**
     * Add default annotation to the given AnnotatedTypeMirror for the given Element.
     *
     * @param annotatedTypeFactory the annotated type factory
     * @param annotatedTypeMirror the annotated type mirror to add default annotation
     * @param element the element to add default annotation
     */
    private void addDefaultForField(
            AnnotatedTypeFactory annotatedTypeFactory,
            AnnotatedTypeMirror annotatedTypeMirror,
            Element element) {
        if (element != null && element.getKind() == ElementKind.FIELD) {
            // If the field is static, apply @Mutable if there is no explicit annotation and the
            // field type is @RDM
            if (ElementUtils.isStatic(element)) {
                //               AnnotatedTypeMirror implicitATM =
                // annotatedTypeFactory.getAnnotatedType(element);
                AnnotatedTypeMirror explicitATM = annotatedTypeFactory.fromElement(element);
                AnnotationMirrorSet declBound =
                        annotatedTypeFactory.getTypeDeclarationBounds(element.asType());
                if (!explicitATM.hasAnnotationInHierarchy(READONLY)
                        && AnnotationUtils.containsSameByName(
                                declBound, RECEIVER_DEPENDENT_MUTABLE)) {
                    if (!PICOTypeUtil.isImplicitlyImmutableType(explicitATM)) {
                        annotatedTypeMirror.replaceAnnotation(IMMUTABLE);
                    } else {
                        annotatedTypeMirror.replaceAnnotation(MUTABLE);
                    }
                }
            } else {
                // Apply default annotation to instance fields if there is no explicit annotation
                AnnotatedTypeMirror explicitATM = annotatedTypeFactory.fromElement(element);
                if (!explicitATM.hasAnnotationInHierarchy(READONLY)) {
                    if (explicitATM instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
                        AnnotatedTypeMirror.AnnotatedDeclaredType adt =
                                (AnnotatedTypeMirror.AnnotatedDeclaredType) explicitATM;
                        Element typeElement = adt.getUnderlyingType().asElement();

                        AnnotationMirrorSet enclosingBound =
                                annotatedTypeFactory.getTypeDeclarationBounds(
                                        Objects.requireNonNull(
                                                        ElementUtils.enclosingTypeElement(element))
                                                .asType());
                        AnnotationMirrorSet declBound =
                                annotatedTypeFactory.getTypeDeclarationBounds(element.asType());
                        // Add RDM if Type declaration bound=M and enclosing class Bound=M/RDM
                        // If the declaration bound is mutable and the enclosing class is also
                        // mutable, replace the annotation as RDM.
                        if (AnnotationUtils.containsSameByName(declBound, MUTABLE)
                                && AnnotationUtils.containsSameByName(enclosingBound, MUTABLE)) {
                            annotatedTypeMirror.replaceAnnotation(RECEIVER_DEPENDENT_MUTABLE);
                        }
                        // If the declaration bound is RDM, replace the annotation as RDM
                        if (typeElement instanceof TypeElement) {
                            AnnotatedTypeMirror bound =
                                    PICOTypeUtil.getBoundTypeOfTypeDeclaration(
                                            typeElement, annotatedTypeFactory);
                            if (bound.hasAnnotation(RECEIVER_DEPENDENT_MUTABLE)) {
                                annotatedTypeMirror.replaceAnnotation(RECEIVER_DEPENDENT_MUTABLE);
                            }
                        }
                    } else if (explicitATM instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
                        // If the ATM is array type, apply RMD to array's component type.
                        annotatedTypeMirror.replaceAnnotation(RECEIVER_DEPENDENT_MUTABLE);
                    }
                }
            }
        }
    }

    /**
     * Add default annotation from type declaration to constructor return type if elt is constructor
     * and doesn't have explicit annotation(type is actually AnnotatedExecutableType of executable
     * element - elt constructor).
     *
     * @param annotatedTypeFactory the annotated type factory
     * @param elt the element to add default annotation
     * @param type the type to add default annotation
     */
    private void defaultConstructorReturnToClassBound(
            AnnotatedTypeFactory annotatedTypeFactory, Element elt, AnnotatedTypeMirror type) {
        if (elt.getKind() == ElementKind.CONSTRUCTOR && type instanceof AnnotatedExecutableType) {
            AnnotatedTypeMirror bound =
                    PICOTypeUtil.getBoundTypeOfEnclosingTypeDeclaration(elt, annotatedTypeFactory);
            ((AnnotatedExecutableType) type)
                    .getReturnType()
                    .addMissingAnnotations(Arrays.asList(bound.getAnnotationInHierarchy(READONLY)));
        }
    }

    /** Tree Annotators */
    public static class PICOPropagationTreeAnnotator extends PropagationTreeAnnotator {
        /** The PICO type factory. */
        private PICONoInitAnnotatedTypeFactory picoTypeFactory;

        /**
         * Create a new PICOPropagationTreeAnnotator.
         *
         * @param atypeFactory the type factory
         */
        public PICOPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
            picoTypeFactory = (PICONoInitAnnotatedTypeFactory) atypeFactory;
        }

        //
        // TODO This is very ugly. Why is array component type from lhs propagates to
        // rhs?!
        @Override
        public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror componentType =
                    ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType();
            boolean noExplicitATM =
                    !componentType.hasAnnotation(picoTypeFactory.RECEIVER_DEPENDENT_MUTABLE);
            super.visitNewArray(tree, type);
            // if new explicit anno before, but RDM after, the RDM must come from the type
            // declaration bound
            // however, for type has declaration bound as RDM, its default use is mutable.
            if (noExplicitATM
                    && componentType.hasAnnotation(picoTypeFactory.RECEIVER_DEPENDENT_MUTABLE)) {
                //                if (checker.hasOption("immutableDefault")) {
                //                    componentType.replaceAnnotation(IMMUTABLE);
                //                } else
                componentType.replaceAnnotation(picoTypeFactory.IMMUTABLE);
            }
            return null;
        }

        @Override
        public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
            boolean hasExplicitAnnos = !type.getAnnotations().isEmpty();
            super.visitTypeCast(node, type);
            if (!hasExplicitAnnos
                    && type.hasAnnotation(picoTypeFactory.RECEIVER_DEPENDENT_MUTABLE)) {
                //                if (checker.hasOption("immutableDefault")) {
                //                    type.replaceAnnotation(IMMUTABLE);
                //                } else
                type.replaceAnnotation(picoTypeFactory.IMMUTABLE);
            }
            return null;
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            return null;
        }
    }

    /** Apply defaults for static fields with non-implicitly immutable types. */
    public static class PICOTreeAnnotator extends TreeAnnotator {
        /** The PICO type factory. */
        private PICONoInitAnnotatedTypeFactory picoTypeFactory;

        /**
         * Create a new PICOTreeAnnotator.
         *
         * @param atypeFactory the type factory
         */
        public PICOTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
            picoTypeFactory = (PICONoInitAnnotatedTypeFactory) atypeFactory;
        }

        /**
         * {@inheritDoc} This adds @Immutable annotation to constructor return type if type
         * declaration has @Immutable when the constructor is accessed as a tree.
         */
        @Override
        public Void visitMethod(MethodTree tree, AnnotatedTypeMirror p) {
            Element element = TreeUtils.elementFromDeclaration(tree);
            picoTypeFactory.defaultConstructorReturnToClassBound(atypeFactory, element, p);
            return super.visitMethod(tree, p);
        }

        /** {@inheritDoc} This covers the declaration of static fields */
        @Override
        public Void visitVariable(VariableTree tree, AnnotatedTypeMirror annotatedTypeMirror) {
            VariableElement element = TreeUtils.elementFromDeclaration(tree);
            picoTypeFactory.addDefaultForField(atypeFactory, annotatedTypeMirror, element);
            return super.visitVariable(tree, annotatedTypeMirror);
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            type.replaceAnnotation(picoTypeFactory.IMMUTABLE);
            return null;
        }
    }

    /** Type Annotators */
    public static class PICOTypeAnnotator extends TypeAnnotator {
        /** The PICO type factory. */
        private PICONoInitAnnotatedTypeFactory picoTypeFactory;

        /**
         * Create a new PICOTypeAnnotator.
         *
         * @param typeFactory the type factory
         */
        public PICOTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
            picoTypeFactory = (PICONoInitAnnotatedTypeFactory) typeFactory;
        }

        /**
         * {@inheritDoc} Applies pre-knowledged defaults that are same with jdk.astub to toString,
         * hashCode, equals, clone Object methods.
         */
        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            super.visitExecutable(t, p);

            // Only handle instance methods, not static methods
            if (!ElementUtils.isStatic(t.getElement())) {
                if (PICOTypeUtil.isMethodOrOverridingMethod(t, "toString()", atypeFactory)
                        || PICOTypeUtil.isMethodOrOverridingMethod(t, "hashCode()", atypeFactory)) {
                    assert t.getReceiverType() != null;
                    t.getReceiverType().replaceAnnotation(picoTypeFactory.READONLY);
                } else if (PICOTypeUtil.isMethodOrOverridingMethod(
                        t, "equals(java.lang.Object)", atypeFactory)) {
                    assert t.getReceiverType() != null;
                    t.getReceiverType().replaceAnnotation(picoTypeFactory.READONLY);
                    t.getParameterTypes().get(0).replaceAnnotation(picoTypeFactory.READONLY);
                }
            } else {
                return null;
            }

            // Array decl methods
            // Array methods are implemented as JVM native method, so we cannot add that to stubs.
            // for now: default array in receiver, parameter and return type to RDM
            if (t.getReceiverType() != null) {
                if (PICOTypeUtil.isArrayType(t.getReceiverType(), atypeFactory)) {
                    if (t.toString()
                            .equals("Object clone(Array this)")) { // Receiver type will not be
                        // viewpoint adapted:
                        // SyntheticArrays.replaceReturnType() will rollback the viewpoint adapt
                        // result.
                        // Use readonly to allow all invocations.
                        if (!t.getReceiverType().hasAnnotationInHierarchy(picoTypeFactory.READONLY))
                            t.getReceiverType().replaceAnnotation(picoTypeFactory.READONLY);
                        // The return type will be fixed by SyntheticArrays anyway.
                        // Qualifiers added here will not have effect.
                    }
                }
            }

            return null;
        }
    }

    /**
     * {@inheritDoc} This is for overriding the behavior of DefaultQualifierForUse and use
     * PICOQualifierForUseTypeAnnotator.
     *
     * @return PICOQualifierForUseTypeAnnotator
     */
    @Override
    protected DefaultQualifierForUseTypeAnnotator createDefaultForUseTypeAnnotator() {
        return new PICOQualifierForUseTypeAnnotator(this);
    }

    /** QualifierForUseTypeAnnotator */
    public static class PICOQualifierForUseTypeAnnotator
            extends DefaultQualifierForUseTypeAnnotator {
        /** The PICO type factory. */
        private PICONoInitAnnotatedTypeFactory picoTypeFactory;

        /**
         * Create a new PICOQualifierForUseTypeAnnotator.
         *
         * @param typeFactory the type factory
         */
        public PICOQualifierForUseTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
            picoTypeFactory = (PICONoInitAnnotatedTypeFactory) typeFactory;
        }

        @Override
        public Void visitDeclared(AnnotatedTypeMirror.AnnotatedDeclaredType type, Void aVoid) {

            Element element = type.getUnderlyingType().asElement();
            AnnotationMirrorSet annosToApply = getDefaultAnnosForUses(element);

            if (annosToApply.contains(picoTypeFactory.MUTABLE)
                    || annosToApply.contains(picoTypeFactory.IMMUTABLE)) {
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

    /** DefaultForTypeAnnotator */
    public static class PICODefaultForTypeAnnotator extends DefaultForTypeAnnotator {

        /**
         * Create a new PICODefaultForTypeAnnotator.
         *
         * @param typeFactory the type factory
         */
        public PICODefaultForTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        /** Also applies implicits to method receiver */
        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            // TODO The implementation before doesn't work after update. Previously, I scanned the
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
    // and this caused the inconsistency when accessing the type of receiver while visiting the
    // method
    // and while visiting the variable tree. Implicit annotation can be inserted to method receiver
    // via
    // extending DefaultForTypeAnnotator; But InheritedFromClassAnnotator cannot be inheritted
    // because its constructor is private and I can't override it to also inherit bound annotation
    // from class
    // element to the declared receiver type of instance methods. To view the details, look at
    // ImmutableClass1.java testcase.
    // class PICOInheritedFromClassAnnotator extends InheritedFromClassAnnotator {}
    /** PICO SuperClause Annotator */
    public static class PICOSuperClauseAnnotator extends TreeAnnotator {
        /** The PICO type factory. */
        private PICONoInitAnnotatedTypeFactory picoTypeFactory;

        /**
         * Create a new PICOSuperClauseAnnotator.
         *
         * @param atypeFactory the type factory
         */
        public PICOSuperClauseAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
            picoTypeFactory = (PICONoInitAnnotatedTypeFactory) atypeFactory;
        }

        /**
         * Check if the given path is a super clause.
         *
         * @param path the path to check
         * @return true if the given path is a super clause, false otherwise
         */
        public static boolean isSuperClause(TreePath path) {
            if (path == null) {
                return false;
            }
            return TreeUtils.isClassTree(path.getParentPath().getLeaf());
        }

        /**
         * Add default annotation from main class to super clause
         *
         * @param tree the tree to add default annotation
         * @param mirror the annotated type mirror to add default annotation
         */
        private void addDefaultFromMain(Tree tree, AnnotatedTypeMirror mirror) {
            TreePath path = atypeFactory.getPath(tree);

            // only annotates when:
            // 1. it's a super clause, AND
            // 2. atm OR tree is not annotated
            // Note: TreeUtils.typeOf returns no stub or default annotations, but in this scenario
            // they are not needed
            // Here only explicit annotation on super clause have effect because framework default
            // rule is override
            if (isSuperClause(path)
                    && (!mirror.hasAnnotationInHierarchy(picoTypeFactory.READONLY)
                            || atypeFactory
                                            .getQualifierHierarchy()
                                            .findAnnotationInHierarchy(
                                                    TreeUtils.typeOf(tree).getAnnotationMirrors(),
                                                    picoTypeFactory.READONLY)
                                    == null)) {
                AnnotatedTypeMirror enclosing =
                        atypeFactory.getAnnotatedType(TreePathUtil.enclosingClass(path));
                AnnotationMirror mainBound =
                        enclosing.getAnnotationInHierarchy(picoTypeFactory.READONLY);
                mirror.replaceAnnotation(mainBound);
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

    /**
     * Defaulting only applies the same annotation to all class declarations, and we need this to
     * "only default enums" to immutable
     */
    public static class PICOEnumDefaultAnnotator extends TypeAnnotator {
        /** The PICO type factory. */
        private PICONoInitAnnotatedTypeFactory picoTypeFactory;

        /**
         * Create a new PICOEnumDefaultAnnotator.
         *
         * @param typeFactory the type factory
         */
        public PICOEnumDefaultAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
            picoTypeFactory = (PICONoInitAnnotatedTypeFactory) typeFactory;
        }

        @Override
        public Void visitDeclared(AnnotatedTypeMirror.AnnotatedDeclaredType type, Void aVoid) {
            if (PICOTypeUtil.isEnumOrEnumConstant(type)) {
                type.addMissingAnnotations(Collections.singleton(picoTypeFactory.IMMUTABLE));
            }
            return super.visitDeclared(type, aVoid);
        }
    }
}

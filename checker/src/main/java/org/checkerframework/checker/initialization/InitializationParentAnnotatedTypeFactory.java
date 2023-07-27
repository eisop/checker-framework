package org.checkerframework.checker.initialization;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

import org.checkerframework.checker.initialization.qual.FBCBottom;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.qual.Unused;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Superclass for {@link InitializationDeclarationAnnotatedTypeFactory} and {@link
 * InitializationAnnotatedTypeFactory} to contain common functionality.
 *
 * @param <Value> the value type for this type factory
 * @param <Store> the store type for this type factory
 * @param <Transfer> the transfer function type for this type factory
 * @param <Analysis> the analysis type for this type factory
 */
public abstract class InitializationParentAnnotatedTypeFactory<
                Value extends CFAbstractValue<Value>,
                Store extends CFAbstractStore<Value, Store>,
                Transfer extends CFAbstractTransfer<Value, Store, Transfer>,
                Analysis extends CFAbstractAnalysis<Value, Store, Transfer>>
        extends GenericAnnotatedTypeFactory<Value, Store, Transfer, Analysis> {

    /** {@link UnknownInitialization}. */
    protected final AnnotationMirror UNKNOWN_INITIALIZATION;

    /** {@link Initialized}. */
    protected final AnnotationMirror INITIALIZED;

    /** {@link UnderInitialization} or null. */
    protected final AnnotationMirror UNDER_INITALIZATION;

    /** {@link NotOnlyInitialized} or null. */
    protected final AnnotationMirror NOT_ONLY_INITIALIZED;

    /** {@link FBCBottom}. */
    protected final AnnotationMirror FBCBOTTOM;

    /** The java.lang.Object type. */
    protected final TypeMirror objectTypeMirror;

    /** The Unused.when field/element. */
    protected final ExecutableElement unusedWhenElement;

    /** The UnderInitialization.value field/element. */
    protected final ExecutableElement underInitializationValueElement;

    /** The UnknownInitialization.value field/element. */
    protected final ExecutableElement unknownInitializationValueElement;

    public InitializationParentAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);

        UNKNOWN_INITIALIZATION = AnnotationBuilder.fromClass(elements, UnknownInitialization.class);
        INITIALIZED = AnnotationBuilder.fromClass(elements, Initialized.class);
        UNDER_INITALIZATION = AnnotationBuilder.fromClass(elements, UnderInitialization.class);
        NOT_ONLY_INITIALIZED = AnnotationBuilder.fromClass(elements, NotOnlyInitialized.class);
        FBCBOTTOM = AnnotationBuilder.fromClass(elements, FBCBottom.class);

        objectTypeMirror =
                processingEnv.getElementUtils().getTypeElement("java.lang.Object").asType();
        unusedWhenElement = TreeUtils.getMethod(Unused.class, "when", 0, processingEnv);
        underInitializationValueElement =
                TreeUtils.getMethod(UnderInitialization.class, "value", 0, processingEnv);
        unknownInitializationValueElement =
                TreeUtils.getMethod(UnknownInitialization.class, "value", 0, processingEnv);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return Set.of(
                UnknownInitialization.class,
                UnderInitialization.class,
                Initialized.class,
                FBCBottom.class);
    }

    /**
     * Returns {@code true}. Initialization cannot be undone, i.e., an @Initialized object always
     * stays @Initialized, an @UnderInitialization(A) object always stays @UnderInitialization(A)
     * (though it may additionally become @Initialized), etc.
     */
    @Override
    public boolean isImmutable(TypeMirror type) {
        return true;
    }

    /**
     * Creates a {@link UnderInitialization} annotation with the given type as its type frame
     * argument.
     *
     * @param typeFrame the type down to which some value has been initialized
     * @return an {@link UnderInitialization} annotation with the given argument
     */
    public AnnotationMirror createUnderInitializationAnnotation(TypeMirror typeFrame) {
        assert typeFrame != null;
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, UnderInitialization.class);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    @Override
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        AnnotatedDeclaredType selfType = super.getSelfType(tree);

        TreePath path = getPath(tree);
        AnnotatedDeclaredType enclosing = selfType;
        while (path != null && enclosing != null) {
            TreePath topLevelMemberPath = findTopLevelClassMemberForTree(path);
            if (topLevelMemberPath != null && topLevelMemberPath.getLeaf() != null) {
                Tree topLevelMember = topLevelMemberPath.getLeaf();
                if (topLevelMember.getKind() != Tree.Kind.METHOD
                        || TreeUtils.isConstructor((MethodTree) topLevelMember)) {
                    setSelfTypeInInitializationCode(tree, enclosing, topLevelMemberPath);
                }
                path = topLevelMemberPath.getParentPath();
                enclosing = enclosing.getEnclosingType();
            } else {
                break;
            }
        }

        return selfType;
    }

    /**
     * In the first enclosing class, find the path to the top-level member that contains {@code
     * path}.
     *
     * @param path the path whose leaf is the target
     * @return path to a top-level member containing the leaf of {@code path}
     */
    @SuppressWarnings("interning:not.interned") // AST node comparison
    private TreePath findTopLevelClassMemberForTree(TreePath path) {
        if (TreeUtils.isClassTree(path.getLeaf())) {
            path = path.getParentPath();
            if (path == null) {
                return null;
            }
        }
        ClassTree enclosingClass = TreePathUtil.enclosingClass(path);
        if (enclosingClass != null) {
            List<? extends Tree> classMembers = enclosingClass.getMembers();
            TreePath searchPath = path;
            while (searchPath.getParentPath() != null
                    && searchPath.getParentPath().getLeaf() != enclosingClass) {
                searchPath = searchPath.getParentPath();
                if (classMembers.contains(searchPath.getLeaf())) {
                    return searchPath;
                }
            }
        }
        return null;
    }

    /**
     * Side-effects argument {@code selfType} to make it @Initialized or @UnderInitialization,
     * depending on whether all fields have been set.
     *
     * @param tree a tree
     * @param selfType the type to side-effect
     * @param path a path
     */
    protected void setSelfTypeInInitializationCode(
            Tree tree, AnnotatedDeclaredType selfType, TreePath path) {
        ClassTree enclosingClass = TreePathUtil.enclosingClass(path);
        Type classType = ((JCTree) enclosingClass).type;
        AnnotationMirror annotation = null;

        if (annotation == null) {
            annotation = getUnderInitializationAnnotationOfSuperType(classType);
        }
        selfType.replaceAnnotation(annotation);
    }

    /**
     * Returns an {@link UnderInitialization} annotation that has the superclass of {@code type} as
     * type frame.
     *
     * @param type a type
     * @return true an {@link UnderInitialization} for the supertype of {@code type}
     */
    protected AnnotationMirror getUnderInitializationAnnotationOfSuperType(TypeMirror type) {
        // Find supertype if possible.
        AnnotationMirror annotation;
        List<? extends TypeMirror> superTypes = types.directSupertypes(type);
        TypeMirror superClass = null;
        for (TypeMirror superType : superTypes) {
            ElementKind kind = types.asElement(superType).getKind();
            if (kind == ElementKind.CLASS) {
                superClass = superType;
                break;
            }
        }
        // Create annotation.
        if (superClass != null) {
            annotation = createUnderInitializationAnnotation(superClass);
        } else {
            // Use Object as a valid super-class.
            annotation = createUnderInitializationAnnotation(Object.class);
        }
        return annotation;
    }

    /** Returns whether the field {@code f} is unused, given the annotations on the receiver. */
    protected boolean isUnused(
            VariableTree field, Collection<? extends AnnotationMirror> receiverAnnos) {
        if (receiverAnnos.isEmpty()) {
            return false;
        }

        AnnotationMirror unused =
                getDeclAnnotation(TreeUtils.elementFromDeclaration(field), Unused.class);
        if (unused == null) {
            return false;
        }

        Name when = AnnotationUtils.getElementValueClassName(unused, unusedWhenElement);
        for (AnnotationMirror anno : receiverAnnos) {
            Name annoName = ((TypeElement) anno.getAnnotationType().asElement()).getQualifiedName();
            if (annoName.contentEquals(when)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates a {@link UnderInitialization} annotation with the given type frame.
     *
     * @param typeFrame the type down to which some value has been initialized
     * @return an {@link UnderInitialization} annotation with the given argument
     */
    public AnnotationMirror createUnderInitializationAnnotation(Class<?> typeFrame) {
        assert typeFrame != null;
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, UnderInitialization.class);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Creates a {@link UnknownInitialization} annotation with a given type frame.
     *
     * @param typeFrame the type down to which some value has been initialized
     * @return an {@link UnknownInitialization} annotation with the given argument
     */
    public AnnotationMirror createUnknownInitializationAnnotation(Class<?> typeFrame) {
        assert typeFrame != null;
        AnnotationBuilder builder =
                new AnnotationBuilder(processingEnv, UnknownInitialization.class);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Creates an {@link UnknownInitialization} annotation with a given type frame.
     *
     * @param typeFrame the type down to which some value has been initialized
     * @return an {@link UnknownInitialization} annotation with the given argument
     */
    public AnnotationMirror createUnknownInitializationAnnotation(TypeMirror typeFrame) {
        assert typeFrame != null;
        AnnotationBuilder builder =
                new AnnotationBuilder(processingEnv, UnknownInitialization.class);
        builder.setValue("value", typeFrame);
        return builder.build();
    }

    /**
     * Is {@code anno} the {@link UnderInitialization} annotation (with any type frame)?
     *
     * @param anno the annotation to check
     * @return true if {@code anno} is {@link UnderInitialization}
     */
    public boolean isUnderInitialization(AnnotationMirror anno) {
        return areSameByClass(anno, UnderInitialization.class);
    }

    /**
     * Is {@code anno} the {@link UnknownInitialization} annotation (with any type frame)?
     *
     * @param anno the annotation to check
     * @return true if {@code anno} is {@link UnknownInitialization}
     */
    public boolean isUnknownInitialization(AnnotationMirror anno) {
        return areSameByClass(anno, UnknownInitialization.class);
    }

    /**
     * Is {@code anno} the bottom annotation?
     *
     * @param anno the annotation to check
     * @return true if {@code anno} is {@link FBCBottom}
     */
    public boolean isFbcBottom(AnnotationMirror anno) {
        return AnnotationUtils.areSame(anno, FBCBOTTOM);
    }

    /**
     * Is {@code anno} the {@link Initialized} annotation?
     *
     * @param anno the annotation to check
     * @return true if {@code anno} is {@link Initialized}
     */
    public boolean isInitialized(AnnotationMirror anno) {
        return AnnotationUtils.areSame(anno, INITIALIZED);
    }

    /**
     * Does {@code anno} have the annotation {@link UnderInitialization} (with any type frame)?
     *
     * @param anno the annotation to check
     * @return true if {@code anno} has {@link UnderInitialization}
     */
    public boolean isUnderInitialization(AnnotatedTypeMirror anno) {
        return anno.hasEffectiveAnnotation(UnderInitialization.class);
    }

    /**
     * Does {@code anno} have the annotation {@link UnknownInitialization} (with any type frame)?
     *
     * @param anno the annotation to check
     * @return true if {@code anno} has {@link UnknownInitialization}
     */
    public boolean isUnknownInitialization(AnnotatedTypeMirror anno) {
        return anno.hasEffectiveAnnotation(UnknownInitialization.class);
    }

    /**
     * Does {@code anno} have the bottom annotation?
     *
     * @param anno the annotation to check
     * @return true if {@code anno} has {@link FBCBottom}
     */
    public boolean isFbcBottom(AnnotatedTypeMirror anno) {
        return anno.hasEffectiveAnnotation(FBCBottom.class);
    }

    /**
     * Does {@code anno} have the annotation {@link Initialized}?
     *
     * @param anno the annotation to check
     * @return true if {@code anno} has {@link Initialized}
     */
    public boolean isInitialized(AnnotatedTypeMirror anno) {
        return anno.hasEffectiveAnnotation(Initialized.class);
    }

    /**
     * Return true if the type is initialized with respect to the given frame -- that is, all of the
     * fields of the frame are initialized.
     *
     * @param type the type whose initialization type qualifiers to check
     * @param frame a class in {@code type}'s class hierarchy
     * @return true if the type is initialized for the given frame
     */
    public boolean isInitializedForFrame(AnnotatedTypeMirror type, TypeMirror frame) {
        if (isInitialized(type)) {
            return true;
        }

        AnnotationMirror initializationAnno =
                type.getEffectiveAnnotationInHierarchy(UNKNOWN_INITIALIZATION);
        if (initializationAnno == null) {
            initializationAnno = type.getEffectiveAnnotationInHierarchy(UNDER_INITALIZATION);
        }

        TypeMirror typeFrame = getTypeFrameFromAnnotation(initializationAnno);
        Types types = processingEnv.getTypeUtils();
        return types.isSubtype(typeFrame, types.erasure(frame));
    }

    /**
     * Returns the type frame (that is, the argument) of a given initialization annotation.
     *
     * @param annotation a {@link UnderInitialization} or {@link UnknownInitialization} annotation
     * @return the annotation's argument
     */
    public TypeMirror getTypeFrameFromAnnotation(AnnotationMirror annotation) {
        if (AnnotationUtils.areSameByName(
                annotation,
                "org.checkerframework.checker.initialization.qual.UnderInitialization")) {
            return AnnotationUtils.getElementValue(
                    annotation,
                    underInitializationValueElement,
                    TypeMirror.class,
                    objectTypeMirror);
        } else {
            return AnnotationUtils.getElementValue(
                    annotation,
                    unknownInitializationValueElement,
                    TypeMirror.class,
                    objectTypeMirror);
        }
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new InitializationQualifierHierarchy();
    }

    /** The {@link QualifierHierarchy} for the initialization type system. */
    protected class InitializationQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

        /** Qualifier kind for the @{@link UnknownInitialization} annotation. */
        private final QualifierKind UNKNOWN_INIT;

        /** Qualifier kind for the @{@link UnderInitialization} annotation. */
        private final QualifierKind UNDER_INIT;

        /** Create an InitializationQualifierHierarchy. */
        protected InitializationQualifierHierarchy() {
            super(
                    InitializationParentAnnotatedTypeFactory.this.getSupportedTypeQualifiers(),
                    elements);
            UNKNOWN_INIT = getQualifierKind(UNKNOWN_INITIALIZATION);
            UNDER_INIT = getQualifierKind(UNDER_INITALIZATION);
        }

        @Override
        public boolean isSubtypeWithElements(
                AnnotationMirror subAnno,
                QualifierKind subKind,
                AnnotationMirror superAnno,
                QualifierKind superKind) {
            if (!subKind.isSubtypeOf(superKind)) {
                return false;
            } else if ((subKind == UNDER_INIT && superKind == UNDER_INIT)
                    || (subKind == UNDER_INIT && superKind == UNKNOWN_INIT)
                    || (subKind == UNKNOWN_INIT && superKind == UNKNOWN_INIT)) {
                // Thus, we only need to look at the type frame.
                TypeMirror frame1 = getTypeFrameFromAnnotation(subAnno);
                TypeMirror frame2 = getTypeFrameFromAnnotation(superAnno);
                return types.isSubtype(frame1, frame2);
            } else {
                return true;
            }
        }

        @Override
        protected AnnotationMirror leastUpperBoundWithElements(
                AnnotationMirror anno1,
                QualifierKind qual1,
                AnnotationMirror anno2,
                QualifierKind qual2,
                QualifierKind lubKind) {
            // Handle the case where one is a subtype of the other.
            if (isSubtypeWithElements(anno1, qual1, anno2, qual2)) {
                return anno2;
            } else if (isSubtypeWithElements(anno2, qual2, anno1, qual1)) {
                return anno1;
            }
            boolean unknowninit1 = isUnknownInitialization(anno1);
            boolean unknowninit2 = isUnknownInitialization(anno2);
            boolean underinit1 = isUnderInitialization(anno1);
            boolean underinit2 = isUnderInitialization(anno2);

            // Handle @Initialized.
            if (isInitialized(anno1)) {
                assert underinit2;
                return createUnknownInitializationAnnotation(getTypeFrameFromAnnotation(anno2));
            } else if (isInitialized(anno2)) {
                assert underinit1;
                return createUnknownInitializationAnnotation(getTypeFrameFromAnnotation(anno1));
            }

            if (underinit1 && underinit2) {
                return createUnderInitializationAnnotation(
                        lubTypeFrame(
                                getTypeFrameFromAnnotation(anno1),
                                getTypeFrameFromAnnotation(anno2)));
            }

            assert (unknowninit1 || underinit1) && (unknowninit2 || underinit2);
            return createUnknownInitializationAnnotation(
                    lubTypeFrame(
                            getTypeFrameFromAnnotation(anno1), getTypeFrameFromAnnotation(anno2)));
        }

        /**
         * Returns the least upper bound of two Java basetypes (without annotations).
         *
         * @param a the first argument
         * @param b the second argument
         * @return the lub of the two arguments
         */
        protected TypeMirror lubTypeFrame(TypeMirror a, TypeMirror b) {
            if (types.isSubtype(a, b)) {
                return b;
            } else if (types.isSubtype(b, a)) {
                return a;
            }

            return TypesUtils.leastUpperBound(a, b, processingEnv);
        }

        @Override
        protected AnnotationMirror greatestLowerBoundWithElements(
                AnnotationMirror anno1,
                QualifierKind qual1,
                AnnotationMirror anno2,
                QualifierKind qual2,
                QualifierKind glbKind) {
            // Handle the case where one is a subtype of the other.
            if (isSubtypeWithElements(anno1, qual1, anno2, qual2)) {
                return anno1;
            } else if (isSubtypeWithElements(anno2, qual2, anno1, qual1)) {
                return anno2;
            }
            boolean unknowninit1 = isUnknownInitialization(anno1);
            boolean unknowninit2 = isUnknownInitialization(anno2);
            boolean underinit1 = isUnderInitialization(anno1);
            boolean underinit2 = isUnderInitialization(anno2);

            // Handle @Initialized.
            if (isInitialized(anno1)) {
                assert underinit2;
                return FBCBOTTOM;
            } else if (isInitialized(anno2)) {
                assert underinit1;
                return FBCBOTTOM;
            }

            TypeMirror typeFrame =
                    TypesUtils.greatestLowerBound(
                            getTypeFrameFromAnnotation(anno1),
                            getTypeFrameFromAnnotation(anno2),
                            processingEnv);
            if (typeFrame.getKind() == TypeKind.ERROR
                    || typeFrame.getKind() == TypeKind.INTERSECTION) {
                return FBCBOTTOM;
            }

            if (underinit1 && underinit2) {
                return createUnderInitializationAnnotation(typeFrame);
            }

            assert (unknowninit1 || underinit1) && (unknowninit2 || underinit2);
            return createUnderInitializationAnnotation(typeFrame);
        }
    }
}

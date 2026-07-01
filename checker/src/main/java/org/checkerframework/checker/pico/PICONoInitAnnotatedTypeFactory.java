package org.checkerframework.checker.pico;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;

import org.checkerframework.checker.initialization.InitializationFieldAccessTreeAnnotator;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.PICOBottom;
import org.checkerframework.checker.pico.qual.PICOLost;
import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.DefaultFor;
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
import org.checkerframework.javacutil.TypesUtils;

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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/** AnnotatedTypeFactory for PICO. */
public class PICONoInitAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<
                PICONoInitValue, PICONoInitStore, PICONoInitTransfer, PICONoInitAnalysis> {
    /** The {@link Mutable} annotation. */
    protected final AnnotationMirror MUTABLE = AnnotationBuilder.fromClass(elements, Mutable.class);

    /** The {@link Immutable} annotation. */
    protected final AnnotationMirror IMMUTABLE =
            AnnotationBuilder.fromClass(elements, Immutable.class);

    /** The {@link Readonly} annotation. */
    protected final AnnotationMirror READONLY =
            AnnotationBuilder.fromClass(elements, Readonly.class);

    /** The {@link ReceiverDependentMutable} annotation. */
    protected final AnnotationMirror RECEIVER_DEPENDENT_MUTABLE =
            AnnotationBuilder.fromClass(elements, ReceiverDependentMutable.class);

    /** The {@link PolyMutable} annotation. */
    protected final AnnotationMirror POLY_MUTABLE =
            AnnotationBuilder.fromClass(elements, PolyMutable.class);

    /** The {@link PICOLost} annotation. */
    protected final AnnotationMirror LOST = AnnotationBuilder.fromClass(elements, PICOLost.class);

    /** The {@link PICOBottom} annotation. */
    protected final AnnotationMirror BOTTOM =
            AnnotationBuilder.fromClass(elements, PICOBottom.class);

    /** The {@link UnderInitialization} annotation. */
    protected final AnnotationMirror UNDER_INITALIZATION =
            AnnotationBuilder.fromClass(elements, UnderInitialization.class);

    /** The {@code value} element of {@link UnderInitialization}. */
    protected final ExecutableElement underInitializationValueElement =
            TreeUtils.getMethod(UnderInitialization.class, "value", 0, processingEnv);

    /**
     * Create a new PICONoInitAnnotatedTypeFactory.
     *
     * @param checker the BaseTypeChecker
     */
    @SuppressWarnings("this-escape")
    public PICONoInitAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        addAliasedTypeAnnotation(org.jmlspecs.annotation.Readonly.class, READONLY);
        postInit();
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
    protected void postDirectSuperTypes(
            AnnotatedTypeMirror type, List<? extends AnnotatedTypeMirror> supertypes) {
        AnnotationMirrorSet annotations = type.getEffectiveAnnotations();
        for (AnnotatedTypeMirror supertype : supertypes) {
            if (!annotations.equals(supertype.getEffectiveAnnotations())) {
                supertype.clearAnnotations();
                supertype.addAnnotations(annotations);
            }
        }
        if (type.getKind() == TypeKind.DECLARED) {
            for (AnnotatedTypeMirror supertype : supertypes) {
                Element elt = ((DeclaredType) supertype.getUnderlyingType()).asElement();
                addComputedTypeAnnotations(elt, supertype);
            }
        }
    }

    @Override
    public ParameterizedExecutableType constructorFromUse(NewClassTree tree) {
        ParameterizedExecutableType cType = super.constructorFromUse(tree);
        AnnotatedExecutableType constructor = cType.executableType;
        // For object creation, if the constructor return type is @RDM and there is no explicit
        // annotation on the new expression, use the default concrete creation qualifier.
        if (getExplicitNewClassAnnos(tree).isEmpty()
                && constructor.getReturnType().hasAnnotation(RECEIVER_DEPENDENT_MUTABLE)) {
            constructor.getReturnType().replaceAnnotation(MUTABLE);
        }
        return cType;
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
     * {@inheritDoc} This also defaults field types, constructor return types, and method receiver
     * types when they are obtained from elements rather than trees.
     */
    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        if (elt != null) {
            addDefaultForField(type, elt);
            defaultConstructorReturnToClassBound(elt, type);
            defaultMethodReceiverToClassBound(elt, type);
        }
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
     * {@inheritDoc} PICO defaults type declaration bounds to {@link Mutable}; special cases such as
     * implicitly immutable types, arrays, and enums are handled by {@link
     * #getTypeDeclarationBounds}.
     *
     * @return mutable default type declaration bounds
     */
    @Override
    protected AnnotationMirrorSet getDefaultTypeDeclarationBounds() {
        AnnotationMirrorSet classBoundDefault =
                new AnnotationMirrorSet(super.getDefaultTypeDeclarationBounds());
        return replaceAnnotationInHierarchy(classBoundDefault, MUTABLE);
    }

    /**
     * Determines whether {@code atm}'s underlying type is implicitly immutable according to {@link
     * Immutable}'s {@link DefaultFor} metadata.
     *
     * @param atm the type to check
     * @return true if the underlying type is implicitly immutable
     */
    public boolean isImplicitlyImmutableType(AnnotatedTypeMirror atm) {
        return isInTypeKindsOfDefaultForOfImmutable(atm) || isInTypesOfDefaultForOfImmutable(atm);
    }

    /**
     * Determines whether {@code atm}'s kind is listed in {@link Immutable}'s {@link DefaultFor}
     * metadata.
     *
     * @param atm the type to check
     * @return true if the type kind is implicitly immutable
     */
    private boolean isInTypeKindsOfDefaultForOfImmutable(AnnotatedTypeMirror atm) {
        DefaultFor defaultFor = Immutable.class.getAnnotation(DefaultFor.class);
        assert defaultFor != null;
        for (org.checkerframework.framework.qual.TypeKind typeKind : defaultFor.typeKinds()) {
            if (typeKind.name().equals(atm.getKind().name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether {@code atm}'s type is listed in {@link Immutable}'s {@link DefaultFor}
     * metadata.
     *
     * @param atm the type to check
     * @return true if the type is implicitly immutable
     */
    private boolean isInTypesOfDefaultForOfImmutable(AnnotatedTypeMirror atm) {
        if (atm.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DefaultFor defaultFor = Immutable.class.getAnnotation(DefaultFor.class);
        assert defaultFor != null;
        String fqn = TypesUtils.getQualifiedName((DeclaredType) atm.getUnderlyingType());
        for (Class<?> type : defaultFor.types()) {
            if (type.getCanonicalName().contentEquals(fqn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code type} has a valid PICO class declaration bound.
     *
     * @param type a class declaration type
     * @return true if the class bound is valid
     */
    public boolean isValidClassBound(AnnotatedTypeMirror type) {
        return type.hasAnnotation(MUTABLE)
                || type.hasAnnotation(RECEIVER_DEPENDENT_MUTABLE)
                || type.hasAnnotation(IMMUTABLE);
    }

    @Override
    public AnnotationMirrorSet getTypeDeclarationBounds(TypeMirror type) {
        // Get the mutability bound for the type declaration
        AnnotationMirror bound = getTypeDeclarationBoundForMutability(type);
        AnnotationMirrorSet classBoundDefault = super.getTypeDeclarationBounds(type);
        if (bound != null) {
            classBoundDefault = replaceAnnotationInHierarchy(classBoundDefault, bound);
        }
        return classBoundDefault;
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
     * Returns the PICO type declaration bound implied by {@code type}.
     *
     * <p>Implicitly immutable types and enum declarations without explicit {@link Mutable} or
     * {@link ReceiverDependentMutable} bounds are bounded by {@link Immutable}. Array types are
     * bounded by {@link ReceiverDependentMutable}. Other types have no special bound here and use
     * the default declaration bound.
     *
     * @param type the type whose declaration bound is being computed
     * @return the implied declaration bound, or null if {@code type} has no special PICO bound
     */
    private AnnotationMirror getTypeDeclarationBoundForMutability(TypeMirror type) {
        if (isImplicitlyImmutableType(toAnnotatedType(type, false))) {
            return IMMUTABLE;
        }
        if (type.getKind() == TypeKind.ARRAY) {
            return RECEIVER_DEPENDENT_MUTABLE;
        }
        if (type instanceof DeclaredType) {
            Element ele = ((DeclaredType) type).asElement();
            if (ele.getKind() == ElementKind.ENUM) {
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

    /**
     * Returns the class bound of the type declaration enclosing {@code node}.
     *
     * @param node tree whose enclosing type declaration bound is needed
     * @return the enclosing type declaration bound, or null if no enclosing type was found
     */
    public AnnotatedTypeMirror getBoundTypeOfEnclosingTypeDeclaration(Tree node) {
        TypeElement typeElement = null;
        if (node instanceof MethodTree) {
            ExecutableElement element = TreeUtils.elementFromDeclaration((MethodTree) node);
            typeElement = ElementUtils.enclosingTypeElement(element);
        } else if (node instanceof VariableTree) {
            VariableElement variableElement = TreeUtils.elementFromDeclaration((VariableTree) node);
            assert variableElement != null && variableElement.getKind().isField();
            typeElement = ElementUtils.enclosingTypeElement(variableElement);
        }

        if (typeElement != null) {
            return getAnnotatedType(typeElement);
        }
        return null;
    }

    /**
     * Returns the class bound of {@code element}'s enclosing type declaration.
     *
     * @param element element whose enclosing type declaration bound is needed
     * @return the enclosing type declaration bound, or null if no enclosing type was found
     */
    public AnnotatedTypeMirror getBoundTypeOfEnclosingClass(Element element) {
        TypeElement typeElement = ElementUtils.enclosingTypeElement(element);
        if (typeElement != null) {
            return getAnnotatedType(typeElement);
        }
        return null;
    }

    /**
     * Determines whether {@code annotatedTypeMirror} represents an enum declaration or enum
     * constant.
     *
     * @param annotatedTypeMirror the type to check
     * @return true if the type is enum-related
     */
    private boolean isEnumOrEnumConstant(AnnotatedTypeMirror annotatedTypeMirror) {
        Element element =
                ((AnnotatedTypeMirror.AnnotatedDeclaredType) annotatedTypeMirror)
                        .getUnderlyingType()
                        .asElement();
        return element != null
                && (element.getKind() == ElementKind.ENUM_CONSTANT
                        || element.getKind() == ElementKind.ENUM);
    }

    /**
     * Determines whether {@code variableElement} is final.
     *
     * @param variableElement the field element
     * @return true if the field is final
     */
    public boolean isFinalField(Element variableElement) {
        assert variableElement instanceof VariableElement;
        return ElementUtils.isFinal(variableElement);
    }

    /**
     * Determines whether {@code variableElement} is assignable. Static non-final fields and fields
     * explicitly annotated with {@link Assignable} are assignable.
     *
     * @param variableElement the field element
     * @return true if the field is assignable
     */
    public boolean isAssignableField(Element variableElement) {
        if (!(variableElement instanceof VariableElement)) {
            return false;
        }
        boolean hasExplicitAssignableAnnotation =
                getDeclAnnotation(variableElement, Assignable.class) != null;
        if (!ElementUtils.isStatic(variableElement)) {
            return hasExplicitAssignableAnnotation;
        }
        return hasExplicitAssignableAnnotation || !isFinalField(variableElement);
    }

    /**
     * Determines whether {@code variableElement} is receiver-dependent assignable.
     *
     * @param variableElement the field element
     * @return true if the field is receiver-dependent assignable
     */
    public boolean isReceiverDependentAssignable(Element variableElement) {
        assert variableElement instanceof VariableElement;
        if (ElementUtils.isStatic(variableElement)) {
            return false;
        }
        return !isAssignableField(variableElement) && !isFinalField(variableElement);
    }

    /**
     * Checks that {@code field} has exactly one assignability status: explicitly assignable, final,
     * or receiver-dependent assignable.
     *
     * @param field the field element
     * @return true if the field has exactly one assignability status
     */
    public boolean hasOneAndOnlyOneAssignabilityQualifier(VariableElement field) {
        if (isAssignableField(field)
                && !isFinalField(field)
                && !isReceiverDependentAssignable(field)) {
            return true;
        } else if (!isAssignableField(field)
                && isFinalField(field)
                && !isReceiverDependentAssignable(field)) {
            return true;
        } else if (!isAssignableField(field)
                && !isFinalField(field)
                && isReceiverDependentAssignable(field)) {
            assert !ElementUtils.isStatic(field);
            return true;
        }
        return false;
    }

    /**
     * Determines whether {@code tree} selects an assignable field.
     *
     * @param tree the field selection tree
     * @return true if the selected field is assignable
     */
    public boolean isAssigningAssignableField(ExpressionTree tree) {
        Element fieldElement = TreeUtils.elementFromUse(tree);
        return fieldElement != null && isAssignableField(fieldElement);
    }

    /**
     * Determines whether {@code type} is javac's synthetic array class.
     *
     * @param type the declared type to check
     * @return true if {@code type} is the synthetic array class
     */
    private boolean isArrayType(AnnotatedTypeMirror.AnnotatedDeclaredType type) {
        Element ele = getProcessingEnv().getTypeUtils().asElement(type.getUnderlyingType());

        // If it is a user-declared "Array" class without package, a class / source file should be
        // there. Otherwise, it is the java inner type.
        return ele instanceof Symbol.ClassSymbol
                && ElementUtils.getQualifiedName(ele).contentEquals("Array")
                && ((Symbol.ClassSymbol) ele).classfile == null
                && ((Symbol.ClassSymbol) ele).sourcefile == null;
    }

    @Override
    public AnnotatedTypeMirror getTypeOfExtendsImplements(Tree clause) {
        // Allow concrete class bounds to satisfy @RDM extends/implements clauses by adapting the
        // clause to the enclosing class's bound.
        AnnotatedTypeMirror fromTypeTree = super.getTypeOfExtendsImplements(clause);
        if (fromTypeTree.hasAnnotation(RECEIVER_DEPENDENT_MUTABLE)) {
            ClassTree enclosingClass = TreePathUtil.enclosingClass(getPath(clause));
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
     * @param annotatedTypeMirror the annotated type mirror to add default annotation
     * @param element the element to add default annotation
     */
    private void addDefaultForField(AnnotatedTypeMirror annotatedTypeMirror, Element element) {
        if (element != null && element.getKind() == ElementKind.FIELD) {
            // If the field is static, apply the default concrete qualifier if there is no explicit
            // annotation and the field type declaration bound is @RDM.
            if (ElementUtils.isStatic(element)) {
                AnnotatedTypeMirror explicitATM = fromElement(element);
                AnnotationMirrorSet declBound = getTypeDeclarationBounds(element.asType());
                if (!explicitATM.hasAnnotationInHierarchy(READONLY)
                        && AnnotationUtils.containsSameByName(
                                declBound, RECEIVER_DEPENDENT_MUTABLE)) {
                    annotatedTypeMirror.replaceAnnotation(MUTABLE);
                }
            } else {
                // Apply default annotation to instance fields if there is no explicit annotation
                AnnotatedTypeMirror explicitATM = fromElement(element);
                if (!explicitATM.hasAnnotationInHierarchy(READONLY)) {
                    if (explicitATM instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
                        AnnotatedTypeMirror.AnnotatedDeclaredType adt =
                                (AnnotatedTypeMirror.AnnotatedDeclaredType) explicitATM;
                        Element typeElement = adt.getUnderlyingType().asElement();
                        // If the declaration bound is RDM, replace the annotation as RDM
                        if (typeElement instanceof TypeElement) {
                            AnnotatedTypeMirror bound = getAnnotatedType(typeElement);
                            if (bound.hasAnnotation(RECEIVER_DEPENDENT_MUTABLE)) {
                                annotatedTypeMirror.replaceAnnotation(RECEIVER_DEPENDENT_MUTABLE);
                            }
                        }
                    } else if (explicitATM instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
                        // If the ATM is array type, replace array type with @RDM.
                        annotatedTypeMirror.replaceAnnotation(RECEIVER_DEPENDENT_MUTABLE);
                    }
                }
            }
        }
    }

    /**
     * Add class bound declaration to constructor return type if it doesn't have explicit
     * annotation.
     *
     * <p>For @Immutable class bound, add @Immutable annotation to constructor return type. For @RDM
     * class bound, add @RDM annotation to constructor return type. For @Mutable class bound,
     * add @Mutable annotation to constructor return type.
     *
     * @param element the element to add default annotation
     * @param type the type to add default annotation
     */
    private void defaultConstructorReturnToClassBound(Element element, AnnotatedTypeMirror type) {
        if (element.getKind() == ElementKind.CONSTRUCTOR
                && type instanceof AnnotatedExecutableType) {
            AnnotatedTypeMirror bound = getBoundTypeOfEnclosingClass(element);
            assert bound != null;
            ((AnnotatedExecutableType) type)
                    .getReturnType()
                    .addMissingAnnotations(
                            Collections.singletonList(bound.getAnnotationInHierarchy(READONLY)));
        }
    }

    /**
     * Add class bound declaration to method receiver type if it doesn't have explicit annotation.
     *
     * <p>For @Immutable class bound, add @Immutable annotation to method receiver type. For @RDM
     * class bound, add @RDM annotation to method receiver type. For @Mutable class bound,
     * add @Mutable annotation to method receiver type.
     *
     * @param element the element to add default annotation
     * @param type the type to add default annotation
     */
    private void defaultMethodReceiverToClassBound(Element element, AnnotatedTypeMirror type) {
        if (element.getKind() == ElementKind.METHOD
                && type instanceof AnnotatedExecutableType
                && !ElementUtils.isStatic(element)) {
            AnnotatedTypeMirror bound = getBoundTypeOfEnclosingClass(element);
            AnnotatedExecutableType methodType = (AnnotatedExecutableType) type;
            assert bound != null;
            methodType
                    .getReceiverType()
                    .addMissingAnnotations(
                            Collections.singletonList(bound.getAnnotationInHierarchy(READONLY)));
        }
    }

    /** Tree annotators for PICO-specific defaults and expression annotations. */
    public static class PICOPropagationTreeAnnotator extends PropagationTreeAnnotator {
        /** The PICO type factory. */
        private final PICONoInitAnnotatedTypeFactory picoTypeFactory;

        /**
         * Create a new PICOPropagationTreeAnnotator.
         *
         * @param atypeFactory the type factory
         */
        public PICOPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
            picoTypeFactory = (PICONoInitAnnotatedTypeFactory) atypeFactory;
        }

        @Override
        public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
            boolean hasExplicitAnnos = !type.getAnnotations().isEmpty();
            super.visitTypeCast(node, type);
            if (!hasExplicitAnnos
                    && type.hasAnnotation(picoTypeFactory.RECEIVER_DEPENDENT_MUTABLE)) {
                type.replaceAnnotation(picoTypeFactory.MUTABLE);
            }
            return null;
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            return null;
        }
    }

    /** Applies PICO-specific tree defaults. */
    public static class PICOTreeAnnotator extends TreeAnnotator {
        /** The PICO type factory. */
        private final PICONoInitAnnotatedTypeFactory picoTypeFactory;

        /**
         * Create a new PICOTreeAnnotator.
         *
         * @param atypeFactory the type factory
         */
        public PICOTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
            picoTypeFactory = (PICONoInitAnnotatedTypeFactory) atypeFactory;
        }

        @Override
        public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror componentType =
                    ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType();
            super.visitNewArray(tree, type);
            // If the array component type from treeAnnotator is @RDM, replace it with the default
            // concrete creation qualifier. This does not change explicitly annotated component
            // types.
            if (componentType.hasAnnotation(picoTypeFactory.RECEIVER_DEPENDENT_MUTABLE)) {
                componentType.replaceAnnotation(picoTypeFactory.MUTABLE);
            }
            return null;
        }

        /**
         * {@inheritDoc} This defaults constructor return and method receiver types to the enclosing
         * class bound when the executable is accessed as a tree.
         */
        @Override
        public Void visitMethod(MethodTree tree, AnnotatedTypeMirror p) {
            Element element = TreeUtils.elementFromDeclaration(tree);
            picoTypeFactory.defaultConstructorReturnToClassBound(element, p);
            picoTypeFactory.defaultMethodReceiverToClassBound(element, p);
            return super.visitMethod(tree, p);
        }

        /** {@inheritDoc} This covers the declaration of static fields */
        @Override
        public Void visitVariable(VariableTree tree, AnnotatedTypeMirror annotatedTypeMirror) {
            VariableElement element = TreeUtils.elementFromDeclaration(tree);
            picoTypeFactory.addDefaultForField(annotatedTypeMirror, element);
            return super.visitVariable(tree, annotatedTypeMirror);
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            type.replaceAnnotation(picoTypeFactory.IMMUTABLE);
            return null;
        }
    }

    /** Type annotators for PICO-specific defaults. */
    public static class PICOTypeAnnotator extends TypeAnnotator {
        /** The PICO type factory. */
        private final PICONoInitAnnotatedTypeFactory picoTypeFactory;

        /**
         * Create a new PICOTypeAnnotator.
         *
         * @param typeFactory the type factory
         */
        public PICOTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
            picoTypeFactory = (PICONoInitAnnotatedTypeFactory) typeFactory;
        }

        /** {@inheritDoc} Applies PICO-specific defaults. */
        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            super.visitExecutable(t, p);

            // Array decl methods
            // Array methods are implemented as JVM native method, so we cannot add that to stubs.
            // for now: default array in receiver, parameter and return type to RDM
            if (t.getReceiverType() != null) {
                if (picoTypeFactory.isArrayType(t.getReceiverType())) {
                    if (t.toString().equals("Object clone(Array this)")) {
                        // Receiver type will not be viewpoint adapted:
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
        private final PICONoInitAnnotatedTypeFactory picoTypeFactory;

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

            if (!type.getTypeArguments().isEmpty()) {
                // Only declared types with type arguments might be recursive.
                if (hasVisited(type)) {
                    return getVisited(type);
                }
                markVisited(type, null);
            }
            Void r = null;
            if (type.getEnclosingType() != null) {
                scan(type.getEnclosingType(), null);
            }
            r = scanAndReduce(type.getTypeArguments(), null, r);
            return r;
        }
    }

    /** Applies PICO defaults derived from {@link DefaultFor}. */
    public static class PICODefaultForTypeAnnotator extends DefaultForTypeAnnotator {

        /**
         * Create a new PICODefaultForTypeAnnotator.
         *
         * @param typeFactory the type factory
         */
        public PICODefaultForTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        /** Also applies implicit defaults to method receivers. */
        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            super.visitExecutable(t, p);
            // Also scan the receiver to apply implicit annotation
            if (t.getReceiverType() != null) {
                return scanAndReduce(t.getReceiverType(), p, null);
            }
            return null;
        }
    }

    /**
     * Defaults enum declarations to immutable without changing other class declaration defaults.
     */
    public static class PICOEnumDefaultAnnotator extends TypeAnnotator {
        /** The PICO type factory. */
        private final PICONoInitAnnotatedTypeFactory picoTypeFactory;

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
            if (picoTypeFactory.isEnumOrEnumConstant(type)) {
                type.addMissingAnnotations(Collections.singleton(picoTypeFactory.IMMUTABLE));
            }
            return super.visitDeclared(type, aVoid);
        }
    }
}

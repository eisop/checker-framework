package org.checkerframework.checker.initialization;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

import org.checkerframework.checker.initialization.qual.FBCBottom;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.qual.Unused;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * The annotated type factory for the freedom-before-commitment type-system. The
 * freedom-before-commitment type-system and this class are abstract and need to be combined with
 * another type-system whose safe initialization should be tracked. For an example, see the {@link
 * NullnessChecker}.
 */
public class InitializationAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<
                InitializationValue,
                InitializationStore,
                InitializationTransfer,
                InitializationAnalysis> {

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

    /** Cache for the initialization annotations. */
    protected final Set<Class<? extends Annotation>> initAnnos;

    protected final Map<Tree, List<VariableTree>> uninitializedFields = new HashMap<>();

    /**
     * String representation of all initialization annotations.
     *
     * <p>{@link UnknownInitialization} {@link UnderInitialization} {@link Initialized} {@link
     * FBCBottom}
     *
     * <p>This is used to quickly check of an AnnotationMirror is one of the initialization
     * annotations without having to repeatedly convert them to strings.
     */
    protected final Set<String> initAnnoNames;

    /**
     * Create a new InitializationAnnotatedTypeFactory.
     *
     * @param checker the checker to which the new type factory belongs
     */
    public InitializationAnnotatedTypeFactory(BaseTypeChecker checker) {
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

        Set<Class<? extends Annotation>> tempInitAnnos = new LinkedHashSet<>(4);
        tempInitAnnos.add(UnderInitialization.class);
        tempInitAnnos.add(Initialized.class);
        tempInitAnnos.add(UnknownInitialization.class);
        tempInitAnnos.add(FBCBottom.class);

        initAnnos = Collections.unmodifiableSet(tempInitAnnos);

        Set<String> tempInitAnnoNames = new HashSet<>(4);
        tempInitAnnoNames.add(AnnotationUtils.annotationName(UNKNOWN_INITIALIZATION));
        tempInitAnnoNames.add(AnnotationUtils.annotationName(UNDER_INITALIZATION));
        tempInitAnnoNames.add(AnnotationUtils.annotationName(INITIALIZED));
        tempInitAnnoNames.add(AnnotationUtils.annotationName(FBCBOTTOM));

        initAnnoNames = Collections.unmodifiableSet(tempInitAnnoNames);

        postInit();
    }

    @Override
    public InitializationChecker getChecker() {
        return (InitializationChecker) super.getChecker();
    }

    public Set<Class<? extends Annotation>> getInitializationAnnotations() {
        return initAnnos;
    }

    /**
     * Is the annotation {@code anno} an initialization qualifier?
     *
     * @param anno the annotation to check
     * @return true if the argument is an initialization qualifier
     */
    protected boolean isInitializationAnnotation(AnnotationMirror anno) {
        assert anno != null;
        return initAnnoNames.contains(AnnotationUtils.annotationName(anno));
    }

    /*
     * The following method can be used to appropriately configure the
     * commitment type-system.
     */

    /**
     * Returns the list of annotations that is forbidden for the constructor return type.
     *
     * @return the list of annotations that is forbidden for the constructor return type
     */
    public Set<Class<? extends Annotation>> getInvalidConstructorReturnTypeAnnotations() {
        return getInitializationAnnotations();
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
     * Are all fields initialized-only?
     *
     * @param classTree the class to query
     * @return true if all fields are initialized-only
     */
    protected boolean areAllFieldsInitializedOnly(ClassTree classTree) {
        for (Tree member : classTree.getMembers()) {
            if (member.getKind() != Tree.Kind.VARIABLE) {
                continue;
            }
            VariableTree var = (VariableTree) member;
            VariableElement varElt = TreeUtils.elementFromDeclaration(var);
            // var is not initialized-only
            if (getDeclAnnotation(varElt, NotOnlyInitialized.class) != null) {
                // var is not static -- need a check of initializer blocks,
                // not of constructor which is where this is used
                if (!varElt.getModifiers().contains(Modifier.STATIC)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void postAsMemberOf(
            AnnotatedTypeMirror type, AnnotatedTypeMirror owner, Element element) {
        super.postAsMemberOf(type, owner, element);

        if (element.getKind().isField()) {
            Collection<? extends AnnotationMirror> declaredFieldAnnotations =
                    getDeclAnnotations(element);
            AnnotatedTypeMirror fieldAnnotations = getAnnotatedType(element);
            computeFieldAccessInitializationType(
                    type, declaredFieldAnnotations, owner, fieldAnnotations);
        }
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

        // If all fields are initialized-only, and they are all initialized,
        // then:
        //  - if the class is final, this is @Initialized
        //  - otherwise, this is @UnderInitialization(CurrentClass) as
        //    there might still be subclasses that need initialization.
        if (areAllFieldsInitializedOnly(enclosingClass)) {
            InitializationStore store = getStoreBefore(tree);
            if (store != null
                    && getUninitializedFields(store, path, false, Collections.emptyList())
                            .isEmpty()) {
                if (classType.isFinal()) {
                    annotation = INITIALIZED;
                } else {
                    annotation = createUnderInitializationAnnotation(classType);
                }
            }
        }

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

    /**
     * Returns the fields that are not yet initialized in a given store.
     *
     * @param store a store
     * @param path the current path, used to determine the current class
     * @param isStatic whether to report static fields or instance fields
     * @param receiverAnnotations the annotations on the receiver
     * @return the fields that are not yet initialized in a given store
     */
    public List<VariableTree> getUninitializedFields(
            InitializationStore store,
            TreePath path,
            boolean isStatic,
            Collection<? extends AnnotationMirror> receiverAnnotations) {
        ClassTree currentClass = TreePathUtil.enclosingClass(path);
        List<VariableTree> fields = InitializationChecker.getAllFields(currentClass);
        List<VariableTree> uninit = new ArrayList<>();
        for (VariableTree field : fields) {
            if (isUnused(field, receiverAnnotations)) {
                continue; // don't consider unused fields
            }
            VariableElement fieldElem = TreeUtils.elementFromDeclaration(field);
            if (ElementUtils.isStatic(fieldElem) == isStatic) {
                if (!store.isFieldInitialized(fieldElem)) {
                    uninit.add(field);
                }
            }
        }
        return uninit;
    }

    /**
     * Reports an error for every field not initialized by a given constructor or static
     * initializer.
     *
     * @param <Value> the parent factory's value type
     * @param <Store> the parent factory's store type
     * @param <Transfer> the parent factory's transfer type
     * @param <Analysis> the parent factory's analysis type
     * @param <Factory> the parent factory type
     * @param node the constructor or static initializer to check
     * @param staticFields whether or not to check for initialization of static fields
     * @param factory the parent factory
     * @param enclosingClass the enclosing class for {@code node}
     * @param invariant the invariant annotation
     * @param filter a predicate which is false if the field should not be checked for
     *     initialization
     */
    public <
                    Value extends CFAbstractValue<Value>,
                    Store extends CFAbstractStore<Value, Store>,
                    Transfer extends CFAbstractTransfer<Value, Store, Transfer>,
                    Analysis extends CFAbstractAnalysis<Value, Store, Transfer>,
                    Factory extends GenericAnnotatedTypeFactory<Value, Store, Transfer, Analysis>>
            void reportUninitializedFields(
                    Tree node,
                    boolean staticFields,
                    Factory factory,
                    ClassTree enclosingClass,
                    AnnotationMirror invariant,
                    Predicate<VariableTree> filter) {
        reportUninitializedFields(
                node,
                staticFields,
                filter.and(
                        var -> {
                            // filter out variables that have the invariant
                            Store store = factory.getRegularExitStore(node);
                            Node receiver;
                            if (ElementUtils.isStatic(TreeUtils.elementFromDeclaration(var))) {
                                receiver = new ClassNameNode(enclosingClass);
                            } else {
                                receiver =
                                        new ImplicitThisNode(
                                                TreeUtils.elementFromDeclaration(enclosingClass)
                                                        .asType());
                            }

                            Value value =
                                    store.getValue(
                                            new FieldAccessNode(
                                                    var,
                                                    TreeUtils.elementFromDeclaration(var),
                                                    receiver));
                            if (value != null) {
                                Set<AnnotationMirror> annotations = value.getAnnotations();
                                for (AnnotationMirror annotation : annotations) {
                                    if (factory.getQualifierHierarchy()
                                            .isSubtype(annotation, invariant)) {
                                        return false;
                                    }
                                }
                            }

                            // filter out variables whose declaration doesn't have the invariant
                            AnnotatedTypeMirror declType = factory.getAnnotatedTypeLhs(var);
                            if (!AnnotationUtils.containsSame(
                                    declType.getAnnotations(), invariant)) {
                                return false;
                            }

                            return true;
                        }));
    }

    /**
     * Reports an error for every field not initialized by a given constructor or static
     * initializer.
     *
     * @param node the constructor or static initializer to check
     * @param staticFields whether or not to check for initialization of static fields
     * @param filter a predicate which is false if the field should not be checked for
     *     initialization
     */
    public void reportUninitializedFields(
            Tree node, boolean staticFields, Predicate<VariableTree> filter) {
        List<VariableTree> uninitializedFields = this.uninitializedFields.get(node);

        if (uninitializedFields == null || uninitializedFields.isEmpty()) {
            return;
        }

        // Errors are issued at the field declaration if the field is static or if the constructor
        // is the default constructor.
        // Errors are issued at the constructor declaration if the field is non-static and the
        // constructor is non-default.
        boolean errorAtField = staticFields || TreeUtils.isSynthetic((MethodTree) node);

        String FIELDS_UNINITIALIZED_KEY =
                (staticFields
                        ? "initialization.static.field.uninitialized"
                        : errorAtField
                                ? "initialization.field.uninitialized"
                                : "initialization.fields.uninitialized");

        // Remove fields with a relevant @SuppressWarnings annotation.
        uninitializedFields.removeIf(
                f ->
                        checker.shouldSuppressWarnings(
                                TreeUtils.elementFromDeclaration(f), FIELDS_UNINITIALIZED_KEY));

        uninitializedFields.removeIf(filter.negate());

        if (!uninitializedFields.isEmpty()) {
            if (errorAtField) {
                // Issue each error at the relevant field
                for (VariableTree f : uninitializedFields) {
                    checker.reportError(f, FIELDS_UNINITIALIZED_KEY, f.getName());
                }
            } else {
                // Issue all the errors at the relevant constructor
                StringJoiner fieldsString = new StringJoiner(", ");
                for (VariableTree f : uninitializedFields) {
                    fieldsString.add(f.getName());
                }
                checker.reportError(node, FIELDS_UNINITIALIZED_KEY, fieldsString);
            }
        }
    }

    /**
     * Returns the fields are initialized in a given store.
     *
     * @param store a store
     * @param path the current path; used to compute the current class
     * @return the fields that are initialized in a given store
     */
    public List<VariableTree> getInitializedFields(InitializationStore store, TreePath path) {
        // TODO: Instead of passing the TreePath around, can we use
        // getCurrentClassTree?
        ClassTree currentClass = TreePathUtil.enclosingClass(path);
        List<VariableTree> fields = InitializationChecker.getAllFields(currentClass);
        List<VariableTree> initializedFields = new ArrayList<>();
        for (VariableTree field : fields) {
            VariableElement fieldElem = TreeUtils.elementFromDeclaration(field);
            if (!ElementUtils.isStatic(fieldElem)) {
                if (store.isFieldInitialized(fieldElem)) {
                    initializedFields.add(field);
                }
            }
        }
        return initializedFields;
    }

    /** Returns whether the field {@code f} is unused, given the annotations on the receiver. */
    private boolean isUnused(
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
     * Return true if the type is initialized with respect to the given frame -- that is, all of the
     * fields of the frame are initialized.
     *
     * @param type the type whose initialization type qualifiers to check
     * @param frame a class in {@code type}'s class hierarchy
     * @return true if the type is initialized for the given frame
     */
    public boolean isInitializedForFrame(AnnotatedTypeMirror type, TypeMirror frame) {
        AnnotationMirror initializationAnno =
                type.getEffectiveAnnotationInHierarchy(UNKNOWN_INITIALIZATION);
        TypeMirror typeFrame = getTypeFrameFromAnnotation(initializationAnno);
        Types types = processingEnv.getTypeUtils();
        return types.isSubtype(typeFrame, types.erasure(frame));
    }

    /**
     * Determine the initialization type of a field access (implicit or explicit) based on the
     * receiver type and the declared annotations for the field.
     *
     * @param type type of the field access expression
     * @param declaredFieldAnnotations declared annotations on the field
     * @param receiverType inferred annotations of the receiver
     * @param fieldType inferred annotations of the field
     */
    private void computeFieldAccessInitializationType(
            AnnotatedTypeMirror type,
            Collection<? extends AnnotationMirror> declaredFieldAnnotations,
            AnnotatedTypeMirror receiverType,
            AnnotatedTypeMirror fieldType) {
        // not necessary for primitive fields
        if (TypesUtils.isPrimitive(type.getUnderlyingType())) {
            return;
        }
        // not necessary if there is an explicit UnknownInitialization
        // annotation on the field
        if (AnnotationUtils.containsSameByName(
                fieldType.getAnnotations(), UNKNOWN_INITIALIZATION)) {
            return;
        }
        if (isUnknownInitialization(receiverType) || isUnderInitialization(receiverType)) {
            if (AnnotationUtils.containsSame(declaredFieldAnnotations, NOT_ONLY_INITIALIZED)) {
                type.replaceAnnotation(UNKNOWN_INITIALIZATION);
            } else {
                type.replaceAnnotation(INITIALIZED);
            }

            if (!AnnotationUtils.containsSame(declaredFieldAnnotations, NOT_ONLY_INITIALIZED)) {
                // add root annotation for all other hierarchies, and
                // Initialized for the initialization hierarchy
                type.replaceAnnotation(INITIALIZED);
            }
        }
    }

    @Override
    public boolean isNotFullyInitializedReceiver(MethodTree methodTree) {
        if (super.isNotFullyInitializedReceiver(methodTree)) {
            return true;
        }
        final AnnotatedDeclaredType receiverType =
                analysis.getTypeFactory().getAnnotatedType(methodTree).getReceiverType();
        if (receiverType != null) {
            return isUnknownInitialization(receiverType) || isUnderInitialization(receiverType);
        } else {
            // There is no receiver e.g. in static methods.
            return false;
        }
    }

    @Override
    protected InitializationAnalysis createFlowAnalysis() {
        return new InitializationAnalysis(checker, this);
    }

    @Override
    public InitializationTransfer createFlowTransferFunction(
            CFAbstractAnalysis<InitializationValue, InitializationStore, InitializationTransfer>
                    analysis) {
        return new InitializationTransfer((InitializationAnalysis) analysis);
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new InitializationQualifierHierarchy();
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(
                super.createTypeAnnotator(), new CommitmentTypeAnnotator(this));
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        // Don't call super.createTreeAnnotator because we want our CommitmentTreeAnnotator
        // instead of the default PropagationTreeAnnotator
        List<TreeAnnotator> treeAnnotators = new ArrayList<>(2);
        treeAnnotators.add(new LiteralTreeAnnotator(this).addStandardLiteralQualifiers());
        if (dependentTypesHelper.hasDependentAnnotations()) {
            treeAnnotators.add(dependentTypesHelper.createDependentTypesTreeAnnotator());
        }
        treeAnnotators.add(new CommitmentTreeAnnotator(this));
        return new ListTreeAnnotator(treeAnnotators);
    }

    protected class CommitmentTypeAnnotator extends TypeAnnotator {
        public CommitmentTypeAnnotator(InitializationAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            Void result = super.visitExecutable(t, p);
            Element elem = t.getElement();
            if (elem.getKind() == ElementKind.CONSTRUCTOR) {
                AnnotatedDeclaredType returnType = (AnnotatedDeclaredType) t.getReturnType();
                DeclaredType underlyingType = returnType.getUnderlyingType();
                returnType.replaceAnnotation(
                        getUnderInitializationAnnotationOfSuperType(underlyingType));
            }
            return result;
        }
    }

    /**
     * This annotator should be added to {@link GenericAnnotatedTypeFactory#createTreeAnnotator} for
     * this initialization checker's parent checker. It ensures that the fields of an uninitialized
     * receiver have the top type in the parent checker's hierarchy.
     */
    public static class CommitmentFieldAccessTreeAnnotator extends TreeAnnotator {

        /**
         * Creates a new CommitmentFieldAccessTreeAnnotator.
         *
         * @param atypeFactory the type factory belonging to the init checker's parent.
         */
        public CommitmentFieldAccessTreeAnnotator(
                GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, AnnotatedTypeMirror p) {
            super.visitIdentifier(node, p);
            computeFieldAccessType(node, p);
            return null;
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror p) {
            super.visitMemberSelect(node, p);
            computeFieldAccessType(node, p);
            return null;
        }

        private void computeFieldAccessType(ExpressionTree node, AnnotatedTypeMirror type) {
            GenericAnnotatedTypeFactory<?, ?, ?, ?> factory =
                    (GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory;
            InitializationAnnotatedTypeFactory initFactory =
                    factory.getChecker().getTypeFactoryOfSubchecker(InitializationChecker.class);
            Element element = TreeUtils.elementFromUse(node);
            AnnotatedTypeMirror owner = initFactory.getReceiverType(node);

            if (owner == null) {
                return;
            }

            if (type instanceof AnnotatedExecutableType) {
                return;
            }

            AnnotatedTypeMirror fieldAnnotations = factory.getAnnotatedType(element);

            // not necessary for primitive fields
            if (TypesUtils.isPrimitive(type.getUnderlyingType())) {
                return;
            }
            // not necessary if there is an explicit UnknownInitialization
            // annotation on the field
            if (AnnotationUtils.containsSameByName(
                    fieldAnnotations.getAnnotations(), initFactory.UNKNOWN_INITIALIZATION)) {
                return;
            }
            if (initFactory.isUnknownInitialization(owner)
                    || initFactory.isUnderInitialization(owner)) {

                TypeMirror fieldDeclarationType = element.getEnclosingElement().asType();
                boolean isOwnerInitialized =
                        initFactory.isInitializedForFrame(owner, fieldDeclarationType);

                Tree declaration =
                        initFactory.declarationFromElement(TreeUtils.elementFromTree(node));

                boolean isFieldInitialized =
                        TreeUtils.isSelfAccess(node)
                                && initFactory
                                        .getInitializedFields(
                                                initFactory.getStoreBefore(node),
                                                initFactory.getPath(node))
                                        .contains(declaration);
                if (!isOwnerInitialized
                        && !isFieldInitialized
                        && !factory.isComputingAnnotatedTypeMirrorOfLHS()) {
                    // The receiver is not initialized for this frame and the type being computed is
                    // not
                    // a LHS.
                    // Replace all annotations with the top annotation for that hierarchy.
                    type.clearAnnotations();
                    type.addAnnotations(factory.getQualifierHierarchy().getTopAnnotations());
                }
            }
        }
    }

    protected class CommitmentTreeAnnotator extends PropagationTreeAnnotator {

        public CommitmentTreeAnnotator(InitializationAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitMethod(MethodTree node, AnnotatedTypeMirror p) {
            Void result = super.visitMethod(node, p);
            if (TreeUtils.isConstructor(node)) {
                assert p instanceof AnnotatedExecutableType;
                AnnotatedExecutableType exeType = (AnnotatedExecutableType) p;
                DeclaredType underlyingType =
                        (DeclaredType) exeType.getReturnType().getUnderlyingType();
                AnnotationMirror a = getUnderInitializationAnnotationOfSuperType(underlyingType);
                exeType.getReturnType().replaceAnnotation(a);
            }
            return result;
        }

        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror p) {
            super.visitNewClass(node, p);
            boolean allInitialized = true;
            Type type = ((JCTree) node).type;
            for (ExpressionTree a : node.getArguments()) {
                final AnnotatedTypeMirror t = getAnnotatedType(a);
                allInitialized &= (isInitialized(t) || isFbcBottom(t));
            }
            if (!allInitialized) {
                p.replaceAnnotation(createUnderInitializationAnnotation(type));
                return null;
            }
            p.replaceAnnotation(INITIALIZED);
            return null;
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (tree.getKind() != Tree.Kind.NULL_LITERAL) {
                type.addAnnotation(INITIALIZED);
            }
            return super.visitLiteral(tree, type);
        }

        @Override
        public Void visitMemberSelect(
                MemberSelectTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            if (TreeUtils.isArrayLengthAccess(node)) {
                annotatedTypeMirror.replaceAnnotation(INITIALIZED);
            }
            return super.visitMemberSelect(node, annotatedTypeMirror);
        }

        /* The result of a binary or unary operator is always @Initialized. */
        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            return null;
        }

        @Override
        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
            return null;
        }
    }

    /** The {@link QualifierHierarchy} for the initialization type system. */
    protected class InitializationQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

        /** Qualifier kind for the @{@link UnknownInitialization} annotation. */
        private final QualifierKind UNKNOWN_INIT;
        /** Qualifier kind for the @{@link UnderInitialization} annotation. */
        private final QualifierKind UNDER_INIT;

        /** Create an InitializationQualifierHierarchy. */
        protected InitializationQualifierHierarchy() {
            super(InitializationAnnotatedTypeFactory.this.getSupportedTypeQualifiers(), elements);
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
            if (!isInitializationAnnotation(anno1) || !isInitializationAnnotation(anno2)) {
                return null;
            }

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
         * Returns the least upper bound of two types.
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
            if (!isInitializationAnnotation(anno1) || !isInitializationAnnotation(anno2)) {
                return null;
            }

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

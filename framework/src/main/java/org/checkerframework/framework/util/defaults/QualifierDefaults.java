package org.checkerframework.framework.util.defaults;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.StringsPlume;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;

/**
 * Determines the default qualifiers on a type. Default qualifiers are specified via the {@link
 * org.checkerframework.framework.qual.DefaultQualifier} annotation.
 *
 * <p>Type variable uses have two possible defaults. If flow sensitive type refinement is enabled,
 * unannotated top-level type variable uses receive the same default as local variables. All other
 * type variable uses are defaulted using the {@code TYPE_VARIABLE_USE} default.
 *
 * <pre>{@code
 * <T> void method(USE T tIn) {
 *     LOCAL T t = tIn;
 * }
 * }</pre>
 *
 * The parameter {@code tIn} will be defaulted using the {@code TYPE_VARIABLE_USE} default. The
 * local variable {@code t} will be defaulted using the {@code LOCAL_VARIABLE} default, in order to
 * allow dataflow to refine {@code T}.
 *
 * @see org.checkerframework.framework.qual.DefaultQualifier
 */
public class QualifierDefaults {

    // TODO add visitor state to get the default annotations from the top down?
    // TODO apply from package elements also
    // TODO try to remove some dependencies (e.g. on factory)

    /** Element utilities to use. */
    private final Elements elements;

    /** The value() element/field of a @DefaultQualifier annotation. */
    protected final ExecutableElement defaultQualifierValueElement;

    /** The locations() element/field of a @DefaultQualifier annotation. */
    protected final ExecutableElement defaultQualifierLocationsElement;

    /** The applyToSubpackages() element/field of a @DefaultQualifier annotation. */
    protected final ExecutableElement defaultQualifierApplyToSubpackagesElement;

    /** The value() element/field of a @DefaultQualifier.List annotation. */
    protected final ExecutableElement defaultQualifierListValueElement;

    /** AnnotatedTypeFactory to use. */
    private final AnnotatedTypeFactory atypeFactory;

    /** Defaults for checked code. */
    private final DefaultSet checkedCodeDefaults = new DefaultSet();

    /** Defaults for unchecked code. */
    private final DefaultSet uncheckedCodeDefaults = new DefaultSet();

    /** Size for caches. */
    private static final int CACHE_SIZE = 300;

    /** Mapping from an Element to the bound type. */
    protected final Map<Element, BoundType> elementToBoundType =
            CollectionsPlume.createLruCache(CACHE_SIZE);

    /**
     * Defaults that apply for a certain Element. On the one hand this is used for caching (an
     * earlier name for the field was "qualifierCache"). It can also be used by type systems to set
     * defaults for certain Elements.
     */
    private final IdentityHashMap<Element, DefaultSet> elementDefaults = new IdentityHashMap<>();

    /** A mapping of Element &rarr; Whether or not that element is AnnotatedFor this type system. */
    private final IdentityHashMap<Element, Boolean> elementAnnotatedFors = new IdentityHashMap<>();

    /** CLIMB locations whose standard default is top for a given type system. */
    public static final List<TypeUseLocation> STANDARD_CLIMB_DEFAULTS_TOP =
            Collections.unmodifiableList(
                    Arrays.asList(
                            TypeUseLocation.LOCAL_VARIABLE,
                            TypeUseLocation.RESOURCE_VARIABLE,
                            TypeUseLocation.EXCEPTION_PARAMETER,
                            TypeUseLocation.IMPLICIT_UPPER_BOUND));

    /** CLIMB locations whose standard default is bottom for a given type system. */
    public static final List<TypeUseLocation> STANDARD_CLIMB_DEFAULTS_BOTTOM =
            Collections.unmodifiableList(Arrays.asList(TypeUseLocation.IMPLICIT_LOWER_BOUND));

    /** List of TypeUseLocations that are valid for unchecked code defaults. */
    private static final List<TypeUseLocation> validUncheckedCodeDefaultLocations =
            Collections.unmodifiableList(
                    Arrays.asList(
                            TypeUseLocation.FIELD,
                            TypeUseLocation.PARAMETER,
                            TypeUseLocation.RETURN,
                            TypeUseLocation.RECEIVER,
                            TypeUseLocation.UPPER_BOUND,
                            TypeUseLocation.LOWER_BOUND,
                            TypeUseLocation.OTHERWISE,
                            TypeUseLocation.ALL));

    /** Standard unchecked default locations that should be top. */
    // Fields are defaulted to top so that warnings are issued at field reads, which we believe are
    // more common than field writes. Future work is to specify different defaults for field reads
    // and field writes.  (When a field is written to, its type should be bottom.)
    public static final List<TypeUseLocation> STANDARD_UNCHECKED_DEFAULTS_TOP =
            Collections.unmodifiableList(
                    Arrays.asList(
                            TypeUseLocation.RETURN,
                            TypeUseLocation.FIELD,
                            TypeUseLocation.UPPER_BOUND));

    /** Standard unchecked default locations that should be bottom. */
    public static final List<TypeUseLocation> STANDARD_UNCHECKED_DEFAULTS_BOTTOM =
            Collections.unmodifiableList(
                    Arrays.asList(TypeUseLocation.PARAMETER, TypeUseLocation.LOWER_BOUND));

    /** True if conservative defaults should be used in unannotated source code. */
    private final boolean useConservativeDefaultsSource;

    /** True if conservative defaults should be used for bytecode. */
    private final boolean useConservativeDefaultsBytecode;

    /**
     * Returns an array of locations that are valid for the unchecked value defaults. These are
     * simply by syntax, since an entire file is typechecked, it is not possible for local variables
     * to be unchecked.
     */
    public static List<TypeUseLocation> validLocationsForUncheckedCodeDefaults() {
        return validUncheckedCodeDefaultLocations;
    }

    /**
     * @param elements interface to Element data in the current processing environment
     * @param atypeFactory an annotation factory, used to get annotations by name
     */
    public QualifierDefaults(Elements elements, AnnotatedTypeFactory atypeFactory) {
        this.elements = elements;
        this.atypeFactory = atypeFactory;
        this.useConservativeDefaultsBytecode =
                atypeFactory.getChecker().useConservativeDefault("bytecode");
        this.useConservativeDefaultsSource =
                atypeFactory.getChecker().useConservativeDefault("source");
        ProcessingEnvironment processingEnv = atypeFactory.getProcessingEnv();
        this.defaultQualifierValueElement =
                TreeUtils.getMethod(DefaultQualifier.class, "value", 0, processingEnv);
        this.defaultQualifierLocationsElement =
                TreeUtils.getMethod(DefaultQualifier.class, "locations", 0, processingEnv);
        this.defaultQualifierApplyToSubpackagesElement =
                TreeUtils.getMethod(DefaultQualifier.class, "applyToSubpackages", 0, processingEnv);
        this.defaultQualifierListValueElement =
                TreeUtils.getMethod(DefaultQualifier.List.class, "value", 0, processingEnv);
    }

    @Override
    public String toString() {
        // displays the checked and unchecked code defaults
        return StringsPlume.joinLines(
                "Checked code defaults: ",
                StringsPlume.joinLines(checkedCodeDefaults),
                "Unchecked code defaults: ",
                StringsPlume.joinLines(uncheckedCodeDefaults),
                "useConservativeDefaultsSource: " + useConservativeDefaultsSource,
                "useConservativeDefaultsBytecode: " + useConservativeDefaultsBytecode);
    }

    /**
     * Check that a default with TypeUseLocation OTHERWISE or ALL is specified.
     *
     * @return whether we found a Default with location OTHERWISE or ALL
     */
    public boolean hasDefaultsForCheckedCode() {
        for (Default def : checkedCodeDefaults) {
            if (def.location == TypeUseLocation.OTHERWISE || def.location == TypeUseLocation.ALL) {
                return true;
            }
        }
        return false;
    }

    /** Add standard unchecked defaults that do not conflict with previously added defaults. */
    public void addUncheckedStandardDefaults() {
        QualifierHierarchy qualHierarchy = this.atypeFactory.getQualifierHierarchy();
        AnnotationMirrorSet tops = qualHierarchy.getTopAnnotations();
        AnnotationMirrorSet bottoms = qualHierarchy.getBottomAnnotations();

        for (TypeUseLocation loc : STANDARD_UNCHECKED_DEFAULTS_TOP) {
            // Only add standard defaults in locations where a default has not be specified.
            for (AnnotationMirror top : tops) {
                if (!conflictsWithExistingDefaults(uncheckedCodeDefaults, top, loc)) {
                    addUncheckedCodeDefault(top, loc);
                }
            }
        }

        for (TypeUseLocation loc : STANDARD_UNCHECKED_DEFAULTS_BOTTOM) {
            for (AnnotationMirror bottom : bottoms) {
                // Only add standard defaults in locations where a default has not be specified.
                if (!conflictsWithExistingDefaults(uncheckedCodeDefaults, bottom, loc)) {
                    addUncheckedCodeDefault(bottom, loc);
                }
            }
        }
    }

    /** Add standard CLIMB defaults that do not conflict with previously added defaults. */
    public void addClimbStandardDefaults() {
        QualifierHierarchy qualHierarchy = this.atypeFactory.getQualifierHierarchy();
        AnnotationMirrorSet tops = qualHierarchy.getTopAnnotations();
        AnnotationMirrorSet bottoms = qualHierarchy.getBottomAnnotations();

        for (TypeUseLocation loc : STANDARD_CLIMB_DEFAULTS_TOP) {
            for (AnnotationMirror top : tops) {
                if (!conflictsWithExistingDefaults(checkedCodeDefaults, top, loc)) {
                    // Only add standard defaults in locations where a default has not been
                    // specified.
                    addCheckedCodeDefault(top, loc);
                }
            }
        }

        for (TypeUseLocation loc : STANDARD_CLIMB_DEFAULTS_BOTTOM) {
            for (AnnotationMirror bottom : bottoms) {
                if (!conflictsWithExistingDefaults(checkedCodeDefaults, bottom, loc)) {
                    // Only add standard defaults in locations where a default has not been
                    // specified.
                    addCheckedCodeDefault(bottom, loc);
                }
            }
        }
    }

    /**
     * Adds a default annotation. A programmer may override this by writing the @DefaultQualifier
     * annotation on an element.
     *
     * @param absoluteDefaultAnno the default annotation mirror
     * @param location the type use location
     * @param applyToSubpackages whether the default should be inherited by subpackages
     */
    public void addCheckedCodeDefault(
            AnnotationMirror absoluteDefaultAnno,
            TypeUseLocation location,
            boolean applyToSubpackages) {
        checkDuplicates(checkedCodeDefaults, absoluteDefaultAnno, location);
        checkedCodeDefaults.add(new Default(absoluteDefaultAnno, location, applyToSubpackages));
    }

    /**
     * Adds a default annotation that also applies to subpackages, if applicable. A programmer may
     * override this by writing the @DefaultQualifier annotation on an element.
     *
     * @param absoluteDefaultAnno the default annotation mirror
     * @param location the type use location
     */
    public void addCheckedCodeDefault(
            AnnotationMirror absoluteDefaultAnno, TypeUseLocation location) {
        addCheckedCodeDefault(absoluteDefaultAnno, location, true);
    }

    /**
     * Add a default annotation for unchecked elements.
     *
     * @param uncheckedDefaultAnno the default annotation mirror
     * @param location the type use location
     * @param applyToSubpackages whether the default should be inherited by subpackages
     */
    public void addUncheckedCodeDefault(
            AnnotationMirror uncheckedDefaultAnno,
            TypeUseLocation location,
            boolean applyToSubpackages) {
        checkDuplicates(uncheckedCodeDefaults, uncheckedDefaultAnno, location);
        checkIsValidUncheckedCodeLocation(uncheckedDefaultAnno, location);

        uncheckedCodeDefaults.add(new Default(uncheckedDefaultAnno, location, applyToSubpackages));
    }

    /**
     * Add a default annotation for unchecked elements that also applies to subpackages, if
     * applicable.
     *
     * @param uncheckedDefaultAnno the default annotation mirror
     * @param location the type use location
     */
    public void addUncheckedCodeDefault(
            AnnotationMirror uncheckedDefaultAnno, TypeUseLocation location) {
        addUncheckedCodeDefault(uncheckedDefaultAnno, location, true);
    }

    /** Sets the default annotation for unchecked elements, with specific locations. */
    public void addUncheckedCodeDefaults(
            AnnotationMirror absoluteDefaultAnno, TypeUseLocation[] locations) {
        for (TypeUseLocation location : locations) {
            addUncheckedCodeDefault(absoluteDefaultAnno, location);
        }
    }

    public void addCheckedCodeDefaults(
            AnnotationMirror absoluteDefaultAnno, TypeUseLocation[] locations) {
        for (TypeUseLocation location : locations) {
            addCheckedCodeDefault(absoluteDefaultAnno, location);
        }
    }

    /**
     * Sets the default annotations for a certain Element.
     *
     * @param elem the scope to set the default within
     * @param elementDefaultAnno the default to set
     * @param location the location to apply the default to
     */
    /*
     * TODO(cpovirk): This method looks dangerous for a type system to call early: If it "adds" a
     * default for an Element before defaultsAt runs for that Element, that looks like it would
     * prevent any @DefaultQualifier or similar annotation from having any effect (because
     * defaultsAt would short-circuit after discovering that an entry already exists for the
     * Element). Maybe this method should run defaultsAt before inserting its own entry? Or maybe
     * it's too early to run defaultsAt? Or maybe we'd see new problems in existing code because
     * we'd start running checkDuplicates to look for overlap between the @DefaultQualifier defaults
     * and addElementDefault defaults?
     */
    public void addElementDefault(
            Element elem, AnnotationMirror elementDefaultAnno, TypeUseLocation location) {
        DefaultSet prevset = elementDefaults.get(elem);
        if (prevset != null) {
            checkDuplicates(prevset, elementDefaultAnno, location);
        } else {
            prevset = new DefaultSet();
        }
        // TODO: expose applyToSubpackages
        prevset.add(new Default(elementDefaultAnno, location, true));
        elementDefaults.put(elem, prevset);
    }

    private void checkIsValidUncheckedCodeLocation(
            AnnotationMirror uncheckedDefaultAnno, TypeUseLocation location) {
        boolean isValidUntypeLocation = false;
        for (TypeUseLocation validLoc : validLocationsForUncheckedCodeDefaults()) {
            if (location == validLoc) {
                isValidUntypeLocation = true;
                break;
            }
        }

        if (!isValidUntypeLocation) {
            throw new BugInCF(
                    "Invalid unchecked code default location: "
                            + location
                            + " -> "
                            + uncheckedDefaultAnno);
        }
    }

    private void checkDuplicates(
            DefaultSet previousDefaults, AnnotationMirror newAnno, TypeUseLocation newLoc) {
        if (conflictsWithExistingDefaults(previousDefaults, newAnno, newLoc)) {
            throw new BugInCF(
                    "Only one qualifier from a hierarchy can be the default. Existing: "
                            + previousDefaults
                            + " and new: "
                            // TODO: expose applyToSubpackages
                            + new Default(newAnno, newLoc, true));
        }
    }

    /**
     * Returns true if there are conflicts with existing defaults.
     *
     * @param previousDefaults the previous defaults
     * @param newAnno the new annotation
     * @param newLoc the location of the type use
     * @return true if there are conflicts with existing defaults
     */
    private boolean conflictsWithExistingDefaults(
            DefaultSet previousDefaults, AnnotationMirror newAnno, TypeUseLocation newLoc) {
        QualifierHierarchy qualHierarchy = atypeFactory.getQualifierHierarchy();

        for (Default previous : previousDefaults) {
            if (!AnnotationUtils.areSame(newAnno, previous.anno) && previous.location == newLoc) {
                AnnotationMirror previousTop = qualHierarchy.getTopAnnotation(previous.anno);
                if (qualHierarchy.isSubtypeQualifiersOnly(newAnno, previousTop)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Applies default annotations to a type obtained from an {@link
     * javax.lang.model.element.Element}.
     *
     * @param elt the element from which the type was obtained
     * @param type the type to annotate
     */
    public void annotate(Element elt, AnnotatedTypeMirror type) {
        if (elt != null) {
            switch (elt.getKind()) {
                case FIELD:
                case LOCAL_VARIABLE:
                case PARAMETER:
                case RESOURCE_VARIABLE:
                case EXCEPTION_PARAMETER:
                case ENUM_CONSTANT:
                    String varName = elt.getSimpleName().toString();
                    ((GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory)
                            .getDefaultForTypeAnnotator()
                            .defaultTypeFromName(type, varName);
                    break;

                case METHOD:
                    String methodName = elt.getSimpleName().toString();
                    AnnotatedTypeMirror returnType =
                            ((AnnotatedExecutableType) type).getReturnType();
                    ((GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory)
                            .getDefaultForTypeAnnotator()
                            .defaultTypeFromName(returnType, methodName);
                    break;

                default:
                    break;
            }
        }

        applyDefaultsElement(elt, type, false);
    }

    /**
     * Applies default annotations to a type given a {@link com.sun.source.tree.Tree}.
     *
     * @param tree the tree from which the type was obtained
     * @param type the type to annotate
     */
    public void annotate(Tree tree, AnnotatedTypeMirror type) {
        applyDefaults(tree, type);
    }

    /**
     * Determines the nearest enclosing element for a tree by climbing the tree toward the root and
     * obtaining the element for the first declaration (variable, method, or class) that encloses
     * the tree. Initializers of local variables are handled in a special way: within an initializer
     * we look for the DefaultQualifier(s) annotation and keep track of the previously visited tree.
     * TODO: explain the behavior better.
     *
     * @param tree the tree
     * @return the nearest enclosing element for a tree
     */
    private @Nullable Element nearestEnclosingExceptLocal(Tree tree) {
        TreePath path = atypeFactory.getPath(tree);
        if (path == null) {
            return TreeUtils.elementFromTree(tree);
        }

        Tree prev = null;

        for (Tree t : path) {
            switch (TreeUtils.getKindRecordAsClass(t)) {
                case ANNOTATED_TYPE:
                case ANNOTATION:
                    // If the tree is in an annotation, then there is no relevant scope.
                    return null;
                case VARIABLE:
                    VariableTree vtree = (VariableTree) t;
                    ExpressionTree vtreeInit = vtree.getInitializer();
                    @SuppressWarnings("interning:not.interned") // check cached value
                    boolean sameAsPrev = (vtreeInit != null && prev == vtreeInit);
                    if (sameAsPrev) {
                        Element elt = TreeUtils.elementFromDeclaration((VariableTree) t);
                        AnnotationMirror d =
                                atypeFactory.getDeclAnnotation(elt, DefaultQualifier.class);
                        AnnotationMirror ds =
                                atypeFactory.getDeclAnnotation(elt, DefaultQualifier.List.class);

                        if (d == null && ds == null) {
                            break;
                        }
                    }
                    if (prev != null && prev.getKind() == Tree.Kind.MODIFIERS) {
                        // Annotations are modifiers. We do not want to apply the local variable
                        // default to annotations. Without this, test fenum/TestSwitch failed,
                        // because the default for an argument became incompatible with the declared
                        // type.
                        break;
                    }
                    return TreeUtils.elementFromDeclaration((VariableTree) t);
                case METHOD:
                    return TreeUtils.elementFromDeclaration((MethodTree) t);
                case CLASS: // Including RECORD
                case ENUM:
                case INTERFACE:
                case ANNOTATION_TYPE:
                    return TreeUtils.elementFromDeclaration((ClassTree) t);
                default: // Do nothing.
            }
            prev = t;
        }

        return null;
    }

    /**
     * Applies default annotations to a type. A {@link com.sun.source.tree.Tree} determines the
     * appropriate scope for defaults.
     *
     * <p>For instance, if the tree is associated with a declaration (e.g., it's the use of a field,
     * or a method invocation), defaults in the scope of the <i>declaration</i> are used; if the
     * tree is not associated with a declaration (e.g., a typecast), defaults in the scope of the
     * tree are used.
     *
     * @param tree the tree associated with the type
     * @param type the type to which defaults will be applied
     * @see #applyDefaultsElement(javax.lang.model.element.Element,
     *     org.checkerframework.framework.type.AnnotatedTypeMirror,boolean)
     */
    private void applyDefaults(Tree tree, AnnotatedTypeMirror type) {
        // The location to take defaults from.
        Element elt;
        switch (tree.getKind()) {
            case MEMBER_SELECT:
                elt = TreeUtils.elementFromUse((MemberSelectTree) tree);
                break;

            case IDENTIFIER:
                elt = TreeUtils.elementFromUse((IdentifierTree) tree);
                if (ElementUtils.isTypeDeclaration(elt)) {
                    // If the identifier is a type, then use the scope of the tree.
                    elt = nearestEnclosingExceptLocal(tree);
                }
                break;

            case METHOD_INVOCATION:
                elt = TreeUtils.elementFromUse((MethodInvocationTree) tree);
                break;

            // TODO cases for array access, etc. -- every expression tree
            // (The above probably means that we should use defaults in the
            // scope of the declaration of the array.  Is that right?  -MDE)

            default:
                // If no associated symbol was found, use the tree's (lexical) scope.
                elt = nearestEnclosingExceptLocal(tree);
                // elt = nearestEnclosing(tree);
        }
        // System.out.println("applyDefaults on tree " + tree +
        //        " gives elt: " + elt + "(" + elt.getKind() + ")");

        applyDefaultsElement(elt, type, true);
    }

    /** The default {@code value} element for a @DefaultQualifier annotation. */
    private static final TypeUseLocation[] defaultQualifierValueDefault =
            new TypeUseLocation[] {org.checkerframework.framework.qual.TypeUseLocation.ALL};

    /**
     * Create a DefaultSet from a @DefaultQualifier annotation.
     *
     * @param dq a @DefaultQualifier annotation
     * @return a DefaultSet corresponding to the @DefaultQualifier annotation
     */
    private @Nullable DefaultSet fromDefaultQualifier(AnnotationMirror dq) {
        @SuppressWarnings("unchecked")
        Name cls = AnnotationUtils.getElementValueClassName(dq, defaultQualifierValueElement);
        AnnotationMirror anno = AnnotationBuilder.fromName(elements, cls);

        if (anno == null) {
            return null;
        }

        if (!atypeFactory.isSupportedQualifier(anno)) {
            anno = atypeFactory.canonicalAnnotation(anno);
        }

        if (atypeFactory.isSupportedQualifier(anno)) {
            TypeUseLocation[] locations =
                    AnnotationUtils.getElementValueEnumArray(
                            dq,
                            defaultQualifierLocationsElement,
                            TypeUseLocation.class,
                            defaultQualifierValueDefault);
            boolean applyToSubpackages =
                    AnnotationUtils.getElementValue(
                            dq, defaultQualifierApplyToSubpackagesElement, Boolean.class, true);

            DefaultSet ret = new DefaultSet();
            for (TypeUseLocation loc : locations) {
                ret.add(new Default(anno, loc, applyToSubpackages));
            }
            return ret;
        } else {
            return null;
        }
    }

    private boolean isElementAnnotatedForThisChecker(Element elt) {
        boolean elementAnnotatedForThisChecker = false;

        if (elt == null) {
            throw new BugInCF(
                    "Call of QualifierDefaults.isElementAnnotatedForThisChecker with null");
        }

        if (elementAnnotatedFors.containsKey(elt)) {
            return elementAnnotatedFors.get(elt);
        }

        AnnotationMirror annotatedFor = atypeFactory.getDeclAnnotation(elt, AnnotatedFor.class);

        if (annotatedFor != null) {
            elementAnnotatedForThisChecker =
                    atypeFactory.doesAnnotatedForApplyToThisChecker(annotatedFor);
        }

        if (!elementAnnotatedForThisChecker) {
            Element parent;
            if (elt.getKind() == ElementKind.PACKAGE) {
                // TODO: should AnnotatedFor apply to subpackages??
                // elt.getEnclosingElement() on a package is null; therefore,
                // use the dedicated method.
                parent = ElementUtils.parentPackage((PackageElement) elt, elements);
            } else {
                parent = elt.getEnclosingElement();
            }

            if (parent != null && isElementAnnotatedForThisChecker(parent)) {
                elementAnnotatedForThisChecker = true;
            }
        }

        elementAnnotatedFors.put(elt, elementAnnotatedForThisChecker);

        return elementAnnotatedForThisChecker;
    }

    /**
     * Returns the defaults that apply to the given Element, considering defaults from enclosing
     * Elements.
     *
     * @param elt the element
     * @return the defaults
     */
    private DefaultSet defaultsAt(Element elt) {
        if (elt == null) {
            return DefaultSet.EMPTY;
        }

        if (elementDefaults.containsKey(elt)) {
            return elementDefaults.get(elt);
        }

        DefaultSet qualifiers = defaultsAtDirect(elt);
        DefaultSet parentDefaults;
        if (elt.getKind() == ElementKind.PACKAGE) {
            Element parent = ElementUtils.parentPackage((PackageElement) elt, elements);
            DefaultSet origParentDefaults = defaultsAt(parent);
            parentDefaults = new DefaultSet();
            for (Default d : origParentDefaults) {
                if (d.applyToSubpackages) {
                    parentDefaults.add(d);
                }
            }
        } else {
            Element parent = elt.getEnclosingElement();
            parentDefaults = defaultsAt(parent);
        }

        if (qualifiers == null || qualifiers.isEmpty()) {
            qualifiers = parentDefaults;
        } else {
            // TODO(cpovirk): What should happen with conflicts?
            qualifiers.addAll(parentDefaults);
        }

        /* TODO: it would seem more efficient to also cache null/empty as the result.
         * However, doing so causes KeyFor tests to fail.
               if (qualifiers == null) {
                   qualifiers = DefaultSet.EMPTY;
               }

               elementDefaults.put(elt, qualifiers);
               return qualifiers;
        */
        if (qualifiers != null && !qualifiers.isEmpty()) {
            elementDefaults.put(elt, qualifiers);
            return qualifiers;
        } else {
            return DefaultSet.EMPTY;
        }
    }

    /**
     * Returns the defaults that apply directly to the given Element, without considering enclosing
     * Elements.
     *
     * @param elt the element
     * @return the defaults
     */
    private DefaultSet defaultsAtDirect(Element elt) {
        DefaultSet qualifiers = null;

        // Handle DefaultQualifier
        AnnotationMirror dqAnno = atypeFactory.getDeclAnnotation(elt, DefaultQualifier.class);

        if (dqAnno != null) {
            Set<Default> p = fromDefaultQualifier(dqAnno);

            if (p != null) {
                qualifiers = new DefaultSet();
                qualifiers.addAll(p);
            }
        }

        // Handle DefaultQualifier.List
        AnnotationMirror dqListAnno =
                atypeFactory.getDeclAnnotation(elt, DefaultQualifier.List.class);
        if (dqListAnno != null) {
            if (qualifiers == null) {
                qualifiers = new DefaultSet();
            }
            List<AnnotationMirror> values =
                    AnnotationUtils.getElementValueArray(
                            dqListAnno, defaultQualifierListValueElement, AnnotationMirror.class);
            for (AnnotationMirror dqlAnno : values) {
                Set<Default> p = fromDefaultQualifier(dqlAnno);
                if (p != null) {
                    // TODO(cpovirk): What should happen with conflicts?
                    qualifiers.addAll(p);
                }
            }
        }
        return qualifiers;
    }

    /**
     * Given an element, returns whether the conservative default should be applied for it. Handles
     * elements from bytecode or source code.
     *
     * @param annotationScope the element that the conservative default might apply to
     * @return whether the conservative default applies to the given element
     */
    public boolean applyConservativeDefaults(Element annotationScope) {
        if (annotationScope == null) {
            return false;
        }

        if (uncheckedCodeDefaults.isEmpty()) {
            return false;
        }

        // TODO: I would expect this:
        //   atypeFactory.isFromByteCode(annotationScope)) {
        // to work instead of the
        // isElementFromByteCode/declarationFromElement/isFromStubFile calls,
        // but it doesn't work correctly and tests fail.

        boolean isFromStubFile = atypeFactory.isFromStubFile(annotationScope);
        boolean isBytecode =
                ElementUtils.isElementFromByteCode(annotationScope)
                        && atypeFactory.declarationFromElement(annotationScope) == null
                        && !isFromStubFile;
        if (isBytecode) {
            return useConservativeDefaultsBytecode
                    && !isElementAnnotatedForThisChecker(annotationScope);
        } else if (isFromStubFile) {
            // TODO: Types in stub files not annotated for a particular checker should be
            // treated as unchecked bytecode.  For now, all types in stub files are treated as
            // checked code. Eventually, @AnnotatedFor("checker") will be programmatically added
            // to methods in stub files supplied via the @StubFiles annotation.  Stub files will
            // be treated like unchecked code except for methods in the scope of an @AnnotatedFor.
            return false;
        } else if (useConservativeDefaultsSource) {
            return !isElementAnnotatedForThisChecker(annotationScope);
        }
        return false;
    }

    /**
     * Applies default annotations to a type. Conservative defaults are applied first as
     * appropriate, followed by source code defaults.
     *
     * <p>For a discussion on the rules for application of source code and conservative defaults,
     * please see the linked manual sections.
     *
     * @param annotationScope the element representing the nearest enclosing default annotation
     *     scope for the type
     * @param type the type to which defaults will be applied
     * @param fromTree whether the element came from a tree
     * @checker_framework.manual #effective-qualifier The effective qualifier on a type (defaults
     *     and inference)
     * @checker_framework.manual #annotating-libraries Annotating libraries
     */
    private void applyDefaultsElement(
            Element annotationScope, AnnotatedTypeMirror type, boolean fromTree) {
        DefaultApplierElement applier =
                createDefaultApplierElement(atypeFactory, annotationScope, type, fromTree);

        DefaultSet defaults = defaultsAt(annotationScope);

        // If there is a default for type variable uses, do not also apply checked/unchecked code
        // defaults to type variables. Otherwise, the default in scope could decide not to annotate
        // the type variable use, whereas the checked/unchecked code default could add an
        // annotation.
        // TODO: the checked/unchecked defaults should be added to `defaults` and then only one
        // iteration through the defaults should be necessary.
        boolean typeVarUseDef = false;

        for (Default def : defaults) {
            applier.applyDefault(def);
            typeVarUseDef |= (def.location == TypeUseLocation.TYPE_VARIABLE_USE);
        }

        if (applyConservativeDefaults(annotationScope)) {
            for (Default def : uncheckedCodeDefaults) {
                if (!typeVarUseDef || def.location != TypeUseLocation.TYPE_VARIABLE_USE) {
                    applier.applyDefault(def);
                }
            }
        }

        for (Default def : checkedCodeDefaults) {
            if (!typeVarUseDef || def.location != TypeUseLocation.TYPE_VARIABLE_USE) {
                applier.applyDefault(def);
            }
        }
    }

    /**
     * Create the default applier element.
     *
     * @param atypeFactory the annotated type factory
     * @param annotationScope the scope of the default
     * @param type the type to which to apply the default
     * @param fromTree whether the element came from a tree
     * @return the default applier element
     */
    protected DefaultApplierElement createDefaultApplierElement(
            AnnotatedTypeFactory atypeFactory,
            Element annotationScope,
            AnnotatedTypeMirror type,
            boolean fromTree) {
        return new DefaultApplierElement(atypeFactory, annotationScope, type, fromTree);
    }

    /** A default applier element. */
    protected class DefaultApplierElement {

        /** The annotated type factory. */
        protected final AnnotatedTypeFactory atypeFactory;

        /** The qualifier hierarchy. */
        protected final QualifierHierarchy qualHierarchy;

        /** The scope of the default. */
        protected final Element scope;

        /** The type to which to apply the default. */
        protected final AnnotatedTypeMirror type;

        /** Whether the element came from a tree. */
        protected final boolean fromTree;

        /**
         * True if type variable uses as top-level type of local variables should be defaulted.
         *
         * @see GenericAnnotatedTypeFactory#getShouldDefaultTypeVarLocals()
         */
        private final boolean shouldDefaultTypeVarLocals;

        /**
         * Location to which to apply the default. (Should only be set by the applyDefault method.)
         */
        protected TypeUseLocation location;

        /** The default element applier implementation. */
        protected final DefaultApplierElementImpl impl;

        /**
         * Create an instance.
         *
         * @param atypeFactory the type factory
         * @param scope the scope for the defaults
         * @param type the type to default
         * @param fromTree whether the element came from a tree
         */
        public DefaultApplierElement(
                AnnotatedTypeFactory atypeFactory,
                Element scope,
                AnnotatedTypeMirror type,
                boolean fromTree) {
            this.atypeFactory = atypeFactory;
            this.qualHierarchy = atypeFactory.getQualifierHierarchy();
            this.scope = scope;
            this.type = type;
            this.fromTree = fromTree;
            this.shouldDefaultTypeVarLocals =
                    (atypeFactory instanceof GenericAnnotatedTypeFactory<?, ?, ?, ?>)
                            && ((GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory)
                                    .getShouldDefaultTypeVarLocals();
            this.impl = new DefaultApplierElementImpl(this);
        }

        /**
         * Apply default to the type.
         *
         * @param def default to apply
         */
        public void applyDefault(Default def) {
            this.location = def.location;
            impl.visit(type, def.anno);
        }

        /**
         * Returns true if the given qualifier should be applied to the given type. Currently we do
         * not apply defaults to void types, packages, wildcards, and type variables.
         *
         * @param type type to which qual would be applied
         * @return true if this application should proceed
         */
        protected boolean shouldBeAnnotated(AnnotatedTypeMirror type) {
            return type != null
                    // TODO: executables themselves should not be annotated
                    // For some reason h1h2checker-tests fails with this.
                    // || type.getKind() == TypeKind.EXECUTABLE
                    && type.getKind() != TypeKind.NONE
                    && type.getKind() != TypeKind.WILDCARD
                    && type.getKind() != TypeKind.TYPEVAR
                    && !(type instanceof AnnotatedNoType);
        }

        /**
         * Add the qualifier to the type if it does not already have an annotation in the same
         * hierarchy as qual.
         *
         * @param type type to add qual
         * @param qual annotation to add
         */
        protected void addAnnotation(AnnotatedTypeMirror type, AnnotationMirror qual) {
            // Add the default annotation, but only if no other annotation is present.
            if (type.getKind() != TypeKind.EXECUTABLE) {
                type.addMissingAnnotation(qual);
            }
        }
    }

    // Only reason this cannot be `static` is call to `getBoundType`.
    protected class DefaultApplierElementImpl extends AnnotatedTypeScanner<Void, AnnotationMirror> {
        private final DefaultApplierElement outer;

        protected DefaultApplierElementImpl(DefaultApplierElement outer) {
            this.outer = outer;
        }

        @Override
        public Void scan(@FindDistinct AnnotatedTypeMirror t, AnnotationMirror qual) {
            if (!outer.shouldBeAnnotated(t)) {
                // Type variables and wildcards are separately handled in the corresponding visitors
                // below.
                return super.scan(t, qual);
            }

            // Some defaults only apply to the top level type.
            boolean isTopLevelType = t == outer.type;
            switch (outer.location) {
                case FIELD:
                    if (outer.scope != null
                            && outer.scope.getKind() == ElementKind.FIELD
                            && isTopLevelType) {
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case LOCAL_VARIABLE:
                    if (outer.scope != null
                            && outer.scope.getKind() == ElementKind.LOCAL_VARIABLE
                            && isTopLevelType) {
                        // TODO: how do we determine that we are in a cast or instanceof type?
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case RESOURCE_VARIABLE:
                    if (outer.scope != null
                            && outer.scope.getKind() == ElementKind.RESOURCE_VARIABLE
                            && isTopLevelType) {
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case EXCEPTION_PARAMETER:
                    if (outer.scope != null
                            && outer.scope.getKind() == ElementKind.EXCEPTION_PARAMETER
                            && isTopLevelType) {
                        outer.addAnnotation(t, qual);
                        if (t.getKind() == TypeKind.UNION) {
                            AnnotatedUnionType aut = (AnnotatedUnionType) t;
                            // Also apply the default to the alternative types
                            for (AnnotatedDeclaredType anno : aut.getAlternatives()) {
                                outer.addAnnotation(anno, qual);
                            }
                        }
                    }
                    break;
                case PARAMETER:
                    if (outer.scope != null
                            && outer.scope.getKind() == ElementKind.PARAMETER
                            && isTopLevelType) {
                        outer.addAnnotation(t, qual);
                    } else if (outer.scope != null
                            && (outer.scope.getKind() == ElementKind.METHOD
                                    || outer.scope.getKind() == ElementKind.CONSTRUCTOR)
                            && t.getKind() == TypeKind.EXECUTABLE
                            && isTopLevelType) {
                        for (AnnotatedTypeMirror atm :
                                ((AnnotatedExecutableType) t).getParameterTypes()) {
                            if (outer.shouldBeAnnotated(atm)) {
                                outer.addAnnotation(atm, qual);
                            }
                        }
                    }
                    break;
                case RECEIVER:
                    if (outer.scope != null
                            && outer.scope.getKind() == ElementKind.PARAMETER
                            && isTopLevelType
                            && outer.scope.getSimpleName().contentEquals("this")) {
                        // TODO: comparison against "this" is ugly, won't work
                        // for all possible names for receiver parameter.
                        // Comparison to Names._this might be a bit faster.
                        outer.addAnnotation(t, qual);
                    } else if (outer.scope != null
                            && (outer.scope.getKind() == ElementKind.METHOD)
                            // TODO: Constructors can also have receivers.
                            && t.getKind() == TypeKind.EXECUTABLE
                            && isTopLevelType) {
                        AnnotatedDeclaredType receiver =
                                ((AnnotatedExecutableType) t).getReceiverType();
                        if (outer.shouldBeAnnotated(receiver)) {
                            outer.addAnnotation(receiver, qual);
                        }
                    }
                    break;
                case RETURN:
                    if (outer.scope != null
                            && outer.scope.getKind() == ElementKind.METHOD
                            && t.getKind() == TypeKind.EXECUTABLE
                            && isTopLevelType) {
                        AnnotatedTypeMirror returnType =
                                ((AnnotatedExecutableType) t).getReturnType();
                        if (outer.shouldBeAnnotated(returnType)) {
                            outer.addAnnotation(returnType, qual);
                        }
                    }
                    break;
                case CONSTRUCTOR_RESULT:
                    if (outer.scope != null
                            && outer.scope.getKind() == ElementKind.CONSTRUCTOR
                            && t.getKind() == TypeKind.EXECUTABLE
                            && isTopLevelType) {
                        // This is the return type of a constructor declaration (not a
                        // constructor invocation).
                        AnnotatedTypeMirror returnType =
                                ((AnnotatedExecutableType) t).getReturnType();
                        if (outer.shouldBeAnnotated(returnType)) {
                            outer.addAnnotation(returnType, qual);
                        }
                    }
                    break;
                case IMPLICIT_LOWER_BOUND:
                    if (isLowerBound
                            && (boundType == BoundType.TYPEVAR_UNBOUNDED
                                    || boundType == BoundType.TYPEVAR_UPPER
                                    || boundType == BoundType.WILDCARD_UNBOUNDED
                                    || boundType == BoundType.WILDCARD_UPPER)) {
                        // TODO: split type variables and wildcards?
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case EXPLICIT_LOWER_BOUND:
                    if (isLowerBound && boundType == BoundType.WILDCARD_LOWER) {
                        // TODO: split type variables and wildcards?
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case LOWER_BOUND:
                    if (isLowerBound) {
                        // TODO: split type variables and wildcards?
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case IMPLICIT_UPPER_BOUND:
                    if (isUpperBound
                            && (boundType == BoundType.TYPEVAR_UNBOUNDED
                                    || boundType == BoundType.WILDCARD_UNBOUNDED
                                    || boundType == BoundType.WILDCARD_LOWER)) {
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case IMPLICIT_TYPE_PARAMETER_UPPER_BOUND:
                    if (isUpperBound && boundType == BoundType.TYPEVAR_UNBOUNDED) {
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case IMPLICIT_WILDCARD_UPPER_BOUND_NO_SUPER:
                    if (isUpperBound && boundType == BoundType.WILDCARD_UNBOUNDED) {
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case IMPLICIT_WILDCARD_UPPER_BOUND_SUPER:
                    if (isUpperBound && boundType == BoundType.WILDCARD_LOWER) {
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case IMPLICIT_WILDCARD_UPPER_BOUND:
                    if (isUpperBound
                            && (boundType == BoundType.WILDCARD_UNBOUNDED
                                    || boundType == BoundType.WILDCARD_LOWER)) {
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case EXPLICIT_UPPER_BOUND:
                    if (isUpperBound
                            && (boundType == BoundType.TYPEVAR_UPPER
                                    || boundType == BoundType.WILDCARD_UPPER)) {
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case EXPLICIT_TYPE_PARAMETER_UPPER_BOUND:
                    if (isUpperBound && boundType == BoundType.TYPEVAR_UPPER) {
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case EXPLICIT_WILDCARD_UPPER_BOUND:
                    if (isUpperBound && boundType == BoundType.WILDCARD_UPPER) {
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case UPPER_BOUND:
                    if (isUpperBound) {
                        // TODO: split type variables and wildcards?
                        outer.addAnnotation(t, qual);
                    }
                    break;
                case OTHERWISE:
                case ALL:
                    // TODO: forbid ALL if anything else was given.
                    outer.addAnnotation(t, qual);
                    break;
                case TYPE_VARIABLE_USE:
                    // This location is handled in visitTypeVariable below. Do nothing here.
                    break;
                default:
                    throw new BugInCF(
                            "QualifierDefaults.DefaultApplierElement: unhandled location: "
                                    + outer.location);
            }

            return super.scan(t, qual);
        }

        @Override
        public void reset() {
            super.reset();
            isLowerBound = false;
            isUpperBound = false;
            boundType = BoundType.TYPEVAR_UNBOUNDED;
        }

        /** Are we currently defaulting the lower bound of a type variable or wildcard? */
        private boolean isLowerBound = false;

        /** Are we currently defaulting the upper bound of a type variable or wildcard? */
        private boolean isUpperBound = false;

        /** The bound type of the current wildcard or type variable being defaulted. */
        private BoundType boundType = BoundType.TYPEVAR_UNBOUNDED;

        @Override
        public Void visitTypeVariable(
                @FindDistinct AnnotatedTypeVariable type, AnnotationMirror qual) {
            if (visitedNodes.containsKey(type)) {
                return null;
            }
            if (outer.qualHierarchy.isParametricQualifier(qual)) {
                // Parametric qualifiers are only applicable to type variables and have no effect on
                // their type. Therefore, do nothing.
                return null;
            }
            if (type.isDeclaration()) {
                // For a type variable declaration, apply the defaults to the bounds. Do not apply
                // `TYPE_VARIALBE_USE` defaults.
                visitBounds(type, type.getUpperBound(), type.getLowerBound(), qual);
                return null;
            }

            boolean isTopLevelType = type == outer.type;
            boolean isLocalVariable =
                    outer.scope != null && ElementUtils.isLocalVariable(outer.scope);

            if (isTopLevelType && isLocalVariable) {
                if (outer.shouldDefaultTypeVarLocals
                        && outer.fromTree
                        && outer.location == TypeUseLocation.LOCAL_VARIABLE) {
                    outer.addAnnotation(type, qual);
                } else {
                    // TODO: Should `TYPE_VARIABLE_USE` default apply to top-level local variables,
                    // if they should not be defaulted according to `shouldDefaultTypeVarLocals`?
                    visitBounds(type, type.getUpperBound(), type.getLowerBound(), qual);
                }
            } else {
                if (outer.location == TypeUseLocation.TYPE_VARIABLE_USE) {
                    outer.addAnnotation(type, qual);
                } else {
                    visitBounds(type, type.getUpperBound(), type.getLowerBound(), qual);
                }
            }
            return null;
        }

        @Override
        public Void visitWildcard(AnnotatedWildcardType type, AnnotationMirror qual) {
            if (visitedNodes.containsKey(type)) {
                return null;
            }
            visitBounds(type, type.getExtendsBound(), type.getSuperBound(), qual);
            return null;
        }

        /**
         * Visit the bounds of a type variable or a wildcard and potentially apply qual to those
         * bounds. This method will also update the boundType, isLowerBound, and isUpperbound
         * fields.
         */
        protected void visitBounds(
                AnnotatedTypeMirror boundedType,
                AnnotatedTypeMirror upperBound,
                AnnotatedTypeMirror lowerBound,
                AnnotationMirror qual) {
            boolean prevIsUpperBound = isUpperBound;
            boolean prevIsLowerBound = isLowerBound;
            BoundType prevBoundType = boundType;

            boundType = getBoundType(boundedType);

            try {
                isLowerBound = true;
                isUpperBound = false;
                scanAndReduce(lowerBound, qual, null);

                visitedNodes.put(boundedType, null);

                isLowerBound = false;
                isUpperBound = true;
                scanAndReduce(upperBound, qual, null);

                visitedNodes.put(boundedType, null);
            } finally {
                isUpperBound = prevIsUpperBound;
                isLowerBound = prevIsLowerBound;
                boundType = prevBoundType;
            }
        }
    }

    /**
     * Specifies whether the type variable or wildcard has an explicit upper bound (UPPER), an
     * explicit lower bound (LOWER), or no explicit bounds (UNBOUNDED).
     */
    protected enum BoundType {

        /** Indicates an upper-bounded type variable. */
        TYPEVAR_UPPER,

        /**
         * Neither bound is specified, BOTH are implicit. (If a type variable is declared in
         * bytecode and the type of the upper bound is Object, then the checker assumes that the
         * bound was not explicitly written in source code.)
         */
        TYPEVAR_UNBOUNDED,

        /** Indicates an upper-bounded wildcard. */
        WILDCARD_UPPER,

        /** Indicates a lower-bounded wildcard. */
        WILDCARD_LOWER,

        /** Neither bound is specified, BOTH are implicit. */
        WILDCARD_UNBOUNDED;
    }

    /**
     * Returns the boundType for type.
     *
     * @param type the type whose boundType is returned. type must be an AnnotatedWildcardType or
     *     AnnotatedTypeVariable.
     * @return the boundType for type
     */
    private BoundType getBoundType(AnnotatedTypeMirror type) {
        if (type instanceof AnnotatedTypeVariable) {
            return getTypeVarBoundType((AnnotatedTypeVariable) type);
        }

        if (type instanceof AnnotatedWildcardType) {
            return getWildcardBoundType((AnnotatedWildcardType) type);
        }

        throw new BugInCF("Unexpected type kind: type=" + type);
    }

    /**
     * Returns the bound type of the input typeVar.
     *
     * @param typeVar the type variable
     * @return the bound type of the input typeVar
     */
    private BoundType getTypeVarBoundType(AnnotatedTypeVariable typeVar) {
        return getTypeVarBoundType((TypeParameterElement) typeVar.getUnderlyingType().asElement());
    }

    /**
     * Returns the boundType (TYPEVAR_UPPER or TYPEVAR_UNBOUNDED) of the declaration of
     * typeParamElem.
     *
     * @param typeParamElem the type parameter element
     * @return the boundType (TYPEVAR_UPPER or TYPEVAR_UNBOUNDED) of the declaration of
     *     typeParamElem
     */
    // Results are cached in {@link elementToBoundType}.
    private BoundType getTypeVarBoundType(TypeParameterElement typeParamElem) {
        BoundType prev = elementToBoundType.get(typeParamElem);
        if (prev != null) {
            return prev;
        }

        TreePath declaredTypeVarEle = atypeFactory.getTreeUtils().getPath(typeParamElem);
        Tree typeParamDecl = declaredTypeVarEle == null ? null : declaredTypeVarEle.getLeaf();

        final BoundType boundType;
        if (typeParamDecl == null) {
            // This is not only for elements from binaries, but also
            // when the compilation unit is no-longer available.
            if (typeParamElem.getBounds().size() == 1
                    && TypesUtils.isObject(typeParamElem.getBounds().get(0))) {
                // If the bound was Object, then it may or may not have been explicitly written.
                // Assume that it was not.
                boundType = BoundType.TYPEVAR_UNBOUNDED;
            } else {
                // The bound is not Object, so it must have been explicitly written and thus the
                // type variable has an upper bound.
                boundType = BoundType.TYPEVAR_UPPER;
            }
        } else {
            if (typeParamDecl.getKind() == Tree.Kind.TYPE_PARAMETER) {
                TypeParameterTree tptree = (TypeParameterTree) typeParamDecl;

                List<? extends Tree> bnds = tptree.getBounds();
                if (bnds != null && !bnds.isEmpty()) {
                    boundType = BoundType.TYPEVAR_UPPER;
                } else {
                    boundType = BoundType.TYPEVAR_UNBOUNDED;
                }
            } else {
                throw new BugInCF(
                        StringsPlume.joinLines(
                                "Unexpected tree type for typeVar Element:",
                                "typeParamElem=" + typeParamElem,
                                typeParamDecl));
            }
        }

        elementToBoundType.put(typeParamElem, boundType);
        return boundType;
    }

    /**
     * Returns the BoundType of wildcardType.
     *
     * @param wildcardType the annotated wildcard type
     * @return the BoundType of annotatedWildcard
     */
    private BoundType getWildcardBoundType(AnnotatedWildcardType wildcardType) {
        if (AnnotatedTypes.hasNoExplicitBound(wildcardType)) {
            return BoundType.WILDCARD_UNBOUNDED;
        } else if (AnnotatedTypes.hasExplicitSuperBound(wildcardType)) {
            return BoundType.WILDCARD_LOWER;
        } else {
            return BoundType.WILDCARD_UPPER;
        }
    }
}

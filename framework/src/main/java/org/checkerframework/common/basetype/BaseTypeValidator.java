package org.checkerframework.common.basetype;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.ArrayMap;
import org.plumelib.util.IPair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

/**
 * A visitor to validate the types in a tree.
 *
 * <p>The validator is called on the type of every expression, such as on the right-hand side of
 * {@code x = Optional.of(Optional.of("baz"));}. However, note that the type of the right-hand side
 * is {@code Optional<? extends Object>}, not {@code Optional<Optional<String>>}.
 *
 * <p>Note: A TypeValidator (this class and its subclasses) cannot tell whether an annotation was
 * written by a programmer or defaulted/inferred/computed by the Checker Framework, because the
 * AnnotatedTypeMirror does not make distinctions about which annotations in an AnnotatedTypeMirror
 * were explicitly written and which were added by a checker. To issue a warning/error only when a
 * programmer writes an annotation, override {@link BaseTypeVisitor#visitAnnotatedType} and {@link
 * BaseTypeVisitor#visitVariable}.
 */
public class BaseTypeValidator extends AnnotatedTypeScanner<Void, Tree> implements TypeValidator {
    /** Is the type valid? This is side-effected by the visitor, and read at the end of visiting. */
    protected boolean isValid = true;

    /** Should the primary annotation on the top level type be checked? */
    protected boolean checkTopLevelDeclaredOrPrimitiveType = true;

    /** BaseTypeChecker. */
    protected final BaseTypeChecker checker;

    /** BaseTypeVisitor. */
    protected final BaseTypeVisitor<?> visitor;

    /** AnnotatedTypeFactory. */
    protected final AnnotatedTypeFactory atypeFactory;

    /** The qualifer hierarchy. */
    protected final QualifierHierarchy qualHierarchy;

    /** True if "-AignoreTargetLocations" was passed on the command line. */
    protected final boolean ignoreTargetLocations;

    /**
     * Mapping from qualifier canonical names to their declared type-use locations, as specified by
     * the {@link org.checkerframework.framework.qual.TargetLocations} meta-annotation.
     */
    protected final Map<@CanonicalName String, List<TypeUseLocation>> qualAllowedLocations;

    /**
     * True if no supported qualifier in this type system declares {@link
     * org.checkerframework.framework.qual.TargetLocations}. Used to short-circuit target-location
     * checks.
     */
    protected final boolean noQualHasTargetLocations;

    /**
     * Creates a new BaseTypeValidator.
     *
     * @param checker the checker
     * @param visitor the visitor
     * @param atypeFactory the type factory
     */
    // TODO: clean up coupling between components
    public BaseTypeValidator(
            BaseTypeChecker checker,
            BaseTypeVisitor<?> visitor,
            AnnotatedTypeFactory atypeFactory) {
        this.checker = checker;
        this.visitor = visitor;
        this.atypeFactory = atypeFactory;
        this.qualHierarchy = atypeFactory.getQualifierHierarchy();
        this.ignoreTargetLocations = checker.hasOption("ignoreTargetLocations");
        this.qualAllowedLocations = createQualAllowedLocations(atypeFactory);
        boolean anyHas = false;
        for (List<TypeUseLocation> locs : qualAllowedLocations.values()) {
            if (locs != null) {
                anyHas = true;
                break;
            }
        }
        this.noQualHasTargetLocations = !anyHas;
    }

    /**
     * Create a new map, which is used for declared type-use locations lookup.
     *
     * @param atypeFactory the annotated type factory
     * @return a new mapping from strings of qualifier names to their declared type-use locations
     */
    protected static Map<@CanonicalName String, List<TypeUseLocation>> createQualAllowedLocations(
            AnnotatedTypeFactory atypeFactory) {
        HashMap<@CanonicalName String, List<TypeUseLocation>> qualAllowedLocations =
                new HashMap<>();
        for (String qual : atypeFactory.getSupportedTypeQualifierNames()) {
            Element elem = atypeFactory.getElementUtils().getTypeElement(qual);
            TargetLocations tls = elem.getAnnotation(TargetLocations.class);
            // @Target({ElementType.TYPE_USE})} together with no @TargetLocations(...) means that
            // the qualifier can be written on any type use.
            if (tls == null) {
                qualAllowedLocations.put(qual, null);
                continue;
            }
            List<TypeUseLocation> locations = Arrays.asList(tls.value());
            qualAllowedLocations.put(qual, locations);
        }
        return qualAllowedLocations;
    }

    /**
     * Validate the type against the given tree. This method both issues error messages and also
     * returns a boolean value.
     *
     * <p>This is the entry point to the type validator. Neither this method nor visit should be
     * called directly by a visitor, only use {@link BaseTypeVisitor#validateTypeOf(Tree)}.
     *
     * <p>This method is only called on top-level types, but it validates the entire type including
     * components of a compound type. Subclasses should override this only if there is special-case
     * behavior that should be performed only on top-level types.
     *
     * @param type the type to validate
     * @param tree the tree from which the type originated. If the tree is a method tree, {@code
     *     type} is its return type. If the tree is a variable tree, {@code type} is the variable's
     *     type.
     * @return true if the type is valid
     */
    @Override
    public boolean isValid(AnnotatedTypeMirror type, Tree tree) {
        List<DiagMessage> diagMessages = isValidStructurally(type);
        if (!diagMessages.isEmpty()) {
            for (DiagMessage d : diagMessages) {
                checker.report(tree, d);
            }
            return false;
        }
        this.isValid = true;
        this.checkTopLevelDeclaredOrPrimitiveType =
                shouldCheckTopLevelDeclaredOrPrimitiveType(type, tree);
        visit(type, tree);
        return this.isValid;
    }

    /**
     * Should the top-level declared or primitive type be checked?
     *
     * <p>If {@code type} is not a declared or primitive type, then this method returns true.
     *
     * <p>Top-level type is not checked if tree is a local variable or an expression tree.
     *
     * @param type the AnnotatedTypeMirror being validated
     * @param tree a Tree whose type is {@code type}
     * @return whether or not the top-level type should be checked, if {@code type} is a declared or
     *     primitive type.
     */
    protected boolean shouldCheckTopLevelDeclaredOrPrimitiveType(
            AnnotatedTypeMirror type, Tree tree) {
        if (type.getKind() != TypeKind.DECLARED && !type.getKind().isPrimitive()) {
            return true;
        }
        return !TreeUtils.isLocalVariable(tree)
                && (!TreeUtils.isExpressionTree(tree) || TreeUtils.isTypeTree(tree));
    }

    /**
     * Performs some well-formedness checks on the given {@link AnnotatedTypeMirror}. Returns a list
     * of failures. If successful, returns an empty list. The method will never return failures for
     * a valid type, but might not catch all invalid types.
     *
     * <p>This method ensures that the type is structurally or lexically well-formed, but it does
     * not check whether the annotations are semantically sensible. Subclasses should generally
     * override visit methods such as {@link #visitDeclared} rather than this method.
     *
     * <p>Currently, this implementation checks the following (subclasses can extend this behavior):
     *
     * <ol>
     *   <li>There should not be multiple annotations from the same qualifier hierarchy.
     *   <li>There should not be more annotations than the width of the QualifierHierarchy.
     *   <li>If the type is not a type variable, then the number of annotations should be the same
     *       as the width of the QualifierHierarchy.
     *   <li>These properties should also hold recursively for component types of arrays and for
     *       bounds of type variables and wildcards.
     * </ol>
     *
     * This does not test whether the Java type is relevant, because by the time this method is
     * called, the type includes some non-programmer-written annotations.
     *
     * @param type the type to test
     * @return list of reasons the type is invalid, or empty list if the type is valid
     */
    protected List<DiagMessage> isValidStructurally(AnnotatedTypeMirror type) {
        if (structuralScanner == null) {
            // Created lazily (rather than in a field initializer) to keep `this` from escaping
            // during construction; the captured `isTopLevelValidType` is dispatched dynamically, so
            // subclass overrides still apply. A single scanner is reused across calls -- it was a
            // per-call allocation source in checkNullness traces -- which is safe because this
            // validator is confined to the javac main thread and SimpleAnnotatedTypeScanner.visit
            // resets its state on each call. isValidStructurally is not re-entrant: it is called
            // once per top-level type and its scan only reads annotations.
            structuralScanner =
                    new SimpleAnnotatedTypeScanner<>(
                            (atm, p) -> isTopLevelValidType(atm),
                            DiagMessage::mergeLists,
                            Collections.emptyList());
        }
        return structuralScanner.visit(type, null);
    }

    /** The reusable scanner backing {@link #isValidStructurally}; see there. */
    private @Nullable SimpleAnnotatedTypeScanner<List<DiagMessage>, Void> structuralScanner = null;

    /**
     * Checks every property listed in {@link #isValidStructurally}, but only for the top level
     * type. If successful, returns an empty list. If not successful, returns diagnostics.
     *
     * @param type the type to be checked
     * @return the diagnostics indicating failure, or an empty list if successful
     */
    // This method returns a singleton or empyty list.  Its return type is List rather than
    // DiagMessage (with null indicting success) because its caller, isValidStructurally(), expects
    // a list.
    protected List<DiagMessage> isTopLevelValidType(AnnotatedTypeMirror type) {
        // multiple annotations from the same hierarchy
        AnnotationMirrorSet annotations = type.getAnnotations();
        AnnotationMirrorSet seenTops = new AnnotationMirrorSet();
        for (AnnotationMirror anno : annotations) {
            AnnotationMirror top = qualHierarchy.getTopAnnotation(anno);
            if (AnnotationUtils.containsSame(seenTops, top)) {
                return Collections.singletonList(
                        DiagMessage.error("type.invalid.conflicting.annos", annotations, type));
            }
            seenTops.add(top);
        }

        boolean canHaveEmptyAnnotationSet = QualifierHierarchy.canHaveEmptyAnnotationSet(type);

        // wrong number of annotations
        if (!canHaveEmptyAnnotationSet && seenTops.size() < qualHierarchy.getWidth()) {
            return Collections.singletonList(
                    DiagMessage.error("type.invalid.too.few.annotations", annotations, type));
        }

        // success
        return Collections.emptyList();
    }

    protected void reportValidityResult(
            @CompilerMessageKey String errorType, AnnotatedTypeMirror type, Tree p) {
        checker.reportError(p, errorType, type.getAnnotations(), type.toString());
        isValid = false;
    }

    /**
     * Like {@link #reportValidityResult}, but the type is printed in the error message without
     * annotations. This method would print "annotation @NonNull is not permitted on type int",
     * whereas {@link #reportValidityResult} would print "annotation @NonNull is not permitted on
     * type @NonNull int". In addition, when the underlying type is a compound type such as
     * {@code @Bad List<String>}, the erased type will be used, i.e., "{@code List}" will print
     * instead of "{@code @Bad List<String>}".
     */
    protected void reportValidityResultOnUnannotatedType(
            @CompilerMessageKey String errorType, AnnotatedTypeMirror type, Tree p) {
        TypeMirror underlying =
                TypeAnnotationUtils.unannotatedType(type.getErased().getUnderlyingType());
        checker.reportError(p, errorType, type.getAnnotations(), underlying.toString());
        isValid = false;
    }

    /**
     * Most errors reported by this class are of the form type.invalid. This method reports when the
     * bounds of a wildcard or type variable don't make sense. Bounds make sense when the effective
     * annotations on the upper bound are supertypes of those on the lower bounds for all
     * hierarchies. To ensure that this subtlety is not lost on users, we report
     * "bound.type.incompatible" and print the bounds along with the invalid type rather than a
     * "type.invalid".
     *
     * @param type the type with invalid bounds
     * @param tree where to report the error
     */
    protected void reportInvalidBounds(AnnotatedTypeMirror type, Tree tree) {
        final String label;
        final AnnotatedTypeMirror upperBound;
        final AnnotatedTypeMirror lowerBound;

        switch (type.getKind()) {
            case TYPEVAR:
                label = "type parameter";
                upperBound = ((AnnotatedTypeVariable) type).getUpperBound();
                lowerBound = ((AnnotatedTypeVariable) type).getLowerBound();
                break;

            case WILDCARD:
                label = "wildcard";
                upperBound = ((AnnotatedWildcardType) type).getExtendsBound();
                lowerBound = ((AnnotatedWildcardType) type).getSuperBound();
                break;

            default:
                throw new BugInCF("Type is not bounded.%ntype=%s%ntree=%s", type, tree);
        }

        checker.reportError(
                tree,
                "bound.type.incompatible",
                label,
                type.toString(),
                upperBound.toString(true),
                lowerBound.toString(true));
        isValid = false;
    }

    protected void reportInvalidType(AnnotatedTypeMirror type, Tree p) {
        reportValidityResult("type.invalid", type, p);
    }

    /**
     * Report an "annotations.on.use" error for the given type and tree.
     *
     * @param type the type with invalid annotations
     * @param p the tree where to report the error
     */
    protected void reportInvalidAnnotationsOnUse(AnnotatedTypeMirror type, Tree p) {
        reportValidityResultOnUnannotatedType("type.invalid.annotations.on.use", type, p);
    }

    /**
     * Returns whether this checker makes a qualifier that appears at a type-use location not
     * permitted by its {@link org.checkerframework.framework.qual.TargetLocations} meta-annotation
     * inert, rather than letting it keep influencing type checking.
     *
     * <p>Subclasses of {@code BaseTypeValidator} may override this method to enable the stripping
     * behavior.
     *
     * @return true if location-invalid qualifiers on bounds should be made inert
     */
    protected boolean shouldStripInvalidLocationQualifiers() {
        return false;
    }

    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
        if (hasVisited(type)) {
            return getVisited(type);
        }

        boolean skipChecks = checker.shouldSkipUses(type.getUnderlyingType().asElement());

        if (checkTopLevelDeclaredOrPrimitiveType && !skipChecks) {
            // Ensure that type use is a subtype of the element type
            // isValidUse determines the erasure of the types.

            AnnotationMirrorSet bounds =
                    atypeFactory.getTypeDeclarationBounds(type.getUnderlyingType());

            AnnotatedDeclaredType elemType = type.deepCopy();
            elemType.clearAnnotations();
            elemType.addAnnotations(bounds);

            if (!visitor.isValidUse(elemType, type, tree)) {
                reportInvalidAnnotationsOnUse(type, tree);
            }
        }
        // Set checkTopLevelDeclaredType to true, because the next time visitDeclared is called,
        // the type isn't the top level, so always do the check.
        checkTopLevelDeclaredOrPrimitiveType = true;

        if (TreeUtils.isClassTree(tree)) {
            markVisited(type, null);
            visitClassTypeParameters(type, (ClassTree) tree);
            return null;
        }

        /*
         * Try to reconstruct the ParameterizedTypeTree from the given tree.
         * TODO: there has to be a nicer way to do this...
         */
        IPair<ParameterizedTypeTree, AnnotatedDeclaredType> p =
                extractParameterizedTypeTree(tree, type);
        ParameterizedTypeTree typeArgTree = p.first;
        type = p.second;

        // Validate the type arguments of any explicitly-written enclosing types, e.g. the
        // `Outer<...>` in `Outer<...>.Inner`. The scan below (and super.visitDeclared) only
        // reaches a type's own direct type arguments; an enclosing type's arguments are buried
        // in a MemberSelectTree and would otherwise never be checked against their bounds.
        validateEnclosingTypeArgs(type, tree);

        if (typeArgTree == null) {
            return super.visitDeclared(type, tree);
        } // else

        // We put this here because we don't want to put it in visitedNodes before calling
        // super (in the else branch) because that would cause the super implementation
        // to detect that we've already visited type and to immediately return.
        markVisited(type, null);

        // We have a ParameterizedTypeTree -> visit it.

        visitParameterizedType(type, typeArgTree);

        /*
         * Instead of calling super with the unchanged "tree", adapt the
         * second argument to be the corresponding type argument tree. This
         * ensures that the first and second parameter to this method always
         * correspond. visitDeclared is the only method that had this
         * problem.
         */
        List<? extends AnnotatedTypeMirror> tatypes = type.getTypeArguments();

        if (tatypes == null) {
            return null;
        }

        // May be zero for a "diamond" (inferred type args in constructor invocation).
        int numTypeArgs = typeArgTree.getTypeArguments().size();
        if (numTypeArgs != 0) {
            // TODO: this should be an equality, but in the past it failed with:
            //   daikon/Debug.java; message: size mismatch for type arguments:
            //   @NonNull Object and Class<?>
            // but I didn't manage to reduce it to a test case.
            assert tatypes.size() <= numTypeArgs || skipChecks
                    : "size mismatch for type arguments: " + type + " and " + typeArgTree;

            for (int i = 0; i < tatypes.size(); ++i) {
                scan(tatypes.get(i), typeArgTree.getTypeArguments().get(i));
            }
        }

        // Don't call the super version, because it creates a mismatch
        // between the first and second parameters.
        // return super.visitDeclared(type, tree);

        return null;
    }

    /**
     * Visits the type parameters of a class tree.
     *
     * @param type type of {@code tree}
     * @param tree a class tree
     */
    protected void visitClassTypeParameters(AnnotatedDeclaredType type, ClassTree tree) {
        for (int i = 0, size = type.getTypeArguments().size(); i < size; i++) {
            AnnotatedTypeVariable typeParameter =
                    (AnnotatedTypeVariable) type.getTypeArguments().get(i);
            TypeParameterTree typeParameterTree = tree.getTypeParameters().get(i);
            scan(typeParameter, typeParameterTree);
        }
    }

    /**
     * Visits type parameter bounds.
     *
     * @param typeParameter type of {@code typeParameterTree}
     * @param typeParameterTree a type parameter tree
     */
    protected void visitTypeParameterBounds(
            AnnotatedTypeVariable typeParameter, TypeParameterTree typeParameterTree) {
        List<? extends Tree> boundTrees = typeParameterTree.getBounds();
        if (boundTrees.size() == 1) {
            scan(typeParameter.getUpperBound(), boundTrees.get(0));
        } else if (boundTrees.size() == 0) {
            // The upper bound is implicitly Object
            scan(typeParameter.getUpperBound(), typeParameterTree);
        } else {
            AnnotatedIntersectionType intersectionType =
                    (AnnotatedIntersectionType) typeParameter.getUpperBound();
            for (int j = 0; j < intersectionType.getBounds().size(); j++) {
                scan(intersectionType.getBounds().get(j), boundTrees.get(j));
            }
        }
    }

    /**
     * If {@code tree} has a {@link ParameterizedTypeTree}, then the tree and its type is returned.
     * Otherwise null and {@code type} are returned.
     *
     * @param tree tree to search
     * @param type type to return if no {@code ParameterizedTypeTree} is found
     * @return if {@code tree} has a {@code ParameterizedTypeTree}, then returns the tree and its
     *     type. Otherwise, returns null and {@code type}.
     */
    private IPair<@Nullable ParameterizedTypeTree, AnnotatedDeclaredType>
            extractParameterizedTypeTree(Tree tree, AnnotatedDeclaredType type) {
        ParameterizedTypeTree typeargtree = null;

        switch (tree.getKind()) {
            case VARIABLE:
                Tree lt = ((VariableTree) tree).getType();
                if (lt instanceof ParameterizedTypeTree) {
                    typeargtree = (ParameterizedTypeTree) lt;
                } else {
                    // System.out.println("Found a: " + lt);
                }
                break;
            case PARAMETERIZED_TYPE:
                typeargtree = (ParameterizedTypeTree) tree;
                break;
            case NEW_CLASS:
                NewClassTree nct = (NewClassTree) tree;
                ExpressionTree nctid = nct.getIdentifier();
                if (nctid instanceof ParameterizedTypeTree) {
                    typeargtree = (ParameterizedTypeTree) nctid;
                    /*
                     * This is quite tricky... for anonymous class instantiations,
                     * the type at this point has no type arguments. By doing the
                     * following, we get the type arguments again.
                     */
                    type = (AnnotatedDeclaredType) atypeFactory.getAnnotatedType(typeargtree);
                }
                break;
            case ANNOTATED_TYPE:
                AnnotatedTypeTree tr = (AnnotatedTypeTree) tree;
                ExpressionTree undtr = tr.getUnderlyingType();
                if (undtr instanceof ParameterizedTypeTree) {
                    typeargtree = (ParameterizedTypeTree) undtr;
                } else if (undtr instanceof IdentifierTree) {
                    // @Something D -> Nothing to do
                } else {
                    // TODO: add more test cases to ensure that nested types are
                    // handled correctly,
                    // e.g. @Nullable() List<@Nullable Object>[][]
                    IPair<ParameterizedTypeTree, AnnotatedDeclaredType> p =
                            extractParameterizedTypeTree(undtr, type);
                    typeargtree = p.first;
                    type = p.second;
                }
                break;
            case IDENTIFIER:
            case ARRAY_TYPE:
            case NEW_ARRAY:
            case MEMBER_SELECT:
            case UNBOUNDED_WILDCARD:
            case EXTENDS_WILDCARD:
            case SUPER_WILDCARD:
            case TYPE_PARAMETER:
                // Nothing to do.
                break;
            case METHOD:
                // If a MethodTree is passed, it's just the return type that is validated.
                // See BaseTypeVisitor#validateTypeOf.
                MethodTree methodTree = (MethodTree) tree;
                if (methodTree.getReturnType() instanceof ParameterizedTypeTree) {
                    typeargtree = (ParameterizedTypeTree) methodTree.getReturnType();
                }
                break;
            default:
                // The parameterized type is the result of some expression tree.
                // No need to do anything further.
                break;
        }

        return IPair.of(typeargtree, type);
    }

    @Override
    @SuppressWarnings(
            "signature:argument.type.incompatible") // PrimitiveType.toString(): @PrimitiveType
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        if (!checkTopLevelDeclaredOrPrimitiveType
                || checker.shouldSkipUses(type.getUnderlyingType().toString())) {
            return super.visitPrimitive(type, tree);
        }

        if (!visitor.isValidUse(type, tree)) {
            reportInvalidAnnotationsOnUse(type, tree);
        }

        return super.visitPrimitive(type, tree);
    }

    @Override
    public Void visitArray(AnnotatedArrayType type, Tree tree) {
        AnnotatedTypeMirror comp = AnnotatedTypes.innerMostType(type);

        if (comp.getKind() == TypeKind.DECLARED
                && checker.shouldSkipUses(
                        ((AnnotatedDeclaredType) comp).getUnderlyingType().asElement())) {
            return super.visitArray(type, tree);
        }

        if (!visitor.isValidUse(type, tree)) {
            reportInvalidAnnotationsOnUse(type, tree);
        }

        return super.visitArray(type, tree);
    }

    /**
     * Validates the type arguments of the explicitly-written enclosing types of a declared type,
     * for example the {@code Outer<...>} part of a qualified type {@code Outer<...>.Inner}.
     *
     * <p>{@link #visitDeclared} and {@link AnnotatedTypeScanner#visitDeclared} only reach a
     * declared type's own direct type arguments. An enclosing type's type arguments live inside a
     * {@link MemberSelectTree} and, without this method, are never checked against the enclosing
     * type parameters' bounds: {@code Outer<@NonNull String>.Inner} would be accepted even when
     * {@code @NonNull String} violates the bound of {@code Outer}'s type parameter, whereas the
     * same argument in the non-enclosing position {@code Outer<@NonNull String>} is rejected.
     *
     * <p>This method walks the enclosing-type/qualifier chain outward and runs {@link
     * #visitParameterizedType} on each enclosing type that is written with explicit type arguments
     * (a {@link ParameterizedTypeTree}). Enclosing types written without type arguments (raw types,
     * or an inner type named by a simple identifier with the enclosing arguments left implicit)
     * have no argument tree to check and are skipped, matching the direct-position behavior.
     *
     * <p>For most tree shapes, each enclosing type checked is read off {@code type}'s own
     * enclosing-type chain ({@link AnnotatedDeclaredType#getEnclosingType()}), matching the
     * qualifier chain in the tree level for level. The exception is an unqualified class instance
     * creation (e.g. {@code new Outer<...>.Inner()}): there, {@code type}'s enclosing type is the
     * *receiver* used to viewpoint-adapt the constructor invocation (see {@link
     * AnnotatedTypeFactory#getConstructorReceiverType}), which need not be, and need not carry the
     * annotations of, the type actually written in the {@code new} expression. In that case, every
     * enclosing type checked is instead derived directly from its own written tree via {@link
     * AnnotatedTypeFactory#getAnnotatedTypeFromTypeTree}.
     *
     * @param type the declared type whose enclosing types should be validated
     * @param tree the tree for {@code type}; besides a type-use tree, this may also be a {@link
     *     MethodTree} (return type validation, see {@link BaseTypeVisitor#validateTypeOf}) or a
     *     {@link NewClassTree} (the instantiated type), matching what {@link
     *     #extractParameterizedTypeTree} accepts
     */
    protected void validateEnclosingTypeArgs(AnnotatedDeclaredType type, Tree tree) {
        if (type.getEnclosingType() == null) {
            return;
        }

        // An unqualified `new Outer<...>.Inner()` has no enclosing expression to supply a receiver
        // for viewpoint adaptation; `type`'s enclosing type is then some other applicable receiver
        // (e.g. the type of an enclosing `this`), not the type written in the `new` expression.
        boolean unqualifiedNewClass =
                tree instanceof NewClassTree
                        && ((NewClassTree) tree).getEnclosingExpression() == null;

        // Unwrap the type tree down to the tree that actually names the type: strip a surrounding
        // VariableTree or AnnotatedTypeTree, unwrap a MethodTree to its return type or a
        // NewClassTree to its instantiated type, then drop a top-level ParameterizedTypeTree's own
        // type arguments to reach the (possibly qualified) name.
        Tree nameTree = tree;
        while (true) {
            if (nameTree instanceof VariableTree) {
                nameTree = ((VariableTree) nameTree).getType();
            } else if (nameTree instanceof AnnotatedTypeTree) {
                nameTree = ((AnnotatedTypeTree) nameTree).getUnderlyingType();
            } else if (nameTree instanceof MethodTree) {
                // A constructor has no written return type to unwrap further.
                nameTree = ((MethodTree) nameTree).getReturnType();
                if (nameTree == null) {
                    return;
                }
            } else if (nameTree instanceof NewClassTree) {
                nameTree = ((NewClassTree) nameTree).getIdentifier();
            } else {
                break;
            }
        }
        if (nameTree instanceof ParameterizedTypeTree) {
            nameTree = ((ParameterizedTypeTree) nameTree).getType();
        }

        // Walk outward through the qualifier chain, validating each enclosing type that is written
        // with explicit type arguments.
        AnnotatedDeclaredType enclosing = unqualifiedNewClass ? null : type.getEnclosingType();
        while (nameTree instanceof MemberSelectTree) {
            ExpressionTree enclosingTree = ((MemberSelectTree) nameTree).getExpression();
            if (enclosingTree instanceof ParameterizedTypeTree) {
                ParameterizedTypeTree enclosingParamTree = (ParameterizedTypeTree) enclosingTree;
                AnnotatedDeclaredType enclosingType =
                        unqualifiedNewClass
                                ? (AnnotatedDeclaredType)
                                        atypeFactory.getAnnotatedTypeFromTypeTree(
                                                enclosingParamTree)
                                : enclosing;
                if (enclosingType != null) {
                    visitParameterizedType(enclosingType, enclosingParamTree);
                }
                nameTree = enclosingParamTree.getType();
            } else {
                // A raw or simple-name enclosing type has no argument tree to validate.
                nameTree = enclosingTree;
            }
            if (!unqualifiedNewClass) {
                enclosing = enclosing == null ? null : enclosing.getEnclosingType();
            }
        }
    }

    /**
     * Checks that the annotations on the type arguments supplied to a type or a method invocation
     * are within the bounds of the type variables as declared, and issues the
     * "type.argument.type.incompatible" error if they are not.
     *
     * @param type the type to check
     * @param tree the type's tree
     */
    protected Void visitParameterizedType(AnnotatedDeclaredType type, ParameterizedTypeTree tree) {
        // System.out.printf("TypeValidator.visitParameterizedType: type: %s, tree: %s%n", type,
        // tree);

        if (TreeUtils.isDiamondTree(tree)) {
            return null;
        }

        TypeElement element = (TypeElement) type.getUnderlyingType().asElement();
        if (checker.shouldSkipUses(element)) {
            return null;
        }

        AnnotatedDeclaredType capturedType =
                (AnnotatedDeclaredType) atypeFactory.applyCaptureConversion(type);
        List<AnnotatedTypeParameterBounds> bounds =
                atypeFactory.typeVariablesFromUse(capturedType, element);

        visitor.checkTypeArguments(
                tree,
                bounds,
                capturedType.getTypeArguments(),
                tree.getTypeArguments(),
                element.getSimpleName(),
                element.getTypeParameters());

        @SuppressWarnings(
                "interning:not.interned") // applyCaptureConversion returns the passed type if type
        // does not have wildcards.
        boolean hasCapturedTypeVariables = capturedType != type;
        if (!hasCapturedTypeVariables) {
            return null;
        }

        checkCapturedWildcardBounds(type, capturedType, tree);
        checkExplicitSuperBoundWildcards(type, capturedType, tree);

        return null;
    }

    /**
     * Rechecks, for every wildcard type argument whose capture is a captured type variable, that
     * the upper bound of the captured type variable is a subtype of the extends bound of the
     * wildcard, and issues the "type.argument.type.incompatible" error if it is not.
     *
     * <p>For most captured type variables, this will trivially hold, as capturing incorporated the
     * extends bound of the wildcard into the upper bound of the type variable. This will fail if
     * the bound and the wildcard have generic types and there is no appropriate glb, in which case
     * the two bounds have contradictory requirements and no type can satisfy both.
     *
     * <p>Checkers with a nonstandard subtyping relationship (where this recheck can spuriously fail
     * even though capture conversion itself succeeded) may override this method to do nothing.
     *
     * @param type the (possibly unconverted) parameterized type being validated
     * @param capturedType {@code type} after capture conversion
     * @param tree the tree for {@code type}
     */
    protected void checkCapturedWildcardBounds(
            AnnotatedDeclaredType type,
            AnnotatedDeclaredType capturedType,
            ParameterizedTypeTree tree) {
        TypeElement element = (TypeElement) type.getUnderlyingType().asElement();

        // Check that the extends bound of the captured type variable is a subtype of the
        // extends bound of the wildcard.
        int numTypeArgs = capturedType.getTypeArguments().size();
        // First create a mapping from captured type variable to its wildcard.
        Map<TypeVariable, AnnotatedTypeMirror> typeVarToWildcard =
                ArrayMap.newArrayMapOrHashMap(numTypeArgs);
        for (int i = 0; i < numTypeArgs; i++) {
            AnnotatedTypeMirror captureTypeArg = capturedType.getTypeArguments().get(i);
            if (TypesUtils.isCapturedTypeVariable(captureTypeArg.getUnderlyingType())
                    && type.getTypeArguments().get(i).getKind() == TypeKind.WILDCARD) {
                AnnotatedTypeVariable capturedTypeVar = (AnnotatedTypeVariable) captureTypeArg;
                AnnotatedWildcardType wildcard =
                        (AnnotatedWildcardType) type.getTypeArguments().get(i);
                typeVarToWildcard.put(capturedTypeVar.getUnderlyingType(), wildcard);
            }
        }

        for (int i = 0; i < numTypeArgs; i++) {
            if (type.getTypeArguments().get(i).getKind() != TypeKind.WILDCARD) {
                continue;
            }
            AnnotatedTypeMirror captureTypeArg = capturedType.getTypeArguments().get(i);
            AnnotatedWildcardType wildcard = (AnnotatedWildcardType) type.getTypeArguments().get(i);
            if (!TypesUtils.isCapturedTypeVariable(captureTypeArg.getUnderlyingType())) {
                continue;
            }
            AnnotatedTypeVariable capturedTypeVar = (AnnotatedTypeVariable) captureTypeArg;
            // Substitute the captured type variables with their wildcards. Without
            // this, the isSubtype check crashes because wildcards aren't comparable
            // with type variables.
            AnnotatedTypeMirror captureTypeVarUB =
                    atypeFactory
                            .getTypeVarSubstitutor()
                            .substituteWithoutCopyingTypeArguments(
                                    typeVarToWildcard, capturedTypeVar.getUpperBound());
            if (!atypeFactory
                    .getTypeHierarchy()
                    .isSubtype(captureTypeVarUB, wildcard.getExtendsBound())) {
                // For most captured type variables, this will trivially hold, as capturing
                // incorporated the extends bound of the wildcard into the upper bound of the
                // type variable.
                // This will fail if the bound and the wildcard have generic types and there is
                // no appropriate GLB.
                // This issues an error for types that cannot be satisfied, because the two
                // bounds have contradictory requirements.
                checker.reportError(
                        tree.getTypeArguments().get(i),
                        "type.argument.type.incompatible",
                        element.getTypeParameters().get(i),
                        element.getSimpleName(),
                        wildcard.getExtendsBound(),
                        capturedTypeVar.getUpperBound());
            }
        }
    }

    /**
     * Checks, for every wildcard type argument whose capture is not itself a captured type
     * variable, the special case described by JDK-8054309: if the super bound of the wildcard is
     * the same as the upper bound of the type parameter, then javac uses the bound rather than
     * creating a fresh type variable. (See https://bugs.openjdk.org/browse/JDK-8054309.) In this
     * case, the Checker Framework uses the annotations on the super bound of the wildcard and
     * ignores the annotations on the extends bound. For example, {@code Set<@1 ? super @2 Object>}
     * will collapse into {@code Set<@2 Object>}. This method issues the
     * "type.invalid.super.wildcard" error if the annotations on the extends bound are not the same
     * as the annotations on the super bound.
     *
     * @param type the (possibly unconverted) parameterized type being validated
     * @param capturedType {@code type} after capture conversion
     * @param tree the tree for {@code type}
     */
    protected void checkExplicitSuperBoundWildcards(
            AnnotatedDeclaredType type,
            AnnotatedDeclaredType capturedType,
            ParameterizedTypeTree tree) {
        int numTypeArgs = capturedType.getTypeArguments().size();
        for (int i = 0; i < numTypeArgs; i++) {
            if (type.getTypeArguments().get(i).getKind() != TypeKind.WILDCARD) {
                continue;
            }
            AnnotatedTypeMirror captureTypeArg = capturedType.getTypeArguments().get(i);
            AnnotatedWildcardType wildcard = (AnnotatedWildcardType) type.getTypeArguments().get(i);
            if (TypesUtils.isCapturedTypeVariable(captureTypeArg.getUnderlyingType())) {
                continue;
            }
            if (AnnotatedTypes.hasExplicitSuperBound(wildcard)) {
                // If the super bound of the wildcard is the same as the upper bound of the
                // type parameter, then javac uses the bound rather than creating a fresh
                // type variable.
                // (See https://bugs.openjdk.org/browse/JDK-8054309.)
                // In this case, the Checker Framework uses the annotations on the super
                // bound of the wildcard and ignores the annotations on the extends bound.
                // For example, Set<@1 ? super @2 Object> will collapse into Set<@2 Object>.
                // So, issue a warning if the annotations on the extends bound are not the
                // same as the annotations on the super bound.
                if (!(atypeFactory
                                .getTypeHierarchy()
                                .isSubtypeShallowEffective(
                                        wildcard.getSuperBound(), wildcard.getExtendsBound())
                        && atypeFactory
                                .getTypeHierarchy()
                                .isSubtypeShallowEffective(
                                        wildcard.getExtendsBound(), wildcard.getSuperBound()))) {
                    checker.reportError(
                            tree.getTypeArguments().get(i),
                            "type.invalid.super.wildcard",
                            wildcard.getExtendsBound(),
                            wildcard.getSuperBound());
                }
            }
        }
    }

    @Override
    public Void visitTypeVariable(AnnotatedTypeVariable type, Tree tree) {
        if (hasVisited(type)) {
            return getVisited(type);
        }

        if (type.isDeclaration()) {
            validateTypeParameterTargetLocations(type, tree);
            if (!areBoundsValid(type.getUpperBound(), type.getLowerBound())) {
                reportInvalidBounds(type, tree);
            }
        }
        AnnotatedTypeVariable useOfTypeVar = type.asUse();
        if (tree instanceof TypeParameterTree) {
            TypeParameterTree typeParameterTree = (TypeParameterTree) tree;
            markVisited(useOfTypeVar, defaultResult);
            visitTypeParameterBounds(useOfTypeVar, typeParameterTree);
            markVisited(useOfTypeVar, defaultResult);
            return null;
        }
        return super.visitTypeVariable(useOfTypeVar, tree);
    }

    @Override
    public Void visitWildcard(AnnotatedWildcardType type, Tree tree) {
        if (hasVisited(type)) {
            return getVisited(type);
        }

        validateWildcardTargetLocations(type, tree);
        if (!areBoundsValid(type.getExtendsBound(), type.getSuperBound())) {
            reportInvalidBounds(type, tree);
        }
        return super.visitWildcard(type, tree);
    }

    /**
     * Validates whether the qualifiers on the tree are at the correct type-use locations, as
     * specified by the meta-annotation {@link org.checkerframework.framework.qual.TargetLocations}.
     *
     * <p>More specifically, this method only checks qualifiers on a VariableTree and thus checks
     * for the following type-use locations: FIELD, LOCAL_VARIABLE, RESOURCE_VARIABLE,
     * EXCEPTION_PARAMETER, PARAMETER, RECEIVER and CONSTRUCTOR_RESULT.
     *
     * <p>The other two validate methods achieve the same goal but perform checks on different trees
     * and different type-use locations. This separation exists because variables can automatically
     * infer their type-use location from their {@link javax.lang.model.element.ElementKind}. By
     * contrast, other constructs (like method returns or type bounds) have context-dependent
     * locations that must be explicitly provided by the caller, and wildcards do not have an
     * element. See {@link #validateTargetLocation(AnnotatedTypeMirror, Tree, TypeUseLocation)} and
     * {@link #validateWildcardTargetLocations(AnnotatedWildcardType, Tree)}.
     *
     * @param type the type of the tree
     * @param tree the tree whose qualifiers are to be validated
     * @see #validateTargetLocation(AnnotatedTypeMirror, Tree, TypeUseLocation)
     * @see #validateWildcardTargetLocations(AnnotatedWildcardType, Tree)
     */
    @Override
    public void validateVariableTargetLocation(AnnotatedTypeMirror type, Tree tree) {
        if (ignoreTargetLocations || noQualHasTargetLocations) {
            return;
        }
        Element element = TreeUtils.elementFromTree(tree);

        if (element != null) {
            ElementKind elemKind = element.getKind();
            // TypeUseLocation.java doesn't have ENUM type use location right now.
            for (AnnotationMirror am : type.getAnnotations()) {
                List<TypeUseLocation> locations =
                        qualAllowedLocations.get(AnnotationUtils.annotationName(am));
                if (locations == null || locations.contains(TypeUseLocation.ALL)) {
                    continue;
                }
                boolean issueError = true;
                switch (elemKind) {
                    case LOCAL_VARIABLE:
                        if (locations.contains(TypeUseLocation.LOCAL_VARIABLE)) {
                            issueError = false;
                        }
                        break;
                    case EXCEPTION_PARAMETER:
                        if (locations.contains(TypeUseLocation.EXCEPTION_PARAMETER)) {
                            issueError = false;
                        }
                        break;
                    case PARAMETER:
                        if (InternalUtils.isThisName(((VariableTree) tree).getName())) {
                            if (locations.contains(TypeUseLocation.RECEIVER)) {
                                issueError = false;
                            }
                        } else {
                            if (locations.contains(TypeUseLocation.PARAMETER)) {
                                issueError = false;
                            }
                        }
                        break;
                    case RESOURCE_VARIABLE:
                        if (locations.contains(TypeUseLocation.RESOURCE_VARIABLE)) {
                            issueError = false;
                        }
                        break;
                    case FIELD:
                        if (locations.contains(TypeUseLocation.FIELD)) {
                            issueError = false;
                        }
                        break;
                    case ENUM_CONSTANT:
                        if (locations.contains(TypeUseLocation.FIELD)
                                || locations.contains(TypeUseLocation.CONSTRUCTOR_RESULT)) {
                            issueError = false;
                        }
                        break;
                    default:
                        throw new BugInCF("Location not matched");
                }
                if (issueError) {
                    checker.reportError(
                            tree,
                            "type.invalid.annotations.on.location",
                            am.toString(),
                            element.getKind().name());
                }
            }
        }
    }

    /**
     * Validates whether the qualifiers on the type parameter bounds are at the correct type-use
     * locations, as specified by the meta-annotation {@link
     * org.checkerframework.framework.qual.TargetLocations}.
     *
     * <p>More specifically, this method checks qualifiers on a TypeParameterTree for the {@link
     * TypeUseLocation#UPPER_BOUND} and {@link TypeUseLocation#LOWER_BOUND} locations.
     *
     * @param type the type variable declaration whose bounds are to be validated
     * @param tree the tree of this type parameter
     * @see #validateWildcardTargetLocations(AnnotatedWildcardType, Tree)
     * @see #validateVariableTargetLocation(AnnotatedTypeMirror, Tree)
     * @see #validateTargetLocation(AnnotatedTypeMirror, Tree, TypeUseLocation)
     * @see #stripInvalidLocationQualifiersFromTypeVariableBounds
     * @see #shouldStripInvalidLocationQualifiers
     */
    protected void validateTypeParameterTargetLocations(AnnotatedTypeVariable type, Tree tree) {
        if (ignoreTargetLocations
                || (noQualHasTargetLocations && !shouldStripInvalidLocationQualifiers())) {
            return;
        }

        for (AnnotationMirror am :
                annotationsDisallowedAtLocation(
                        type.getUpperBound(), TypeUseLocation.UPPER_BOUND)) {
            checker.reportError(
                    tree,
                    "type.invalid.annotations.on.location",
                    am.toString(),
                    TypeUseLocation.UPPER_BOUND.toString());
        }

        for (AnnotationMirror am :
                annotationsDisallowedAtLocation(
                        type.getLowerBound(), TypeUseLocation.LOWER_BOUND)) {
            checker.reportError(
                    tree,
                    "type.invalid.annotations.on.location",
                    am.toString(),
                    TypeUseLocation.LOWER_BOUND.toString());
        }

        if (shouldStripInvalidLocationQualifiers()) {
            stripInvalidLocationQualifiersFromTypeVariableBounds(
                    type, type.getUpperBound(), type.getLowerBound(), tree);
        }
    }

    /**
     * Validates whether the qualifiers on the tree are at the correct type-use locations, as
     * specified by the meta-annotation {@link org.checkerframework.framework.qual.TargetLocations}.
     *
     * <p>This method checks qualifiers for context-dependent locations such as {@link
     * TypeUseLocation#CONSTRUCTOR_RESULT} and {@link TypeUseLocation#RETURN} on Method trees, or
     * other caller-specified locations.
     *
     * <p>The other validate methods achieve the same goal but perform checks on specific trees and
     * their associated type-use locations: {@link
     * #validateVariableTargetLocation(AnnotatedTypeMirror, Tree)}, {@link
     * #validateWildcardTargetLocations(AnnotatedWildcardType, Tree)}, and {@link
     * #validateTypeParameterTargetLocations(AnnotatedTypeVariable, Tree)}.
     *
     * @param type the type of the tree
     * @param tree the tree whose qualifiers are to be validated
     * @param required the required TypeUseLocation. If it is not present in the specification of
     *     the meta-annotation ({@link org.checkerframework.framework.qual.TargetLocations}), issue
     *     an error.
     * @see #validateVariableTargetLocation(AnnotatedTypeMirror, Tree)
     * @see #validateWildcardTargetLocations(AnnotatedWildcardType, Tree)
     * @see #validateTypeParameterTargetLocations(AnnotatedTypeVariable, Tree)
     */
    @Override
    public void validateTargetLocation(
            AnnotatedTypeMirror type, Tree tree, TypeUseLocation required) {
        if (ignoreTargetLocations || noQualHasTargetLocations) {
            return;
        }
        for (AnnotationMirror am : annotationsDisallowedAtLocation(type, required)) {
            checker.reportError(
                    tree,
                    "type.invalid.annotations.on.location",
                    am.toString(),
                    required.toString());
        }
    }

    /**
     * Returns the primary annotations on {@code type} that are not permitted at the given type-use
     * location, according to their {@link org.checkerframework.framework.qual.TargetLocations}
     * meta-annotation. A qualifier without a {@code TargetLocations} meta-annotation, or one that
     * lists {@link TypeUseLocation#ALL}, is permitted everywhere and is never returned.
     *
     * @param type the type whose primary annotations to check
     * @param required the type-use location at which {@code type} appears
     * @return the primary annotations on {@code type} that {@code required} does not permit
     * @see #annotationsDisallowedAtLocation(AnnotatedTypeMirror, Set)
     * @see #stripInvalidLocationQualifiersFromTypeVariableBounds
     * @see #shouldStripInvalidLocationQualifiers
     */
    protected List<AnnotationMirror> annotationsDisallowedAtLocation(
            AnnotatedTypeMirror type, TypeUseLocation required) {
        List<AnnotationMirror> result = Collections.emptyList();
        for (AnnotationMirror am : type.getAnnotations()) {
            List<TypeUseLocation> locations =
                    qualAllowedLocations.get(AnnotationUtils.annotationName(am));
            if (locations == null || locations.contains(TypeUseLocation.ALL)) {
                continue;
            }
            if (!locations.contains(required)) {
                if (result.isEmpty()) {
                    result = new ArrayList<>(1);
                }
                result.add(am);
            }
        }
        return result;
    }

    /**
     * Returns the primary annotations on {@code type} that are not permitted at any of the given
     * type-use locations, according to their {@link
     * org.checkerframework.framework.qual.TargetLocations} meta-annotation. A qualifier without a
     * {@code TargetLocations} meta-annotation, or one that lists {@link TypeUseLocation#ALL}, is
     * permitted everywhere and is never returned.
     *
     * @param type the type whose primary annotations to check
     * @param allowedLocations the type-use locations {@code type} may be annotated at
     * @return the primary annotations on {@code type} that {@code allowedLocations} does not permit
     * @see #annotationsDisallowedAtLocation(AnnotatedTypeMirror, TypeUseLocation)
     * @see #additionalAnnotationsToStripFromWildcardBound
     */
    protected List<AnnotationMirror> annotationsDisallowedAtLocation(
            AnnotatedTypeMirror type, Set<TypeUseLocation> allowedLocations) {
        List<AnnotationMirror> result = Collections.emptyList();
        for (AnnotationMirror am : type.getAnnotations()) {
            List<TypeUseLocation> locations =
                    qualAllowedLocations.get(AnnotationUtils.annotationName(am));
            if (locations == null || containsAny(locations, allowedLocations)) {
                continue;
            }
            if (result.isEmpty()) {
                result = new ArrayList<>(1);
            }
            result.add(am);
        }
        return result;
    }

    /**
     * Returns true if the effective annotations on the upperBound are above (or equal to) those on
     * the lowerBound.
     *
     * @param upperBound the upper bound to check
     * @param lowerBound the lower bound to check
     * @return true if the effective annotations on the upperBound are above (or equal to) those on
     *     the lowerBound
     */
    protected boolean areBoundsValid(
            AnnotatedTypeMirror upperBound, AnnotatedTypeMirror lowerBound) {
        AnnotationMirrorSet upperBoundAnnos =
                AnnotatedTypes.findEffectiveAnnotations(qualHierarchy, upperBound);
        AnnotationMirrorSet lowerBoundAnnos =
                AnnotatedTypes.findEffectiveAnnotations(qualHierarchy, lowerBound);

        if (upperBoundAnnos.size() == lowerBoundAnnos.size()) {
            return atypeFactory
                    .getTypeHierarchy()
                    .isSubtypeShallowEffective(lowerBound, upperBound);
        } else {
            // When upperBoundAnnos.size() != lowerBoundAnnos.size() one of the two bound types will
            // be reported as invalid.  Therefore, we do not do any other comparisons nor do we
            // report a bound.
            return true;
        }
    }

    /**
     * Removes from the bounds of a type-variable declaration any primary annotation that its {@link
     * org.checkerframework.framework.qual.TargetLocations} does not permit at that bound (or that
     * {@link #additionalAnnotationsToStripFromTypeVariableBound} specifies to strip), then
     * re-defaults the now-bare positions. Called only when the checker opts in via {@link
     * #shouldStripInvalidLocationQualifiers}.
     *
     * <p>{@code addDefaultAnnotations} fills only positions that are missing an annotation, so it
     * re-defaults exactly the stripped bound(s) with the correct bound context and leaves every
     * other qualifier untouched.
     *
     * @param type the type-variable declaration whose bounds to fix up and re-default
     * @param upperBound {@code type}'s upper bound
     * @param lowerBound {@code type}'s lower bound
     * @param tree the tree for {@code type}'s declaration
     * @see #additionalAnnotationsToStripFromTypeVariableBound
     * @see #validateWildcardTargetLocations
     * @see #additionalAnnotationsToStripFromWildcardBound
     * @see #shouldStripInvalidLocationQualifiers
     */
    protected void stripInvalidLocationQualifiersFromTypeVariableBounds(
            AnnotatedTypeVariable type,
            AnnotatedTypeMirror upperBound,
            AnnotatedTypeMirror lowerBound,
            Tree tree) {
        boolean stripped = false;
        for (AnnotationMirror am :
                annotationsDisallowedAtLocation(upperBound, TypeUseLocation.UPPER_BOUND)) {
            upperBound.removeAnnotation(am);
            stripped = true;
        }
        for (AnnotationMirror am :
                additionalAnnotationsToStripFromTypeVariableBound(
                        type, tree, upperBound, TypeUseLocation.UPPER_BOUND)) {
            upperBound.removeAnnotation(am);
            stripped = true;
        }
        for (AnnotationMirror am :
                annotationsDisallowedAtLocation(lowerBound, TypeUseLocation.LOWER_BOUND)) {
            lowerBound.removeAnnotation(am);
            stripped = true;
        }
        for (AnnotationMirror am :
                additionalAnnotationsToStripFromTypeVariableBound(
                        type, tree, lowerBound, TypeUseLocation.LOWER_BOUND)) {
            lowerBound.removeAnnotation(am);
            stripped = true;
        }
        if (stripped) {
            atypeFactory.addDefaultAnnotations(type);
        }
    }

    /**
     * Returns additional annotations to strip from {@code bound} (one of {@code type}'s upper or
     * lower bound), beyond whatever {@link #annotationsDisallowedAtLocation(AnnotatedTypeMirror,
     * TypeUseLocation)} already found. Called only when the checker opts in via {@link
     * #shouldStripInvalidLocationQualifiers}. The default returns an empty list, so opted-in
     * checkers whose qualifiers use {@code @TargetLocations} are unaffected ({@code
     * annotationsDisallowedAtLocation} already covers their case).
     *
     * <p>{@code tree} is the type-parameter declaration's own tree (as received by {@link
     * #visitTypeVariable}). A checker whose validity check is tree-based instead (e.g., it must
     * tell an explicitly written annotation apart from the same qualifier arriving through
     * defaulting, which {@code @TargetLocations} cannot express) can override this method, inspect
     * {@code tree}, and return additional annotations on {@code bound} to strip.
     *
     * @param type the type-variable declaration whose bounds are being validated
     * @param tree the tree for {@code type}'s declaration
     * @param bound {@code type}'s upper or lower bound
     * @param location {@link TypeUseLocation#UPPER_BOUND} or {@link TypeUseLocation#LOWER_BOUND}
     * @return additional annotations on {@code bound} to strip
     * @see #stripInvalidLocationQualifiersFromTypeVariableBounds
     * @see #additionalAnnotationsToStripFromWildcardBound
     * @see #annotationsDisallowedAtLocation(AnnotatedTypeMirror, TypeUseLocation)
     * @see #shouldStripInvalidLocationQualifiers
     */
    protected List<AnnotationMirror> additionalAnnotationsToStripFromTypeVariableBound(
            AnnotatedTypeVariable type,
            Tree tree,
            AnnotatedTypeMirror bound,
            TypeUseLocation location) {
        return Collections.emptyList();
    }

    /** The type-use locations permissible for wildcard super bounds. */
    private static final Set<TypeUseLocation> WILDCARD_SUPER_BOUND_LOCATIONS =
            EnumSet.of(
                    TypeUseLocation.ALL,
                    TypeUseLocation.LOWER_BOUND,
                    TypeUseLocation.IMPLICIT_LOWER_BOUND,
                    TypeUseLocation.EXPLICIT_LOWER_BOUND);

    /** The type-use locations permissible for wildcard extends bounds. */
    private static final Set<TypeUseLocation> WILDCARD_EXTENDS_BOUND_LOCATIONS =
            EnumSet.of(
                    TypeUseLocation.ALL,
                    TypeUseLocation.UPPER_BOUND,
                    TypeUseLocation.IMPLICIT_UPPER_BOUND,
                    TypeUseLocation.EXPLICIT_UPPER_BOUND);

    /**
     * Validates whether the qualifiers on the wildcard tree are at the correct type-use locations,
     * as specified by the meta-annotation {@link
     * org.checkerframework.framework.qual.TargetLocations}.
     *
     * <p>More specifically, this method only checks qualifiers on a WildcardTree and thus checks
     * for the following type-use locations: (EXPLICIT/IMPLICIT) LOWER_BOUND and (EXPLICIT/IMPLICIT)
     * UPPER_BOUND.
     *
     * <p>The other two validate methods achieve the same goal but perform checks on different trees
     * and different type-use locations. This separation exists because wildcards do not have an
     * element and determine their locations based on their bounds. By contrast, variables can
     * automatically infer their type-use location from their ElementKind, and other constructs have
     * context-dependent locations that must be explicitly provided by the caller. See {@link
     * #validateVariableTargetLocation(AnnotatedTypeMirror, Tree)} and {@link
     * #validateTargetLocation(AnnotatedTypeMirror, Tree, TypeUseLocation)}.
     *
     * @param type the type to check
     * @param tree the tree of this type
     * @see #validateVariableTargetLocation(AnnotatedTypeMirror, Tree)
     * @see #validateTargetLocation(AnnotatedTypeMirror, Tree, TypeUseLocation)
     * @see #stripInvalidLocationQualifiersFromTypeVariableBounds
     * @see #additionalAnnotationsToStripFromWildcardBound
     * @see #shouldStripInvalidLocationQualifiers
     */
    protected void validateWildcardTargetLocations(AnnotatedWildcardType type, Tree tree) {
        // noQualHasTargetLocations is a pure optimization for the common case (skip a check that
        // can never find anything). It must not also skip a checker that has opted in to
        // stripping via shouldStripInvalidLocationQualifiers: such a checker may override
        // additionalAnnotationsToStripFromWildcardBound with its own tree-based detection,
        // independent of
        // @TargetLocations, in which case no qualifier having @TargetLocations says nothing about
        // whether there is something to detect and strip.
        if (ignoreTargetLocations
                || (noQualHasTargetLocations && !shouldStripInvalidLocationQualifiers())) {
            return;
        }

        boolean strip = shouldStripInvalidLocationQualifiers();

        List<AnnotationMirror> superDisallowed =
                annotationsDisallowedAtLocation(
                        type.getSuperBound(), WILDCARD_SUPER_BOUND_LOCATIONS);
        for (AnnotationMirror am : superDisallowed) {
            checker.reportError(
                    tree, "type.invalid.annotations.on.location", am.toString(), "SUPER_WILDCARD");
        }

        List<AnnotationMirror> extendsDisallowed =
                annotationsDisallowedAtLocation(
                        type.getExtendsBound(), WILDCARD_EXTENDS_BOUND_LOCATIONS);
        for (AnnotationMirror am : extendsDisallowed) {
            checker.reportError(
                    tree,
                    "type.invalid.annotations.on.location",
                    am.toString(),
                    "EXTENDS_WILDCARD");
        }

        if (strip) {
            // Make the location-invalid qualifiers inert: remove them and re-default the now-bare
            // bounds. addDefaultAnnotations only fills positions missing an annotation, so it
            // re-defaults exactly the stripped bound(s) and leaves every other qualifier untouched.
            // visitWildcard runs this before areBoundsValid, so no bound.type.incompatible cascade
            // is reported for the stripped qualifier.
            stripInvalidLocationQualifiersFromWildcardBounds(
                    type, tree, superDisallowed, extendsDisallowed);
        }
    }

    /**
     * Removes from the bounds of a wildcard type any primary annotation that its {@link
     * org.checkerframework.framework.qual.TargetLocations} does not permit at that bound (or that
     * {@link #additionalAnnotationsToStripFromWildcardBound} specifies to strip), then re-defaults
     * the now-bare positions. Called only when the checker opts in via {@link
     * #shouldStripInvalidLocationQualifiers}.
     *
     * <p>{@code addDefaultAnnotations} fills only positions that are missing an annotation, so it
     * re-defaults exactly the stripped bound(s) with the correct bound context and leaves every
     * other qualifier untouched.
     *
     * @param type the wildcard type whose bounds to fix up and re-default
     * @param tree the tree for {@code type}
     * @see #stripInvalidLocationQualifiersFromTypeVariableBounds
     * @see #additionalAnnotationsToStripFromWildcardBound
     * @see #shouldStripInvalidLocationQualifiers
     */
    protected void stripInvalidLocationQualifiersFromWildcardBounds(
            AnnotatedWildcardType type, Tree tree) {
        List<AnnotationMirror> superDisallowed =
                annotationsDisallowedAtLocation(
                        type.getSuperBound(), WILDCARD_SUPER_BOUND_LOCATIONS);
        List<AnnotationMirror> extendsDisallowed =
                annotationsDisallowedAtLocation(
                        type.getExtendsBound(), WILDCARD_EXTENDS_BOUND_LOCATIONS);
        stripInvalidLocationQualifiersFromWildcardBounds(
                type, tree, superDisallowed, extendsDisallowed);
    }

    /**
     * Strips the specified disallowed annotations from the bounds of a wildcard type.
     *
     * @param type the wildcard type
     * @param tree the tree for the wildcard type
     * @param superDisallowed annotations to remove from the super bound
     * @param extendsDisallowed annotations to remove from the extends bound
     */
    private void stripInvalidLocationQualifiersFromWildcardBounds(
            AnnotatedWildcardType type,
            Tree tree,
            List<AnnotationMirror> superDisallowed,
            List<AnnotationMirror> extendsDisallowed) {
        boolean stripped = false;
        for (AnnotationMirror am : superDisallowed) {
            type.getSuperBound().removeAnnotation(am);
            stripped = true;
        }
        for (AnnotationMirror am :
                additionalAnnotationsToStripFromWildcardBound(
                        type, tree, type.getSuperBound(), WILDCARD_SUPER_BOUND_LOCATIONS)) {
            type.getSuperBound().removeAnnotation(am);
            stripped = true;
        }
        for (AnnotationMirror am : extendsDisallowed) {
            type.getExtendsBound().removeAnnotation(am);
            stripped = true;
        }
        for (AnnotationMirror am :
                additionalAnnotationsToStripFromWildcardBound(
                        type, tree, type.getExtendsBound(), WILDCARD_EXTENDS_BOUND_LOCATIONS)) {
            type.getExtendsBound().removeAnnotation(am);
            stripped = true;
        }
        if (stripped) {
            atypeFactory.addDefaultAnnotations(type);
        }
    }

    /**
     * Returns additional annotations to strip from {@code bound} (one of {@code type}'s super or
     * extends bound), beyond whatever {@link #annotationsDisallowedAtLocation(AnnotatedTypeMirror,
     * Set)} already found. Called only when the checker opts in via {@link
     * #shouldStripInvalidLocationQualifiers}. The default returns an empty list, so opted-in
     * checkers whose qualifiers use {@code @TargetLocations} are unaffected ({@code
     * annotationsDisallowedAtLocation} already covers their case, for both reporting and
     * stripping).
     *
     * <p>{@code type} and {@code tree} let a checker whose validity check is tree-based instead
     * (e.g., it must tell an explicitly written annotation apart from the same qualifier arriving
     * through defaulting, which {@code @TargetLocations} cannot express) decide independently what
     * to strip, without also triggering {@code annotationsDisallowedAtLocation}'s {@code
     * type.invalid.annotations.on.location} report for annotations the checker already reports
     * through its own, more specific mechanism.
     *
     * @param type the wildcard type being validated
     * @param tree the tree for {@code type}
     * @param bound {@code type}'s super or extends bound
     * @param allowedLocations the type-use locations {@code bound} may be annotated at
     * @return additional annotations on {@code bound} to strip
     * @see #stripInvalidLocationQualifiersFromTypeVariableBounds
     * @see #additionalAnnotationsToStripFromTypeVariableBound
     * @see #validateWildcardTargetLocations
     * @see #annotationsDisallowedAtLocation(AnnotatedTypeMirror, Set)
     * @see #shouldStripInvalidLocationQualifiers
     */
    protected List<AnnotationMirror> additionalAnnotationsToStripFromWildcardBound(
            AnnotatedWildcardType type,
            Tree tree,
            AnnotatedTypeMirror bound,
            Set<TypeUseLocation> allowedLocations) {
        return Collections.emptyList();
    }

    /**
     * Check whether the list contains any of the permitted locations.
     *
     * @param locations the locations to check
     * @param permitted the permitted locations
     * @return whether locations contains any of the permitted locations
     */
    private static boolean containsAny(
            List<TypeUseLocation> locations, Set<TypeUseLocation> permitted) {
        for (int i = 0, n = locations.size(); i < n; ++i) {
            if (permitted.contains(locations.get(i))) {
                return true;
            }
        }
        return false;
    }
}

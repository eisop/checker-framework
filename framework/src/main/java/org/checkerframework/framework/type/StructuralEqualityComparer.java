package org.checkerframework.framework.type;

import org.checkerframework.checker.interning.qual.EqualsMethod;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AtmCombo;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.StringsPlume;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A visitor used to compare two type mirrors for "structural" equality. Structural equality implies
 * that, for two objects, all fields are also structurally equal and for primitives their values are
 * equal.
 *
 * <p>See also DefaultTypeHierarchy, and SubtypeVisitHistory
 */
public class StructuralEqualityComparer extends AbstractAtmComboVisitor<Boolean, Void> {
    /** History saving the result of previous comparisons. */
    protected final StructuralEqualityVisitHistory visitHistory;

    /**
     * The top of the hierarchy in which the types are compared, as for {@code
     * DefaultTypeHierarchy.currentTop}.
     */
    private AnnotationMirror currentTop = null;

    /**
     * The type parameters of the pairs of type variables whose bounds {@link #visitTypevar_Typevar}
     * is comparing, as consecutive elements: the parameter of the first type variable of each pair
     * is followed by that of the second. A pair that recurs while its bounds are compared, such as
     * the inner {@code K} of {@code K extends Comparable<? super K>}, denotes the type variables
     * themselves and is therefore equal.
     */
    private final List<Element> typeVarsBeingCompared = new ArrayList<>();

    /**
     * Create a StructuralEqualityComparer.
     *
     * @param typeargVisitHistory history saving the result of previous comparisons
     */
    public StructuralEqualityComparer(StructuralEqualityVisitHistory typeargVisitHistory) {
        this.visitHistory = typeargVisitHistory;
    }

    @Override
    public Boolean defaultAction(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void p) {

        return super.defaultAction(type1, type2, p);
    }

    /**
     * Called for every combination that isn't specifically handled.
     *
     * @return error message explaining the two types' classes are not the same
     */
    @Override
    public String defaultErrorMessage(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void p) {
        return super.defaultErrorMessage(type1, type2, p)
                + System.lineSeparator()
                + "  visitHistory = "
                + visitHistory;
    }

    /**
     * Returns true if type1 and type2 are structurally equivalent. The two types must be of the
     * same kind, except that a declared type may be compared with the primitive type it boxes and
     * with a captured type variable; any other combination of kinds is a bug.
     *
     * @param type1 the first AnnotatedTypeMirror to compare
     * @param type2 the second AnnotatedTypeMirror to compare
     * @return true if type1 and type2 are equal
     */
    @EqualsMethod
    private boolean areEqual(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
        if (type1 == type2) {
            return true;
        }
        assert currentTop != null;
        if (type1 == null || type2 == null) {
            return false;
        }
        return AtmCombo.accept(type1, type2, null, this);
    }

    public boolean areEqualInHierarchy(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, AnnotationMirror top) {
        assert top != null;
        boolean areEqual;
        AnnotationMirror prevTop = currentTop;
        currentTop = top;
        try {
            areEqual = areEqual(type1, type2);
        } finally {
            currentTop = prevTop;
        }

        return areEqual;
    }

    /**
     * Returns true if type1 and type2 have the same set of annotations.
     *
     * @param type1 a type
     * @param type2 a type
     * @return true if type1 and type2 have the same set of annotations
     */
    protected boolean arePrimaryAnnosEqual(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
        if (currentTop == null) {
            throw new BugInCF("currentTop null");
        }
        return arePrimaryAnnosEqual(
                type1.getAnnotationInHierarchy(currentTop),
                type2.getAnnotationInHierarchy(currentTop),
                type1,
                type2);
    }

    /**
     * Returns true if {@code anno1} (on {@code type1}) and {@code anno2} (on {@code type2}) are
     * equal, i.e. each is a shallow subtype of the other. This overload takes the annotations to
     * compare explicitly rather than reading them from the types, so a subclass can compare
     * normalized/canonicalized annotations <em>without mutating</em> the types (see {@code
     * ValueAnnotatedTypeFactory}); a mutating comparator cannot be used on the shared, frozen types
     * returned by the annotated-type caches.
     *
     * @param anno1 the annotation on {@code type1} to compare, or null
     * @param anno2 the annotation on {@code type2} to compare, or null
     * @param type1 the first type (used for its underlying type and factory)
     * @param type2 the second type
     * @return true if {@code anno1} and {@code anno2} are equal
     */
    protected boolean arePrimaryAnnosEqual(
            AnnotationMirror anno1,
            AnnotationMirror anno2,
            AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2) {
        TypeMirror typeMirror1 = type1.underlyingType;
        TypeMirror typeMirror2 = type2.underlyingType;
        QualifierHierarchy qh = type1.atypeFactory.getQualifierHierarchy();
        return qh.isSubtypeShallow(anno1, typeMirror1, anno2, typeMirror2)
                && qh.isSubtypeShallow(anno2, typeMirror2, anno1, typeMirror1);
    }

    /**
     * Compare each type in types1 and types2 pairwise and return true if they are all equal. This
     * method throws an exceptions if types1.size() != types2.size()
     *
     * @return true if for each pair (t1 = types1.get(i); t2 = types2.get(i)), areEqual(t1,t2)
     */
    protected boolean areAllEqual(
            Collection<? extends AnnotatedTypeMirror> types1,
            Collection<? extends AnnotatedTypeMirror> types2) {
        if (types1.size() != types2.size()) {
            throw new BugInCF(
                    "Mismatching collection sizes:%n    types 1: %s (%d)%n    types 2: %s (%d)",
                    StringsPlume.join("; ", types1),
                    types1.size(),
                    StringsPlume.join("; ", types2),
                    types2.size());
        }

        Iterator<? extends AnnotatedTypeMirror> types1Iter = types1.iterator();
        Iterator<? extends AnnotatedTypeMirror> types2Iter = types2.iterator();
        while (types1Iter.hasNext()) {
            AnnotatedTypeMirror type1 = types1Iter.next();
            AnnotatedTypeMirror type2 = types2Iter.next();
            if (!checkOrAreEqual(type1, type2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * First check visitHistory to see if type1 and type2 have been compared once already. If so
     * return true; otherwise compare them and put them in visitHistory.
     *
     * @param type1 the first type
     * @param type2 the second type
     * @return true if the two types are equal
     */
    protected boolean checkOrAreEqual(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
        Boolean pastResult = visitHistory.get(type1, type2, currentTop);
        if (pastResult != null) {
            return pastResult;
        }

        Boolean result = areEqual(type1, type2);
        visitHistory.put(type1, type2, currentTop, result);
        return result;
    }

    /**
     * Two arrays are equal if:
     *
     * <ol>
     *   <li>Their sets of primary annotations are equal, and
     *   <li>Their component types are equal
     * </ol>
     */
    @Override
    public Boolean visitArray_Array(AnnotatedArrayType type1, AnnotatedArrayType type2, Void p) {
        if (!arePrimaryAnnosEqual(type1, type2)) {
            return false;
        }

        return areEqual(type1.getComponentType(), type2.getComponentType());
    }

    /**
     * Two declared types are equal if:
     *
     * <ol>
     *   <li>The types are of the same class/interfaces
     *   <li>Their sets of primary annotations are equal
     *   <li>Their sets of type arguments are equal or one type is raw
     * </ol>
     */
    @Override
    public Boolean visitDeclared_Declared(
            AnnotatedDeclaredType type1, AnnotatedDeclaredType type2, Void p) {
        Boolean pastResult = visitHistory.get(type1, type2, currentTop);
        if (pastResult != null) {
            return pastResult;
        }

        // TODO: same class/interface is not enforced. Why?

        if (!arePrimaryAnnosEqual(type1, type2)) {
            return false;
        }
        // Prevent infinite recursion e.g. in Issue1587b
        visitHistory.put(type1, type2, currentTop, true);

        boolean result = areTypeArgsEqual(type1, type2);
        visitHistory.put(type1, type2, currentTop, result);
        return result;
    }

    /**
     * Returns true if the type arguments of {@code type1} and {@code type2} are equal. The type
     * arguments of an inner class type include those of its enclosing types (JLS 4.5), so this
     * method also compares the type arguments of the enclosing types. It does not compare the
     * primary annotations of the enclosing types.
     *
     * @param type1 a declared type
     * @param type2 a declared type
     * @return true if the type arguments of {@code type1} and {@code type2}, including those of
     *     their enclosing types, are equal
     */
    private boolean areTypeArgsEqual(AnnotatedDeclaredType type1, AnnotatedDeclaredType type2) {
        List<AnnotatedTypeMirror> type1Args = type1.getTypeArguments();
        List<AnnotatedTypeMirror> type2Args = type2.getTypeArguments();

        // Wildcard type arguments are compared through their captures (JLS 5.1.10): the upper
        // bound of the capture of a wildcard is the greatest lower bound of the wildcard's extends
        // bound and the declared bound of the type parameter, so `C<?>` and `C<? extends Bound>`
        // are equal, as they are in Java.  The captured type variables are compared structurally,
        // so the qualifiers of the super bound, of the extends bound, and of the type arguments of
        // either bound all take part in the comparison.
        AnnotatedTypeFactory atypeFactory = type1.atypeFactory;
        AnnotatedDeclaredType capturedType1 =
                (AnnotatedDeclaredType) atypeFactory.applyCaptureConversion(type1);
        AnnotatedDeclaredType capturedType2 =
                (AnnotatedDeclaredType) atypeFactory.applyCaptureConversion(type2);
        visitHistory.put(capturedType1, capturedType2, currentTop, true);

        List<AnnotatedTypeMirror> capturedType1Args = capturedType1.getTypeArguments();
        List<AnnotatedTypeMirror> capturedType2Args = capturedType2.getTypeArguments();
        boolean result = true;
        for (int i = 0, n = type1Args.size(); i < n; ++i) {
            AnnotatedTypeMirror type1Arg = type1Args.get(i);
            AnnotatedTypeMirror type2Arg = type2Args.get(i);
            Boolean pastResultTA = visitHistory.get(type1Arg, type2Arg, currentTop);
            if (pastResultTA != null) {
                result = pastResultTA;
            } else if (type1Arg.getKind() != TypeKind.WILDCARD
                    && type2Arg.getKind() != TypeKind.WILDCARD) {
                result = areEqual(type1Arg, type2Arg);
            } else if (atypeFactory.ignoreRawTypeArguments
                    && (AnnotatedTypes.isTypeArgOfRawType(type1Arg)
                            || AnnotatedTypes.isTypeArgOfRawType(type2Arg))) {
                result = true;
            } else {
                // At least one argument is a wildcard: compare the captured arguments. A
                // non-wildcard argument is unchanged by capture, and javac collapses a wildcard
                // whose bounds coincide to a declared type, so a declared type may stand opposite
                // a wildcard here.
                result = areEqual(capturedType1Args.get(i), capturedType2Args.get(i));
            }
            if (!result) {
                break;
            }
        }

        visitHistory.put(capturedType1, capturedType2, currentTop, result);
        if (!result) {
            return false;
        }

        AnnotatedDeclaredType enclosing1 = type1.getEnclosingType();
        AnnotatedDeclaredType enclosing2 = type2.getEnclosingType();
        if (enclosing1 == null
                || enclosing2 == null
                || enclosing1.getTypeArguments().size() != enclosing2.getTypeArguments().size()) {
            return true;
        }
        return areTypeArgsEqual(enclosing1, enclosing2);
    }

    /**
     * Two intersection types are equal if:
     *
     * <ul>
     *   <li>Their sets of primary annotations are equal
     *   <li>Their sets of bounds (the types being intersected) are equal
     * </ul>
     */
    @Override
    public Boolean visitIntersection_Intersection(
            AnnotatedIntersectionType type1, AnnotatedIntersectionType type2, Void p) {
        if (!arePrimaryAnnosEqual(type1, type2)) {
            return false;
        }

        boolean result = areAllEqual(type1.getBounds(), type2.getBounds());
        visitHistory.put(type1, type2, currentTop, result);
        return result;
    }

    /**
     * Two primitive types are equal if:
     *
     * <ul>
     *   <li>Their sets of primary annotations are equal
     * </ul>
     */
    @Override
    public Boolean visitPrimitive_Primitive(
            AnnotatedPrimitiveType type1, AnnotatedPrimitiveType type2, Void p) {
        return arePrimaryAnnosEqual(type1, type2);
    }

    @Override
    public Boolean visitNull_Null(AnnotatedNullType type1, AnnotatedNullType type2, Void unused) {
        return arePrimaryAnnosEqual(type1, type2);
    }

    /**
     * Two type variables are equal if:
     *
     * <ul>
     *   <li>Their bounds are equal
     * </ul>
     *
     * Note: Primary annotations will be taken into account when the bounds are retrieved
     *
     * <p>A reference to the same pair of type variables within their own bounds, such as the inner
     * {@code K} of {@code K extends Comparable<? super K>}, is equal: it denotes the type variables
     * whose bounds are being compared. Such a reference may be the very {@code
     * AnnotatedTypeVariable} being compared or a separate copy of it, and only in the former case
     * does it carry the primary annotations of the outer use in its bounds, so comparing its bounds
     * would compare an artifact of the representation rather than the types.
     */
    @Override
    public Boolean visitTypevar_Typevar(
            AnnotatedTypeVariable type1, AnnotatedTypeVariable type2, Void p) {
        Boolean pastResult = visitHistory.get(type1, type2, currentTop);
        if (pastResult != null) {
            return pastResult;
        }

        Element typeParam1 = type1.getUnderlyingType().asElement();
        Element typeParam2 = type2.getUnderlyingType().asElement();
        for (int i = 0, n = typeVarsBeingCompared.size(); i < n; i += 2) {
            if (typeVarsBeingCompared.get(i).equals(typeParam1)
                    && typeVarsBeingCompared.get(i + 1).equals(typeParam2)) {
                return true;
            }
        }

        typeVarsBeingCompared.add(typeParam1);
        typeVarsBeingCompared.add(typeParam2);
        boolean result;
        try {
            result =
                    areEqual(type1.getUpperBound(), type2.getUpperBound())
                            && areEqual(type1.getLowerBound(), type2.getLowerBound());
        } finally {
            int size = typeVarsBeingCompared.size();
            typeVarsBeingCompared.remove(size - 1);
            typeVarsBeingCompared.remove(size - 2);
        }
        visitHistory.put(type1, type2, currentTop, result);
        return result;
    }

    /**
     * Two wildcards are equal if:
     *
     * <ul>
     *   <li>Their bounds are equal
     * </ul>
     *
     * Note: Primary annotations will be taken into account when the bounds are retrieved
     */
    @Override
    public Boolean visitWildcard_Wildcard(
            AnnotatedWildcardType type1, AnnotatedWildcardType type2, Void p) {
        Boolean pastResult = visitHistory.get(type1, type2, currentTop);
        if (pastResult != null) {
            return pastResult;
        }

        if (type1.atypeFactory.ignoreRawTypeArguments
                && (type1.isTypeArgOfRawType() || type2.isTypeArgOfRawType())) {
            return true;
        }

        Boolean result =
                areEqual(type1.getExtendsBound(), type2.getExtendsBound())
                        && areEqual(type1.getSuperBound(), type2.getSuperBound());
        visitHistory.put(type1, type2, currentTop, result);
        return result;
    }

    // Since we don't do a boxing conversion between primitive and declared types, in some cases
    // we must compare primitives with their boxed counterparts.
    @Override
    public Boolean visitDeclared_Primitive(
            AnnotatedDeclaredType type1, AnnotatedPrimitiveType type2, Void p) {
        if (!TypesUtils.isBoxOf(type1.getUnderlyingType(), type2.getUnderlyingType())) {
            throw new BugInCF(defaultErrorMessage(type1, type2, p));
        }

        return arePrimaryAnnosEqual(type1, type2);
    }

    @Override
    public Boolean visitPrimitive_Declared(
            AnnotatedPrimitiveType type1, AnnotatedDeclaredType type2, Void p) {
        if (!TypesUtils.isBoxOf(type2.getUnderlyingType(), type1.getUnderlyingType())) {
            throw new BugInCF(defaultErrorMessage(type1, type2, p));
        }

        return arePrimaryAnnosEqual(type1, type2);
    }

    @Override
    public Boolean visitTypevar_Declared(
            AnnotatedTypeVariable type1, AnnotatedDeclaredType type2, Void unused) {
        // This case should not happen, but sometimes type argument inference incorrectly infers a
        // captured type, when javac does not. See Issue6755.java.
        if (TypesUtils.isCapturedTypeVariable(type1.underlyingType)) {
            if (type1.getLowerBound().getKind() != TypeKind.NULL) {
                return visit(type1.getLowerBound(), type2, unused);
            }
            return visit(type1.getUpperBound(), type2, unused);
        }
        return super.visitTypevar_Declared(type1, type2, unused);
    }

    @Override
    public Boolean visitDeclared_Typevar(
            AnnotatedDeclaredType type1, AnnotatedTypeVariable type2, Void unused) {
        // This case should not happen, but sometimes type argument inference incorrectly infers a
        // captured type, when javac does not. See Issue6755.java.
        if (TypesUtils.isCapturedTypeVariable(type2.underlyingType)) {
            if (type2.getLowerBound().getKind() != TypeKind.NULL) {
                return visit(type1, type2.getLowerBound(), unused);
            }
            return visit(type1, type2.getUpperBound(), unused);
        }
        return super.visitDeclared_Typevar(type1, type2, unused);
    }
}

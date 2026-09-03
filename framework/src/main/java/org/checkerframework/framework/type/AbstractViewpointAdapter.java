package org.checkerframework.framework.type;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.ElementUtils;
import org.plumelib.util.IPair;

import java.util.IdentityHashMap;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

/**
 * Abstract utility class for performing viewpoint adaptation.
 *
 * <p>This class contains the common logic for extracting and inserting viewpoint adapted
 * annotations into the corresponding types for member/field access, constructor and method
 * invocations, and type parameter bound instantiations.
 *
 * <p>Subclasses implement the computation of the precise viewpoint adapted type given a receiver
 * type and a declared type, and implement how to extract the qualifier given an ATM.
 */
public abstract class AbstractViewpointAdapter implements ViewpointAdapter {

    /** The annotated type factory. */
    protected final AnnotatedTypeFactory atypeFactory;

    /**
     * Construct an abstract viewpoint adapter with the given type factory.
     *
     * @param atypeFactory the type factory to use
     */
    protected AbstractViewpointAdapter(final AnnotatedTypeFactory atypeFactory) {
        this.atypeFactory = atypeFactory;
    }

    @Override
    public void viewpointAdaptMember(
            AnnotatedTypeMirror receiverType,
            Element memberElement,
            AnnotatedTypeMirror memberType) {
        if (!shouldAdaptMember(memberType, memberElement)) {
            return;
        }

        AnnotatedTypeMirror decltype = atypeFactory.getAnnotatedType(memberElement);
        AnnotatedTypeMirror combinedType = combineTypeWithType(receiverType, decltype);
        memberType.replaceAnnotations(combinedType.getAnnotationsField());
        if (memberType.getKind() == TypeKind.DECLARED
                && combinedType.getKind() == TypeKind.DECLARED) {
            AnnotatedDeclaredType adtType = (AnnotatedDeclaredType) memberType;
            AnnotatedDeclaredType adtCombinedType = (AnnotatedDeclaredType) combinedType;
            adtType.setTypeArguments(adtCombinedType.getTypeArguments());
        } else if (memberType.getKind() == TypeKind.ARRAY
                && combinedType.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType aatType = (AnnotatedArrayType) memberType;
            AnnotatedArrayType aatCombinedType = (AnnotatedArrayType) combinedType;
            aatType.setComponentType(aatCombinedType.getComponentType());
        }
    }

    /**
     * Determines whether a particular member should be viewpoint adapted or not. The default
     * implementation adapts all members except for local variables and method formal parameters.
     *
     * @param type type of the member, used to decide whether a member should be viewpoint adapted
     *     or not. A subclass of {@link ViewpointAdapter} may disable viewpoint adaptation for
     *     elements based on their types.
     * @param element element of the member
     * @return true if the member needs viewpoint adaptation
     */
    protected boolean shouldAdaptMember(AnnotatedTypeMirror type, Element element) {
        if (element.getKind() == ElementKind.LOCAL_VARIABLE
                || element.getKind() == ElementKind.PARAMETER) {
            return false;
        }
        return true;
    }

    @Override
    public void viewpointAdaptConstructor(
            AnnotatedTypeMirror receiverType,
            ExecutableElement constructorElt,
            AnnotatedExecutableType constructorType) {
        // 1. Make a copy of constructorType before type variables are substituted.
        AnnotatedExecutableType unsubstitutedConstructorType = constructorType.deepCopy();

        // 2. Viewpoint-adapt constructor parameter types, type variable bounds, and return type.
        List<AnnotatedTypeMirror> parameterTypes = unsubstitutedConstructorType.getParameterTypes();
        List<AnnotatedTypeVariable> typeVariables = unsubstitutedConstructorType.getTypeVariables();
        AnnotatedTypeMirror constructorReturn = unsubstitutedConstructorType.getReturnType();

        IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings =
                new IdentityHashMap<>();

        // 2a. Adapt parameter types.
        for (AnnotatedTypeMirror parameterType : parameterTypes) {
            AnnotatedTypeMirror p = combineTypeWithType(receiverType, parameterType);
            mappings.put(parameterType, p);
        }

        // 2b. Adapt upper and lower bounds of constructor type variables.
        for (AnnotatedTypeVariable typeVariable : typeVariables) {
            AnnotatedTypeMirror adaptedUpper =
                    combineTypeWithType(receiverType, typeVariable.getUpperBound());
            mappings.put(typeVariable.getUpperBound(), adaptedUpper);

            AnnotatedTypeMirror adaptedLower =
                    combineTypeWithType(receiverType, typeVariable.getLowerBound());
            mappings.put(typeVariable.getLowerBound(), adaptedLower);
        }

        // 2c. Adapt constructor return type.
        AnnotatedTypeMirror cr = combineTypeWithType(receiverType, constructorReturn);
        mappings.put(constructorReturn, cr);

        // 3. Replace components using AnnotatedTypeCopierWithReplacement.
        unsubstitutedConstructorType =
                (AnnotatedExecutableType)
                        AnnotatedTypeCopierWithReplacement.replace(
                                unsubstitutedConstructorType, mappings);

        // 4. Update target constructor type in place with adapted components.
        constructorType.setParameterTypes(unsubstitutedConstructorType.getParameterTypes());
        constructorType.setTypeVariables(unsubstitutedConstructorType.getTypeVariables());
        constructorType.setReturnType(unsubstitutedConstructorType.getReturnType());
        // Recompute the vararg type to ensure it corresponds to the newly updated parameter list.
        constructorType.computeVarargType();
    }

    @Override
    public void viewpointAdaptMethod(
            AnnotatedTypeMirror receiverType,
            ExecutableElement methodElt,
            AnnotatedExecutableType methodType) {
        // 1. Check whether the method should be viewpoint-adapted (e.g. skip static methods).
        if (!shouldAdaptMethod(methodElt)) {
            return;
        }

        // 2. Make a copy of methodType before type variables are substituted.
        AnnotatedExecutableType unsubstitutedMethodType = methodType.deepCopy();

        // 3. Viewpoint-adapt parameter types, type variable bounds, return type, and receiver.
        List<AnnotatedTypeMirror> parameterTypes = unsubstitutedMethodType.getParameterTypes();
        List<AnnotatedTypeVariable> typeVariables = unsubstitutedMethodType.getTypeVariables();
        AnnotatedTypeMirror returnType = unsubstitutedMethodType.getReturnType();
        AnnotatedTypeMirror methodReceiver = unsubstitutedMethodType.getReceiverType();

        IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings =
                new IdentityHashMap<>();

        // 3a. Adapt parameter types.
        for (AnnotatedTypeMirror parameterType : parameterTypes) {
            AnnotatedTypeMirror p = combineTypeWithType(receiverType, parameterType);
            mappings.put(parameterType, p);
        }

        // 3b. Adapt upper and lower bounds of method type variables.
        for (AnnotatedTypeVariable typeVariable : typeVariables) {
            AnnotatedTypeMirror adaptedUpper =
                    combineTypeWithType(receiverType, typeVariable.getUpperBound());
            mappings.put(typeVariable.getUpperBound(), adaptedUpper);

            AnnotatedTypeMirror adaptedLower =
                    combineTypeWithType(receiverType, typeVariable.getLowerBound());
            mappings.put(typeVariable.getLowerBound(), adaptedLower);
        }

        // 3c. Adapt non-void return type.
        if (returnType.getKind() != TypeKind.VOID) {
            AnnotatedTypeMirror r = combineTypeWithType(receiverType, returnType);
            mappings.put(returnType, r);
        }

        // 3d. Adapt method receiver type.
        if (methodReceiver != null) {
            AnnotatedTypeMirror mr = combineTypeWithType(receiverType, methodReceiver);
            mappings.put(methodReceiver, mr);
        }

        // 4. Replace components using AnnotatedTypeCopierWithReplacement.
        unsubstitutedMethodType =
                (AnnotatedExecutableType)
                        AnnotatedTypeCopierWithReplacement.replace(
                                unsubstitutedMethodType, mappings);

        // 5. Update target method type in place with adapted components.
        // Because we can't viewpoint adapt asMemberOf result, we adapt the declared method first,
        // and set the corresponding parts on the asMemberOf result.
        methodType.setReturnType(unsubstitutedMethodType.getReturnType());
        methodType.setReceiverType(unsubstitutedMethodType.getReceiverType());
        methodType.setParameterTypes(unsubstitutedMethodType.getParameterTypes());
        methodType.setTypeVariables(unsubstitutedMethodType.getTypeVariables());
        // Recompute the vararg type to ensure it corresponds to the newly updated parameter list.
        methodType.computeVarargType();
    }

    /**
     * Determine if an invocation of the given method needs to be adapted.
     *
     * @param element the executable element for a method
     * @return true if an invocation of the executable element needs to be adapted
     */
    protected boolean shouldAdaptMethod(ExecutableElement element) {
        return !ElementUtils.isStatic(element);
    }

    @Override
    public void viewpointAdaptTypeParameterBounds(
            AnnotatedTypeMirror receiverType,
            List<AnnotatedTypeParameterBounds> typeParameterBounds) {
        // Update in place: callers below assume the list is the same mutable list they passed in.
        for (int i = 0, n = typeParameterBounds.size(); i < n; ++i) {
            AnnotatedTypeParameterBounds typeParameterBound = typeParameterBounds.get(i);
            AnnotatedTypeMirror adaptedUpper =
                    combineTypeWithType(receiverType, typeParameterBound.getUpperBound());
            AnnotatedTypeMirror adaptedLower =
                    combineTypeWithType(receiverType, typeParameterBound.getLowerBound());
            typeParameterBounds.set(
                    i, new AnnotatedTypeParameterBounds(adaptedUpper, adaptedLower));
        }
    }

    @Override
    public AnnotatedTypeMirror viewpointAdaptType(
            AnnotatedTypeMirror receiverType, AnnotatedTypeMirror declaredType) {
        return combineTypeWithType(receiverType, declaredType);
    }

    /**
     * Viewpoint adapt declared type to receiver type, and return the result atm
     *
     * @param receiver receiver type
     * @param declared declared type
     * @return {@link AnnotatedTypeMirror} after viewpoint adaptation
     */
    protected AnnotatedTypeMirror combineTypeWithType(
            AnnotatedTypeMirror receiver, AnnotatedTypeMirror declared) {
        assert receiver != null && declared != null;

        AnnotatedTypeMirror result = declared;

        if (receiver.getKind() == TypeKind.TYPEVAR) {
            receiver = ((AnnotatedTypeVariable) receiver).getUpperBound();
        }
        AnnotationMirror receiverAnnotation = extractAnnotationMirror(receiver);
        if (receiverAnnotation != null) {
            result = combineAnnotationWithType(receiverAnnotation, declared);
            result = substituteTVars(receiver, result);
        }

        return result;
    }

    /**
     * Extract the relevant qualifier from an {@link AnnotatedTypeMirror}.
     *
     * @param atm AnnotatedTypeMirror from which qualifier is extracted
     * @return extracted qualifier
     */
    protected abstract AnnotationMirror extractAnnotationMirror(AnnotatedTypeMirror atm);

    /**
     * Combine receiver qualifiers with declared types. Qualifiers are extracted from declared types
     * to further perform viewpoint adaptation only between two qualifiers.
     *
     * @param receiverAnnotation receiver qualifier
     * @param declared declared type
     * @return {@link AnnotatedTypeMirror} after viewpoint adaptation
     */
    protected AnnotatedTypeMirror combineAnnotationWithType(
            AnnotationMirror receiverAnnotation, AnnotatedTypeMirror declared) {
        return new ViewpointAdaptationCopier(receiverAnnotation).visit(declared);
    }

    /**
     * Copies an annotated type graph while viewpoint-adapting its qualifiers. The copier's
     * original-to-copy map preserves recursive edges and shared subgraphs, so a type that refers
     * back to itself is adapted once and every reference to it points at the adapted copy.
     */
    private final class ViewpointAdaptationCopier extends AnnotatedTypeCopier {

        /** The receiver qualifier used for viewpoint adaptation. */
        private final AnnotationMirror receiverAnnotation;

        /**
         * Creates a copier that viewpoint-adapts qualifiers using {@code receiverAnnotation}.
         *
         * @param receiverAnnotation the receiver qualifier
         */
        private ViewpointAdaptationCopier(AnnotationMirror receiverAnnotation) {
            this.receiverAnnotation = receiverAnnotation;
        }

        @Override
        protected void maybeCopyPrimaryAnnotations(
                AnnotatedTypeMirror source, AnnotatedTypeMirror dest) {
            super.maybeCopyPrimaryAnnotations(source, dest);
            TypeKind kind = source.getKind();
            if (kind.isPrimitive()
                    || kind == TypeKind.DECLARED
                    || kind == TypeKind.ARRAY
                    || kind == TypeKind.NULL) {
                AnnotationMirror resultAnnotation =
                        combineAnnotationWithAnnotation(
                                receiverAnnotation, extractAnnotationMirror(source));
                dest.replaceAnnotation(resultAnnotation);
            }
        }

        @Override
        public AnnotatedTypeMirror visitIntersection(
                AnnotatedIntersectionType original,
                IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
            AnnotatedTypeMirror existing = originalToCopy.get(original);
            if (existing != null) {
                return existing;
            }
            AnnotatedIntersectionType result =
                    (AnnotatedIntersectionType) super.visitIntersection(original, originalToCopy);
            // Recompute the intersection's own primary annotation from the adapted bounds.  Do not
            // clear it first: AnnotatedIntersectionType#clearAnnotations also clears every bound,
            // which would discard the adapted bounds before summarizeBounds reads them.
            result.summarizeBounds();
            return result;
        }
    }

    /**
     * Viewpoint adapt declared qualifier to receiver qualifier.
     *
     * @param receiverAnnotation receiver qualifier
     * @param declaredAnnotation declared qualifier
     * @return result qualifier after viewpoint adaptation
     */
    @SideEffectFree
    protected abstract AnnotationMirror combineAnnotationWithAnnotation(
            AnnotationMirror receiverAnnotation, AnnotationMirror declaredAnnotation);

    /**
     * If rhs contains or is a use of a type variable of lhs's class, substitutes lhs's actual type
     * argument for it and returns the result. Side-effect free: when there is anything to
     * substitute, rhs is copied and the copy is returned; when lhs is not a declared type there is
     * nothing to substitute and rhs itself is returned.
     *
     * @param lhs type from which type arguments are extracted to replace formal type parameters of
     *     rhs
     * @param rhs {@link AnnotatedTypeMirror} that might be, or contain, a formal type parameter
     * @return a copy of rhs with its type parameters substituted
     */
    private AnnotatedTypeMirror substituteTVars(AnnotatedTypeMirror lhs, AnnotatedTypeMirror rhs) {
        if (lhs.getKind() != TypeKind.DECLARED) {
            return rhs;
        }
        return new TypeVariableSubstitutionCopier((AnnotatedDeclaredType) lhs).visit(rhs);
    }

    /**
     * Performs the substitution described by {@link #substituteTVars}. Cycles terminate on {@link
     * AnnotatedTypeCopier}'s original-to-copy map: a type that refers back to itself is copied once
     * and every reference to it points at that copy.
     */
    private final class TypeVariableSubstitutionCopier extends AnnotatedTypeCopier {

        /** The receiver from which actual type arguments are taken. */
        private final AnnotatedDeclaredType receiver;

        /**
         * Creates a copier that substitutes type variables of {@code receiver}'s class.
         *
         * @param receiver the receiver
         */
        private TypeVariableSubstitutionCopier(AnnotatedDeclaredType receiver) {
            this.receiver = receiver;
        }

        @Override
        public AnnotatedTypeMirror visitTypeVariable(
                AnnotatedTypeVariable original,
                IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
            AnnotatedTypeMirror existing = originalToCopy.get(original);
            if (existing != null) {
                return existing;
            }
            // As before this class replaced a hand-rolled traversal, a type variable is terminal:
            // if it has no actual type argument, it is returned unchanged rather than descended
            // into.
            return getTypeVariableSubstitution(receiver, original);
        }

        @Override
        public AnnotatedTypeMirror visitIntersection(
                AnnotatedIntersectionType original,
                IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
            AnnotatedTypeMirror existing = originalToCopy.get(original);
            if (existing != null) {
                return existing;
            }
            AnnotatedIntersectionType result =
                    (AnnotatedIntersectionType) super.visitIntersection(original, originalToCopy);
            result.summarizeBounds();
            return result;
        }
    }

    /**
     * Return actual type argument for formal type parameter "var" from "type"
     *
     * @param type type from which type arguments are extracted to replace "var"
     * @param var formal type parameter that needs real type arguments
     * @return Real type argument
     */
    private AnnotatedTypeMirror getTypeVariableSubstitution(
            AnnotatedDeclaredType type, AnnotatedTypeVariable var) {
        IPair<AnnotatedDeclaredType, Integer> res = findDeclType(type, var);

        if (res == null) {
            return var;
        }

        AnnotatedDeclaredType decltype = res.first;
        int foundindex = res.second;

        List<AnnotatedTypeMirror> tas = decltype.getTypeArguments();
        // return a copy, as we want to modify the type later.
        AnnotatedTypeMirror result = tas.get(foundindex).shallowCopy(true);
        if (result.getKind() == TypeKind.WILDCARD) {
            AnnotatedWildcardType wildcard = (AnnotatedWildcardType) result;
            // When substituting an unbounded wildcard for a bounded type variable, the shallow
            // copy might lose the reference to the formal type variable that provides its
            // effective extends bound. Preserve this bound so that subsequent subtype checks
            // do not treat the wildcard as implicitly bounded by Object.
            if (wildcard.getUnderlyingType().getExtendsBound() == null
                    && wildcard.getTypeVariable() == null
                    && wildcard.getSuperBound().getKind() == TypeKind.NULL) {
                wildcard.setExtendsBound(var.getUpperBound().deepCopy(true));
            }
        }
        return result;
    }

    /**
     * Find the index (position) of this type variable from type
     *
     * @param type type from which we infer actual type arguments
     * @param var formal type parameter
     * @return index(position) of this type variable from type
     */
    private IPair<AnnotatedDeclaredType, Integer> findDeclType(
            AnnotatedDeclaredType type, AnnotatedTypeVariable var) {
        Element varelem = var.getUnderlyingType().asElement();

        DeclaredType dtype = type.getUnderlyingType();
        TypeElement el = (TypeElement) dtype.asElement();
        List<? extends TypeParameterElement> tparams = el.getTypeParameters();
        int foundindex = 0;

        for (TypeParameterElement tparam : tparams) {
            if (tparam.equals(varelem)) {
                // we found the right index!
                break;
            }
            ++foundindex;
        }

        if (foundindex >= tparams.size()) {
            // Didn't find the desired type => Head for super type of "type"!
            for (AnnotatedDeclaredType sup : type.directSupertypes()) {
                IPair<AnnotatedDeclaredType, Integer> res = findDeclType(sup, var);
                if (res != null) {
                    return res;
                }
            }
            // We reach this point if the variable wasn't found in any recursive call on ALL direct
            // supertypes.
            return null;
        }

        return IPair.of(type, foundindex);
    }
}

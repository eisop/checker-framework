package org.checkerframework.framework.type;

import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable.TypeVariableUseKind;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.TypesUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

/** TypeVariableSubstitutor replaces type variables from a declaration with arguments to its use. */
public class TypeVariableSubstitutor {

    /** Create a TypeVariableSubstitutor. */
    public TypeVariableSubstitutor() {}

    /**
     * Given a mapping from type variable to its type argument, replace each instance of a type
     * variable with a copy of type argument.
     *
     * <p>This method is {@code final}: it is a convenience entry point, not an extension point. A
     * checker that wants to customize substitution should override {@link
     * #substituteTypeVariable(AnnotatedTypeMirror, AnnotatedTypeVariable, boolean)} instead
     * (installed via {@code AnnotatedTypeFactory#createTypeVariableSubstitutor}), so there is
     * exactly one override point rather than two arities that could be overridden inconsistently.
     *
     * @see #substituteTypeVariable(AnnotatedTypeMirror,
     *     org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable, boolean)
     * @param typeVarToTypeArgument a mapping from type variable to its type argument
     * @param type the type to substitute
     * @return a copy of type with its type variables substituted
     */
    public final AnnotatedTypeMirror substitute(
            Map<TypeVariable, AnnotatedTypeMirror> typeVarToTypeArgument,
            AnnotatedTypeMirror type) {
        return substitute(typeVarToTypeArgument, type, false);
    }

    /**
     * Given a mapping from type variable to its type argument, replace each instance of a type
     * variable with a copy of type argument.
     *
     * <p>This method is {@code final}; see {@link #substitute(Map, AnnotatedTypeMirror)}.
     *
     * @see #substituteTypeVariable(AnnotatedTypeMirror,
     *     org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable, boolean)
     * @param typeVarToTypeArgument a mapping from type variable to its type argument
     * @param type the type to substitute
     * @param typeArgumentsInferred whether the type arguments in {@code typeVarToTypeArgument} were
     *     inferred by the type checker, as opposed to written explicitly by the programmer at the
     *     call site
     * @return a copy of type with its type variables substituted
     */
    public final AnnotatedTypeMirror substitute(
            Map<TypeVariable, AnnotatedTypeMirror> typeVarToTypeArgument,
            AnnotatedTypeMirror type,
            boolean typeArgumentsInferred) {
        return new Visitor(typeVarToTypeArgument, true, typeArgumentsInferred).visit(type);
    }

    /**
     * Given a mapping from type variable to its type argument, replace each instance of a type
     * variable with the given type argument.
     *
     * @see #substituteTypeVariable(AnnotatedTypeMirror,
     *     org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable, boolean)
     * @param typeVarToTypeArgument a mapping from type variable to its type argument
     * @param type the type to substitute
     * @return a copy of type with its type variables substituted
     */
    public AnnotatedTypeMirror substituteWithoutCopyingTypeArguments(
            Map<TypeVariable, AnnotatedTypeMirror> typeVarToTypeArgument,
            AnnotatedTypeMirror type) {
        return new Visitor(typeVarToTypeArgument, false, false).visit(type);
    }

    /**
     * Given the types of a type parameter declaration, the argument to that type parameter
     * declaration, and a given use of that declaration, return a substitute for the use with the
     * correct annotations.
     *
     * <p>To determine what primary annotations are correct for the substitute the following rules
     * are used: if the type variable use represents {@code @Sub E}, use the annotations of the
     * argument. If it represents {@code @Concrete q E}, apply the type variable use's primary
     * annotations to the substitute.
     *
     * @param argument the argument to declaration (this will be a value in typeParamToArg)
     * @param use the use that is being replaced
     * @param argumentIsInferred whether {@code argument} is a type argument that the type checker
     *     inferred (true), as opposed to one the programmer wrote explicitly at the call site
     *     (false)
     * @return a deep copy of argument with the appropriate annotations applied
     */
    protected AnnotatedTypeMirror substituteTypeVariable(
            AnnotatedTypeMirror argument, AnnotatedTypeVariable use, boolean argumentIsInferred) {
        AnnotatedTypeMirror substitute = argument.deepCopy(true);
        if (use.getTypeVariableUseKind() == TypeVariableUseKind.CONCRETE) {
            AnnotationMirrorSet concreteAnnotations = use.getConcreteTypeVariableUseAnnotations();
            if (!concreteAnnotations.isEmpty()) {
                substitute.replaceAnnotations(concreteAnnotations);
            }
        }
        return substitute;
    }

    /**
     * Visitor that makes the substitution. This is an inner class so that its methods cannot be
     * called by clients of {@link TypeVariableSubstitutor}.
     */
    protected class Visitor extends AnnotatedTypeCopier {

        /**
         * A mapping from {@link TypeParameterElement} to the {@link AnnotatedTypeMirror} that
         * should replace its uses.
         */
        private final Map<TypeParameterElement, AnnotatedTypeMirror> elementToArgMap;

        /**
         * A list of type variables that should be replaced by the type mirror at the same index in
         * {@code typeMirrors}
         */
        private final List<TypeVariable> typeVars;

        /**
         * A list of TypeMirrors that should replace the type variable at the same index in {@code
         * typeVars}
         */
        private final List<TypeMirror> typeMirrors;

        /** Whether or not a copy of type argument should be substituted. */
        private final boolean copyArgument;

        /**
         * Whether the type arguments in {@code elementToArgMap} were inferred by the type checker,
         * as opposed to written explicitly by the programmer at the call site.
         */
        private final boolean typeArgumentsInferred;

        /**
         * Creates the Visitor.
         *
         * @param typeParamToArg mapping from TypeVariable to the AnnotatedTypeMirror that will
         *     replace it
         * @param copyArgument whether or not a copy of type argument should be substituted
         * @param typeArgumentsInferred whether the type arguments in {@code typeParamToArg} were
         *     inferred by the type checker, as opposed to written explicitly by the programmer at
         *     the call site
         */
        public Visitor(
                Map<TypeVariable, AnnotatedTypeMirror> typeParamToArg,
                boolean copyArgument,
                boolean typeArgumentsInferred) {
            int size = typeParamToArg.size();
            elementToArgMap = new HashMap<>(size);
            typeVars = new ArrayList<>(size);
            typeMirrors = new ArrayList<>(size);

            for (Map.Entry<TypeVariable, AnnotatedTypeMirror> paramToArg :
                    typeParamToArg.entrySet()) {
                elementToArgMap.put(
                        (TypeParameterElement) paramToArg.getKey().asElement(),
                        paramToArg.getValue());
                typeVars.add(paramToArg.getKey());
                typeMirrors.add(paramToArg.getValue().getUnderlyingType());
            }
            this.copyArgument = copyArgument;
            this.typeArgumentsInferred = typeArgumentsInferred;
        }

        @Override
        protected <T extends AnnotatedTypeMirror> T makeCopy(T original) {
            if (original.getKind() == TypeKind.TYPEVAR) {
                return super.makeCopy(original);
            }
            TypeMirror s =
                    TypesUtils.substitute(
                            original.getUnderlyingType(),
                            typeVars,
                            typeMirrors,
                            original.atypeFactory.processingEnv);

            @SuppressWarnings("unchecked")
            T copy =
                    (T)
                            AnnotatedTypeMirror.createType(
                                    s, original.atypeFactory, original.isDeclaration());
            maybeCopyPrimaryAnnotations(original, copy);

            return copy;
        }

        @Override
        public AnnotatedTypeMirror visitTypeVariable(
                AnnotatedTypeVariable original,
                IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
            if (visitingExecutableTypeParam) {
                // AnnotatedExecutableType differs from AnnotatedDeclaredType in that its list of
                // type parameters cannot be adapted in place since the
                // AnnotatedExecutable.typeVarTypes field is of type AnnotatedTypeVariable and not
                // AnnotatedTypeMirror.  When substituting, all component types that contain a use
                // of the executable's type parameters will be substituted.  The executable's type
                // parameters will have their bounds substituted but the top-level
                // AnnotatedTypeVariable's will remain
                visitingExecutableTypeParam = false;
                return super.visitTypeVariable(original, originalToCopy);
            } else {
                Element typeVarElem = original.getUnderlyingType().asElement();
                AnnotatedTypeMirror argument = elementToArgMap.get(typeVarElem);
                if (argument != null) {
                    if (copyArgument) {
                        return substituteTypeVariable(argument, original, typeArgumentsInferred);
                    } else {
                        return argument;
                    }
                }
            }

            return super.visitTypeVariable(original, originalToCopy);
        }
    }
}

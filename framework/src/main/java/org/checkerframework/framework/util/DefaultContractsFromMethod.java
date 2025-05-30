package org.checkerframework.framework.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.IPair;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.util.ElementFilter;

/**
 * A utility class to retrieve pre- and postconditions from a method.
 *
 * @see PreconditionAnnotation
 * @see RequiresQualifier
 * @see PostconditionAnnotation
 * @see EnsuresQualifier
 * @see ConditionalPostconditionAnnotation
 * @see EnsuresQualifierIf
 */
// TODO: This class assumes that most annotations have a field named "expression". If not, issue a
// more helpful error message.
public class DefaultContractsFromMethod implements ContractsFromMethod {

    /** The QualifierArgument.value field/element. */
    protected final ExecutableElement qualifierArgumentValueElement;

    /** The factory that this ContractsFromMethod is associated with. */
    protected final GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory;

    /**
     * Creates a ContractsFromMethod for the given factory.
     *
     * @param atypeFactory the type factory associated with the newly-created ContractsFromMethod
     */
    public DefaultContractsFromMethod(GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
        this.atypeFactory = atypeFactory;
        qualifierArgumentValueElement =
                TreeUtils.getMethod(
                        QualifierArgument.class, "value", 0, atypeFactory.getProcessingEnv());
    }

    /**
     * Returns all the contracts on method or constructor {@code executableElement}.
     *
     * @param executableElement the method or constructor whose contracts to retrieve
     * @return the contracts on {@code executableElement}
     */
    @Override
    public Set<Contract> getContracts(ExecutableElement executableElement) {
        Set<Contract> contracts = new LinkedHashSet<>();
        contracts.addAll(getPreconditions(executableElement));
        contracts.addAll(getPostconditions(executableElement));
        contracts.addAll(getConditionalPostconditions(executableElement));
        return contracts;
    }

    /**
     * Returns the precondition contracts on method or constructor {@code executableElement}.
     *
     * @param executableElement the method whose contracts to return
     * @return the precondition contracts on {@code executableElement}
     */
    @Override
    public Set<Contract.Precondition> getPreconditions(ExecutableElement executableElement) {
        return getContractsOfKind(
                executableElement, Contract.Kind.PRECONDITION, Contract.Precondition.class);
    }

    /**
     * Returns the postcondition contracts on {@code executableElement}.
     *
     * @param executableElement the method whose contracts to return
     * @return the postcondition contracts on {@code executableElement}
     */
    @Override
    public Set<Contract.Postcondition> getPostconditions(ExecutableElement executableElement) {
        return getContractsOfKind(
                executableElement, Contract.Kind.POSTCONDITION, Contract.Postcondition.class);
    }

    /**
     * Returns the conditional postcondition contracts on method {@code methodElement}.
     *
     * @param methodElement the method whose contracts to return
     * @return the conditional postcondition contracts on {@code methodElement}
     */
    @Override
    public Set<Contract.ConditionalPostcondition> getConditionalPostconditions(
            ExecutableElement methodElement) {
        return getContractsOfKind(
                methodElement,
                Contract.Kind.CONDITIONALPOSTCONDITION,
                Contract.ConditionalPostcondition.class);
    }

    // Helper methods

    /**
     * Returns the contracts (of a particular kind) on method or constructor {@code
     * executableElement}.
     *
     * @param <T> the type of {@link Contract} to return
     * @param executableElement the method whose contracts to return
     * @param kind the kind of contracts to retrieve
     * @param clazz the class to determine the return type
     * @return the contracts on {@code executableElement}
     */
    private <T extends Contract> Set<T> getContractsOfKind(
            ExecutableElement executableElement, Contract.Kind kind, Class<T> clazz) {
        Set<T> result = new LinkedHashSet<>();
        // Check for a single framework-defined contract annotation.
        // The result is RequiresQualifier, EnsuresQualifier, EnsuresQualifierIf, or null.
        AnnotationMirror frameworkContractAnno =
                atypeFactory.getDeclAnnotation(executableElement, kind.frameworkContractClass);
        if (frameworkContractAnno != null) {
            result.addAll(getContract(kind, frameworkContractAnno, clazz));
        }

        // Check for a framework-defined wrapper around contract annotations.
        // The result is RequiresQualifier.List, EnsuresQualifier.List, or EnsuresQualifierIf.List.
        // Add its elements to `result`.
        AnnotationMirror frameworkContractListAnno =
                atypeFactory.getDeclAnnotation(executableElement, kind.frameworkContractListClass);
        if (frameworkContractListAnno != null) {
            List<AnnotationMirror> frameworkContractAnnoList =
                    atypeFactory.getContractListValues(frameworkContractListAnno);
            for (AnnotationMirror a : frameworkContractAnnoList) {
                result.addAll(getContract(kind, a, clazz));
            }
        }

        // Check for type-system specific annotations.  These are the annotations that are
        // meta-annotated by `kind.metaAnnotation`, which is PreconditionAnnotation,
        // PostconditionAnnotation, or ConditionalPostconditionAnnotation.
        List<IPair<AnnotationMirror, AnnotationMirror>> declAnnotations =
                atypeFactory.getDeclAnnotationWithMetaAnnotation(
                        executableElement, kind.metaAnnotation);
        for (IPair<AnnotationMirror, AnnotationMirror> r : declAnnotations) {
            AnnotationMirror anno = r.first;
            // contractAnno is the meta-annotation on anno, such as PreconditionAnnotation,
            // PostconditionAnnotation, or ConditionalPostconditionAnnotation.
            AnnotationMirror contractAnno = r.second;
            AnnotationMirror enforcedQualifier =
                    getQualifierEnforcedByContractAnnotation(contractAnno, anno);
            if (enforcedQualifier == null) {
                continue;
            }
            List<String> expressions = atypeFactory.getContractExpressions(kind, anno);
            Collections.sort(expressions);
            Boolean ensuresQualifierIfResult = atypeFactory.getEnsuresQualifierIfResult(kind, anno);

            for (String expr : expressions) {
                T contract =
                        clazz.cast(
                                Contract.create(
                                        kind,
                                        expr,
                                        enforcedQualifier,
                                        anno,
                                        ensuresQualifierIfResult));
                result.add(contract);
            }
        }
        return result;
    }

    /**
     * Returns the contracts expressed by the given framework contract annotation.
     *
     * @param <T> the type of {@link Contract} to return
     * @param kind the kind of {@code contractAnnotation}
     * @param contractAnnotation a {@link RequiresQualifier}, {@link EnsuresQualifier}, or {@link
     *     EnsuresQualifierIf}
     * @param clazz the class to determine the return type
     * @return the contracts expressed by the given annotation, or the empty set if the argument is
     *     null
     */
    private <T extends Contract> Set<T> getContract(
            Contract.Kind kind, AnnotationMirror contractAnnotation, Class<T> clazz) {
        if (contractAnnotation == null) {
            return Collections.emptySet();
        }

        AnnotationMirror enforcedQualifier =
                getQualifierEnforcedByContractAnnotation(contractAnnotation);
        if (enforcedQualifier == null) {
            return Collections.emptySet();
        }

        List<String> expressions = atypeFactory.getContractExpressions(contractAnnotation);
        Collections.sort(expressions);

        Boolean ensuresQualifierIfResult =
                atypeFactory.getEnsuresQualifierIfResult(kind, contractAnnotation);

        Set<T> result = new LinkedHashSet<>();
        for (String expr : expressions) {
            T contract =
                    clazz.cast(
                            Contract.create(
                                    kind,
                                    expr,
                                    enforcedQualifier,
                                    contractAnnotation,
                                    ensuresQualifierIfResult));
            result.add(contract);
        }
        return result;
    }

    /**
     * Returns the annotation mirror as specified by the {@code qualifier} element in {@code
     * contractAnno}. May return null.
     *
     * @param contractAnno a pre- or post-condition annotation, such as {@code @RequiresQualifier}
     * @return the type annotation specified in {@code contractAnno.qualifier}
     */
    private @Nullable AnnotationMirror getQualifierEnforcedByContractAnnotation(
            AnnotationMirror contractAnno) {
        return getQualifierEnforcedByContractAnnotation(contractAnno, null, null);
    }

    /**
     * Returns the annotation mirror as specified by the {@code qualifier} element in {@code
     * contractAnno}, with elements/arguments taken from {@code argumentAnno}. May return null.
     *
     * @param contractAnno a pre- or post-condition annotation, such as {@code @RequiresQualifier}
     * @param argumentAnno supplies the elements/fields in the return value
     * @return the type annotation specified in {@code contractAnno.qualifier}
     */
    private @Nullable AnnotationMirror getQualifierEnforcedByContractAnnotation(
            AnnotationMirror contractAnno, AnnotationMirror argumentAnno) {

        Map<String, String> argumentRenaming =
                makeArgumentRenaming(argumentAnno.getAnnotationType().asElement());
        return getQualifierEnforcedByContractAnnotation(
                contractAnno, argumentAnno, argumentRenaming);
    }

    /**
     * Returns the annotation mirror as specified by the "qualifier" element in {@code
     * contractAnno}. If {@code argumentAnno} is specified, then elements/arguments are copied from
     * {@code argumentAnno} to the returned annotation, renamed according to {@code
     * argumentRenaming}. If {@code argumentAnno} is not specified, the result has no
     * elements/arguments; this may make it invalid.
     *
     * <p>This is a helper method. Use one of its overloads if possible.
     *
     * @param contractAnno a contract annotation, such as {@code @RequiresQualifier}, which has a
     *     {@code qualifier} element/field
     * @param argumentAnno annotation containing the element {@code values}, or {@code null}
     * @param argumentRenaming renaming of argument names, which maps from names in {@code
     *     argumentAnno} to names used in the returned annotation, or {@code null}
     * @return a qualifier whose type is that of {@code contractAnno.qualifier}, or an alias for it,
     *     or null if it is not a supported qualifier of the type system
     */
    private @Nullable AnnotationMirror getQualifierEnforcedByContractAnnotation(
            AnnotationMirror contractAnno,
            @Nullable AnnotationMirror argumentAnno,
            @Nullable Map<String, String> argumentRenaming) {

        @SuppressWarnings("deprecation") // permitted for use in the framework
        Name c = AnnotationUtils.getElementValueClassName(contractAnno, "qualifier", false);

        AnnotationMirror anno;
        if (argumentAnno == null || argumentRenaming.isEmpty()) {
            // If there are no arguments, use factory method that allows caching
            anno = AnnotationBuilder.fromName(atypeFactory.getElementUtils(), c);
        } else {
            AnnotationBuilder builder = new AnnotationBuilder(atypeFactory.getProcessingEnv(), c);
            builder.copyRenameElementValuesFromAnnotation(argumentAnno, argumentRenaming);
            anno = builder.build();
        }

        if (atypeFactory.isSupportedQualifier(anno)) {
            return anno;
        } else {
            AnnotationMirror aliasedAnno = atypeFactory.canonicalAnnotation(anno);
            if (atypeFactory.isSupportedQualifier(aliasedAnno)) {
                return aliasedAnno;
            } else {
                return null;
            }
        }
    }

    /**
     * Makes a map from element names of a contract annotation to qualifier argument names, as
     * defined by {@link QualifierArgument}.
     *
     * <p>Each element of {@code contractAnnoElement} that is annotated by {@link QualifierArgument}
     * is mapped to the name specified by the value of {@link QualifierArgument}. If the value is
     * not specified or is an empty string, then the element is mapped to an argument of the same
     * name.
     *
     * @param contractAnnoElement the declaration of the contract annotation containing the elements
     * @return map from the names of elements of {@code sourceArgumentNames} to the corresponding
     *     qualifier argument names
     * @see QualifierArgument
     */
    private Map<String, String> makeArgumentRenaming(Element contractAnnoElement) {
        HashMap<String, String> argumentRenaming = new HashMap<>();
        for (ExecutableElement meth :
                ElementFilter.methodsIn(contractAnnoElement.getEnclosedElements())) {
            AnnotationMirror argumentAnnotation =
                    atypeFactory.getDeclAnnotation(meth, QualifierArgument.class);
            if (argumentAnnotation != null) {
                String sourceName = meth.getSimpleName().toString();
                String targetName =
                        AnnotationUtils.getElementValue(
                                argumentAnnotation, qualifierArgumentValueElement, String.class);
                if (targetName == null || targetName.isEmpty()) {
                    targetName = sourceName;
                }
                argumentRenaming.put(sourceName, targetName);
            }
        }
        return argumentRenaming;
    }
}

package org.checkerframework.framework.util;

import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.RequiresQualifier;

import java.util.Set;

import javax.lang.model.element.ExecutableElement;

/**
 * Interface to retrieve pre- and postconditions from a method.
 *
 * @see PreconditionAnnotation
 * @see RequiresQualifier
 * @see PostconditionAnnotation
 * @see EnsuresQualifier
 * @see ConditionalPostconditionAnnotation
 * @see EnsuresQualifierIf
 */
public interface ContractsFromMethod {

    /**
     * Returns all the contracts on method or constructor {@code executableElement}.
     *
     * @param executableElement the method or constructor whose contracts to retrieve
     * @return the contracts on {@code executableElement}
     */
    Set<Contract> getContracts(ExecutableElement executableElement);

    /**
     * Returns the precondition contracts on method or constructor {@code executableElement}.
     *
     * @param executableElement the method whose contracts to return
     * @return the precondition contracts on {@code executableElement}
     */
    Set<Contract.Precondition> getPreconditions(ExecutableElement executableElement);

    /**
     * Returns the postcondition contracts on {@code executableElement}.
     *
     * @param executableElement the method whose contracts to return
     * @return the postcondition contracts on {@code executableElement}
     */
    Set<Contract.Postcondition> getPostconditions(ExecutableElement executableElement);

    /**
     * Returns the conditional postcondition contracts on method {@code methodElement}.
     *
     * @param methodElement the method whose contracts to return
     * @return the conditional postcondition contracts on {@code methodElement}
     */
    Set<Contract.ConditionalPostcondition> getConditionalPostconditions(
            ExecutableElement methodElement);
}

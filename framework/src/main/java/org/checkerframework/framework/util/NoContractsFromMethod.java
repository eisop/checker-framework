package org.checkerframework.framework.util;

import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;

/** Dummy implementation of {@link ContractsFromMethod} that only returns empty sets. */
public class NoContractsFromMethod implements ContractsFromMethod {

    /** Creates a NoContractsFromMethod object. */
    public NoContractsFromMethod() {}

    /**
     * Returns an empty set.
     *
     * @param executableElement the method or constructor whose contracts to retrieve
     * @return an empty set
     */
    @Override
    public Set<Contract> getContracts(ExecutableElement executableElement) {
        return Collections.emptySet();
    }

    /**
     * Returns an empty set
     *
     * @param executableElement the method whose contracts to return
     * @return an empty set
     */
    @Override
    public Set<Contract.Precondition> getPreconditions(ExecutableElement executableElement) {
        return Collections.emptySet();
    }

    /**
     * Returns an empty set
     *
     * @param executableElement the method whose contracts to return
     * @return an empty set
     */
    @Override
    public Set<Contract.Postcondition> getPostconditions(ExecutableElement executableElement) {
        return Collections.emptySet();
    }

    /**
     * Returns an empty set.
     *
     * @param methodElement the method whose contracts to return
     * @return an empty set
     */
    @Override
    public Set<Contract.ConditionalPostcondition> getConditionalPostconditions(
            ExecutableElement methodElement) {
        return Collections.emptySet();
    }
}

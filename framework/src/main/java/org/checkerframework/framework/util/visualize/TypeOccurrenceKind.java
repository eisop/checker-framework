package org.checkerframework.framework.util.visualize;

/** Types can occurr in different kinds of positions. */
public enum TypeOccurrenceKind {
    /** The type of the tree at its use site. */
    USE_TYPE,
    /**
     * The declared type of the tree. For a method, it should be the method's signature. For a
     * field, it should be the type of the field in its declaration.
     */
    DECLARED_TYPE,
    /** The declared type of the LHS of an assignment or compound assignment tree. */
    ASSIGN_LHS_DECLARED_TYPE,
    /**
     * The type of the RHS of an assignment or compound assignment tree.
     *
     * <p>For a postfix operation, it can be considered as a special assignment tree, in which the
     * LHS is returned and the RHS is the new value of the variable. In this situation, this message
     * kind means the type of the new value of the variable.
     */
    ASSIGN_RHS_TYPE,
}

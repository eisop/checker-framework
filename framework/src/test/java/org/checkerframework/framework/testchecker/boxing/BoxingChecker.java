package org.checkerframework.framework.testchecker.boxing;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A test type system in which boxing changes the qualifier.
 *
 * <p>Hierarchy:
 *
 * <pre>
 *      BoxUnknown
 *       /      \
 *    Alpha     Beta
 *       \      /
 *      BoxBottom
 * </pre>
 *
 * <p>A stub declares {@code Integer.valueOf} as {@code @Alpha Integer valueOf(@Alpha int)}, so
 * boxing a {@code @Beta int} is a type error. {@code Alpha} and {@code Beta} are siblings, so
 * neither is a subtype of the other and substituting one for the other is unsound.
 */
public class BoxingChecker extends BaseTypeChecker {}

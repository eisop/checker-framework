package org.checkerframework.framework.testchecker.standalonediamond;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A test checker for the dataflow value of a diamond nested in an enclosing type argument
 * inference, which {@link
 * org.checkerframework.framework.type.GenericAnnotatedTypeFactory#getStandaloneTypeForNodeValue}
 * may compute without that inference. Its qualifiers are {@link
 * org.checkerframework.framework.testchecker.util.Encrypted} and {@link
 * org.checkerframework.framework.testchecker.util.PolyEncrypted}, so that a constructor can be
 * qualifier-polymorphic. Its visitor also reports a dataflow value of a diamond whose underlying
 * type is not javac's type of the diamond.
 */
public class StandaloneDiamondChecker extends BaseTypeChecker {

    /** Creates a StandaloneDiamondChecker. */
    public StandaloneDiamondChecker() {}
}

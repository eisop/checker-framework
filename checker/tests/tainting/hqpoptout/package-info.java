// This file and the classes beside it are deliberately in one directory so that the package
// annotation and the subpackage class are compiled together.
@HasQualifierParameter(value = Tainted.class, applyToSubpackages = false)
package hqpoptout;

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

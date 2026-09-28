// This file and the classes beside it are deliberately in one directory: @AnnotatedFor is
// source-retention, so a package annotation only reaches other compilation units when its
// package-info is compiled in the same run.
@AnnotatedFor(value = "subtyping", applyToSubpackages = false)
package afoptout;

import org.checkerframework.framework.qual.AnnotatedFor;

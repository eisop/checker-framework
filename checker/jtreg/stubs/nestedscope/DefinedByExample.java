package nestedscope;

// A minimal, non-JDK-internal stand-in for com.sun.tools.javac.util.DefinedBy, which is not
// reliably resolvable from a test's classpath (it lives in the jdk.compiler module). Its
// "Outer.Inner.CONSTANT" scope shape -- an annotation with a nested enum, referenced through the
// annotation's own (imported) simple name rather than the nested enum's simple name -- is what
// UsesDefinedBy.astub exercises; see that file for the construct this pins. The package
// declaration matters: without it, "DefinedByExample.Api" is itself a resolvable canonical name
// in the default package, masking the bug this test pins.
public @interface DefinedByExample {
    /** The nested enum whose constants are the annotation's possible values. */
    enum Api {
        /** An arbitrary enum constant, standing in for {@code DefinedBy.Api.COMPILER}. */
        COMPILER
    }

    /**
     * The API this member is defined by.
     *
     * @return the API this member is defined by
     */
    Api value();
}

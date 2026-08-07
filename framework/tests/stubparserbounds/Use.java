import org.checkerframework.framework.testchecker.typedeclbounds.quals.Bottom;

import java.util.Iterator;

// The annotation file tests/stubparserbounds/typeparambound.astub declares the library type
// java.util.Iterator (loaded from bytecode, whose element model carries no qualifiers) as:
//   interface Iterator<E extends @Bottom Object> { @Bottom E next(); }
// This test confirms that the @Bottom type-use annotation on the type parameter's upper bound
// reaches the loaded AnnotatedTypeVariable's upper bound: a type argument that is not @Bottom
// then violates the declared bound, and one that is @Bottom satisfies it.
public class Use {

    // The default qualifier for String is @S1, which is not a subtype of the @Bottom upper bound,
    // so this use is rejected. Without the stub-supplied bound annotation the upper bound would
    // default to @Top and this would be accepted -- so the diagnostic proves the bound was applied.
    // :: error: (type.argument.type.incompatible)
    void argAboveBound(Iterator<String> it) {}

    // A @Bottom String argument satisfies the @Bottom upper bound.
    void argAtBound(Iterator<@Bottom String> it) {}
}

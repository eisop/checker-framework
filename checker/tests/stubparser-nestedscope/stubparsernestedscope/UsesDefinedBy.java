// UsesDefinedBy.astub adds @DefinedByExample(DefinedByExample.Api.COMPILER) to bar(), using a
// scope ("DefinedByExample.Api") that is itself a field access, not a simple name -- see that
// file for the construct this pins.
package stubparsernestedscope;

public class UsesDefinedBy {
    void bar() {}
}

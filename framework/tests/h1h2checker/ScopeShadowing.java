import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S1;

@DefaultQualifier(value = H1S1.class, locations = TypeUseLocation.RETURN)
public class ScopeShadowing {

    // The enclosing scope sets the default for the H1 hierarchy to @H1S1.
    // The nearer scope (this method) sets the default for the H2 hierarchy to @H2S1.
    // Under the buggy shadowing logic, the nearer @H2S1 would shadow the enclosing @H1S1
    // simply because they both share the location TypeUseLocation.RETURN, which meant
    // that the H1 hierarchy would fallback to its standard default (H1Top) instead of H1S1.
    @DefaultQualifier(value = H2S1.class, locations = TypeUseLocation.RETURN)
    public Object testMultiHierarchyShadowing() {
        return null;
    }

    public void test() {
        // testMultiHierarchyShadowing() should return @H1S1 @H2S1 Object.
        // Assigning it to a variable explicitly typed as @H1S1 @H2S1 Object should be valid.
        // Under the bug, the return type would be @H1Top @H2S1 Object, causing an assignment error.
        @H1S1 @H2S1 Object result = testMultiHierarchyShadowing();
    }
}

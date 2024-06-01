// Simple test that the `@StubFiles({"junit-assertions.astub"})` in the Nullness Checker works
// correctly.

import org.junit.jupiter.api.Assertions;

class JUnitNull {
    {
        Assertions.assertEquals(null, "dummy");
    }
}

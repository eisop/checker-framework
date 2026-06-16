import org.checkerframework.checker.pico.qual.Immutable;

@Immutable public class EqualsTest {
    @java.lang.Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return false;
    }
}

class EqualsTest2 {
    @java.lang.Override
    // ::error: (override.param.invalid)
    public boolean equals(@Immutable Object obj) {
        if (this == obj) return true;
        return false;
    }
}

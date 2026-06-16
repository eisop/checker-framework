import org.checkerframework.checker.pico.qual.Immutable;

@Immutable enum Underline {
    NONE,
    SINGLE,
    DOUBLE,
    SINGLE_ACCOUNTING,
    DOUBLE_ACCOUNTING
}

public class EnumTest {
    Underline u;

    public void foo() {
        u.ordinal();
    }
}

import org.checkerframework.checker.pico.qual.Immutable;

interface ICMI_MutableInterface {}
;

@Immutable public class ImmutableClassMutableInterface implements ICMI_MutableInterface {
    public static void bar(ICMI_MutableInterface m) {}
    ;

    public static void foo() {
        ImmutableClassMutableInterface x = null;
        bar(x);
    }
}
;

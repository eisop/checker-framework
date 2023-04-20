import org.checkerframework.checker.interning.qual.Interned;

public class StaticFinalStringDefault {
    // the default type of str should not be @Interned, even though it's later refined to it.
    static final @Interned String str = "a";
}

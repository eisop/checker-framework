import org.checkerframework.checker.interning.qual.Interned;

public class StaticFinalStringDefault {
    // The default type of str is not @Interned, even though it is later refined to it.
    static final @Interned String str = "a";
}

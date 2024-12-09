import org.checkerframework.checker.nullness.qual.*;

@org.checkerframework.framework.qual.DefaultQualifier(Nullable.class)
public class ArrayArgs {

    public void test(@NonNull String[] args) {}

    public void test(Class<@NonNull ? extends java.lang.annotation.Annotation> cls) {}

    public void test() {
        test(NonNull.class);
        // :: error: (nullness.on.new.array)
        String[] s1 = new String[] {null, null, null};
        // :: error: (argument.type.incompatible)
        test(s1);
        // :: error: (nullness.on.new.array)
        String[] s2 = new String[] {"hello", null, "goodbye"};
        // :: error: (argument.type.incompatible)
        test(s2);
        // :: error: (assignment.type.incompatible)
        // :: error: (nullness.on.new.array)
        @NonNull String[] s3 = new String[] {"hello", null, "goodbye"};
        // :: error: (nullness.on.new.array)
        // :: error: (new.array.type.invalid)
        @NonNull String[] s4 = new String[3];

        // below is still not safe since everything is defaulting to Nullable
        // :: error: (nullness.on.new.array)
        String[] s5 = new String[] {"hello", "goodbye"};
        // :: error: (argument.type.incompatible)
        test(s5);
        // :: error: (nullness.on.new.array)
        @NonNull String[] s6 = new String[] {"hello", "goodbye"};
        test(s6);
    }
}

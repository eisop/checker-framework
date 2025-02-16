import org.checkerframework.common.value.qual.StringVal;

public class Issue6125B {
    // :: error: (assignment.type.incompatible)
    @StringVal("hello") String s = "world";
}

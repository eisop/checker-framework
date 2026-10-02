import org.checkerframework.common.value.qual.StringVal;

public class Issue6125A {
    // :: error: (assignment.type.incompatible)
    @StringVal("hello") String s = "goodbye";
}

import org.checkerframework.framework.testchecker.util.*;

// :: error: (super.invocation.invalid)
public @Odd class ClassAnnotations {

    ClassAnnotations c;

    public void test() {
        @Odd ClassAnnotations d = c;
    }
}

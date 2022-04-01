import org.checkerframework.framework.testchecker.implicitconversion.quals.*;

import java.util.ArrayList;
import java.util.List;

public class StringConcatConversion<T> {

    @Top List<? extends T> ts = new ArrayList<>();

    // :: error: (type.invalid.annotations.on.use)
    @Top String topString;

    void foo(@Top T t) {
        throwException("test normal top to bottom conversion" + ts);
        throwException("test type variable" + t);
        throwException("test wildcard" + ts.get(0));
    }

    void throwException(String s) {}
}

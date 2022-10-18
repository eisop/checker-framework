import org.checkerframework.checker.locationtest.qual.*;
import org.checkerframework.checker.nullness.qual.*;

import java.io.*;
import java.util.List;

/*
Check redundant annotations on below locations
(+ means that now CF reports an expected warning on this location
 - means that CF doesn't report an expected warning on this location, mostly because compiler
 doesn't store all explicit annotations in underlying typemirrors)

1. Field                                           +
2. Local Variable                                  +
3. Parameter (includes Receiver Parameter)         +
4. Exception Parameter                             +
5. Resource Variable                               +
6. Return Type                                     +
7. Enum Constant                                   - (cannot get explicit annotations here)
8. Constructor Result                              - (cannot get explicit annotations here)
9. Wildcard upper bound and lower bound            - (cannot get explicit annotations here)
10. Type Parameter upper bound and lower bound     - (cannot get explicit annotations here)
11. Type (class, interface or Enum)                - (cannot get explicit annotations here)
12. TypeCast                                       - (cannot get explicit annotations here)
13. InstanceOf                                     - (cannot get explicit annotations here)
14. Object Creation                                - (cannot get explicit annotations here)
15. Component Type                                 - (cannot get explicit annotations here)
*/

@NonNull class RedundantAnnotation<
        T extends @Nullable Object> { // expects a "redundant.anno" warning on the extends

    enum InnerEnum {
        // expects a "redundant.anno" warning on the enum constant
        // :: error: ("nullness.on.enum")
        @NonNull EXPLICIT,
        IMPLICIT,
    }

    // :: warning: ("redundant.anno")
    @NonNull Object f;

    // :: warning: ("redundant.anno")
    @NonNull Integer foo(InputStream arg) {
        // :: warning: ("redundant.anno")
        @Nullable Object local;
        return Integer.valueOf(1);
    }

    // expects a "redundant.anno" warning on the constructor
    // :: error: ("nullness.on.constructor")
    // :: warning: ("redundant.anno")
    @NonNull RedundantAnnotation(@NonNull Integer i) {
        f = new Object();
    }

    void bar(@NonNull RedundantAnnotation<T> this, InputStream arg) throws Exception {
        // :: warning: ("redundant.anno")
        try (@Nullable InputStream in = arg) {

            // :: warning: ("redundant.anno")
            // :: warning: ("nullness.on.exception.parameter")
        } catch (@NonNull Exception e) {

        }

        // expects a "redundant.anno" warning on the upper bound
        List<? extends @Nullable Object> l;
        // expects a "redundant.anno" warning on the lower bound
        List<? super @Nullable Object> l2;

        Object obj = null;
        // expects a "redundant.anno" warning on the typecast
        String x = (@Nullable String) obj;

        // :: error: ("instanceof.nullable")
        boolean b = x instanceof @Nullable String;

        // expects a "redundant.anno" warning on the component type
        @Nullable String[] strs = new String[10];
    }
}

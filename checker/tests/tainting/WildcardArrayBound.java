package wildcards;

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

import java.io.Serializable;

public class WildcardArrayBound {
    interface MyInterface {}

    abstract static class Other<U extends @Untainted Serializable> {
        void use1(
                Other<? extends @Untainted MyInterface @Untainted []> x,
                Other<@Tainted MyInterface @Untainted []> y) {
            Other<? extends @Untainted Serializable> z = y;
            // :: error: (assignment.type.incompatible)
            x = y;
        }

        void use(
                Other<? extends @Untainted MyInterface @Untainted []> x,
                Other<@Tainted MyInterface @Untainted []> y) {
            // :: error: (assignment.type.incompatible)
            x = y;
            @Untainted Serializable s = x.getU();
            @Untainted MyInterface @Untainted [] sw = x.getU();
        }

        abstract U getU();
    }

    abstract static class Another<U extends Serializable> {
        void use(Another<? extends MyInterface[]> x) {
            Serializable s = x.getU();
            MyInterface[] sw = x.getU();
        }

        abstract U getU();
    }
}

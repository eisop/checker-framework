import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;

public class OverrideEquals {
    class OverrideEqualsInner extends A {
        @Override
        void foo(@Readonly Object o) {}

        @Override
        public boolean equals(@Readonly Object o) {
            return super.equals(o);
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException exc) {
                throw new InternalError(); // should never happen since we are cloneable
            }
        }
    }

    class SubOverrideEquals extends OverrideEquals {
        @Override
        public boolean equals(@Readonly Object o) {
            return super.equals(new @Mutable Object());
        }
    }

    class ThrowableOverridingError extends Throwable {

        @Override
        public String getMessage() {
            return super.getMessage();
        }
    }

    class Test extends Throwable {
        @Override
        public String getMessage(@Readonly Test this) {
            // :: error: (method.invocation.invalid)
            return super.getMessage();
        }
    }

    class A {
        void foo(@Readonly Object o) {}
    }
}

import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

import java.util.Date;

// TODO Understand polymutable creation
@ReceiverDependentMutable public class SupportedBuilderPattern {
    private final int id;
    private String address;
    private @Immutable Date date;

    private @ReceiverDependentMutable SupportedBuilderPattern(Builder builder) {
        this.id = builder.id;
        this.address = builder.address;
        this.date = builder.date;
    }

    public static @Mutable class Builder {
        private final int id;
        private String address;
        private @Immutable Date date;

        public Builder(int id) {
            this.id = id;
        }

        public Builder withAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder withDate(@Immutable Date date) {
            this.date = date;
            return this;
        }

        public @PolyMutable SupportedBuilderPattern build() {
            return new @PolyMutable SupportedBuilderPattern(this);
        }
    }

    class Test {
        void test(String[] args) {
            SupportedBuilderPattern.@Mutable Builder builder =
                    new SupportedBuilderPattern.Builder(0);
            @Mutable SupportedBuilderPattern msbp =
                    builder.withAddress("10 King St.").withDate(new @Immutable Date()).build();
            @Immutable SupportedBuilderPattern imsbp =
                    builder.withAddress("1 Lester St.").withDate(new @Immutable Date()).build();
        }
    }
}

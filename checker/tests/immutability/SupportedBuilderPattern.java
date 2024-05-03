import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.PolyMutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

import java.util.Date;

@ReceiverDependantMutable
public class SupportedBuilderPattern {
    private final int id;
    private String address;
    private @Immutable Date date;

    private @ReceiverDependantMutable SupportedBuilderPattern(Builder builder) {
        this.id = builder.id;
        this.address = builder.address;
        this.date = builder.date;
    }

    public static class Builder {
        private final int id;
        private String address;
        private @Immutable Date date;

        // :: error: (initialization.fields.uninitialized)
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
}

class Test {
    public static void main(String[] args) {
        SupportedBuilderPattern.@Mutable Builder builder = new SupportedBuilderPattern.Builder(0);
        @Mutable
        SupportedBuilderPattern msbp =
                builder.withAddress("10 King St.").withDate(new @Immutable Date()).build();
        @Immutable
        SupportedBuilderPattern imsbp =
                builder.withAddress("1 Lester St.").withDate(new @Immutable Date()).build();
    }
}

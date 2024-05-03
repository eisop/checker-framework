import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Readonly;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

/**
 * This builder pattern is NOT supported if the builder internally holds the constructed object
 * rather than having the representation states. Update: this is fake builder pattern from
 * Wikipedia!
 */
public class UnsupportedCarBuilder {
    @ReceiverDependantMutable
    class Car {
        private int wheels;
        private String color;

        // :: error: (initialization.fields.uninitialized)
        private @Immutable Car() {}

        public String getColor(@Readonly Car this) {
            return color;
        }

        private void setColor(final String color) {
            this.color = color;
        }

        public int getWheels(@Readonly Car this) {
            return wheels;
        }

        private void setWheels(final int wheels) {
            this.wheels = wheels;
        }

        @Override
        public String toString(@Readonly Car this) {
            return "Car [wheels = " + wheels + ", color = " + color + "]";
        }
    }

    private @Immutable Car car;

    public UnsupportedCarBuilder() {
        car = new @Immutable Car();
    }

    public UnsupportedCarBuilder setColor(final String color) {
        // :: error: (method.invocation.invalid)
        car.setColor(color);
        return this;
    }

    public UnsupportedCarBuilder setWheels(final int wheels) {
        // :: error: (method.invocation.invalid)
        car.setWheels(wheels);
        return this;
    }

    public static UnsupportedCarBuilder getBuilder() {
        return new UnsupportedCarBuilder();
    }

    public @Immutable Car build() {
        return car;
    }
}

class A {
    void foo() {
        UnsupportedCarBuilder.@Immutable Car car =
                UnsupportedCarBuilder.getBuilder().setColor("red").setWheels(4).build();
    }
}

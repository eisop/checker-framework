import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

public class ConstructorDefaulting {

    @ReceiverDependentMutable static class GenericClass<T extends @Readonly Object> {}

    @ReceiverDependentMutable static class GenericConstructor {
        <T extends @Readonly Object> GenericConstructor(T value) {}
    }

    void classTypeArguments() {
        // Diamond inference requests the constructor type both with and without type inference.
        @Mutable GenericClass<@Mutable Object> inferred = new GenericClass<>();
        @Mutable GenericClass<@Mutable Object> explicit = new GenericClass<@Mutable Object>();
    }

    void constructorTypeArguments(@Mutable Object value) {
        // A generic constructor also requests both constructor-resolution paths.
        @Mutable GenericConstructor inferred = new GenericConstructor(value);
        @Mutable GenericConstructor explicit = new <@Mutable Object>GenericConstructor(value);
    }
}

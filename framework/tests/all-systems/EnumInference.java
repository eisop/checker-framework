// Inferring a type variable bounded by Enum<T> from an enum argument, where the assignment target
// is also an upper bound: the type argument of the target's supertype Enum<E> carries the target's
// primary qualifier, which must not be forced onto T.
public class EnumInference {
    enum Prefix {
        one,
        kilo
    }

    static <T extends Enum<T>> T getElementValueEnum(
            Object anno, Class<T> expectedType, T defaultValue) {
        return defaultValue;
    }

    static <T extends Enum<T>> T getElementValueEnum2(Object anno, Class<T> expectedType) {
        return Enum.valueOf(expectedType, "one");
    }

    static <T extends Enum<T>> T fromClass(Class<T> expectedType) {
        return Enum.valueOf(expectedType, "one");
    }

    void m(Object anno) {
        Prefix p = getElementValueEnum(anno, Prefix.class, Prefix.one);
        Prefix q = getElementValueEnum2(anno, Prefix.class);
        Prefix r = fromClass(Prefix.class);
        Enum<?> s = getElementValueEnum(anno, Prefix.class, Prefix.one);
    }
}

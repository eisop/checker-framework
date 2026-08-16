// Test case for casts whose cast type is not a supertype of the type of the cast expression.

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked") // The casts are unchecked casts.
public class Downcast {
    interface Supplier<T extends @Nullable Object> {}

    interface SubSupplier<T extends @Nullable Object> extends Supplier<T> {}

    interface Unrelated<T extends @Nullable Object> {}

    interface UnrelatedNoTypeArgs {}

    <T extends @Nullable Object> void downcastTypeVariable(Supplier<T> supplier) {
        SubSupplier<T> cast = (SubSupplier<T>) supplier;
    }

    void downcastSafe(Supplier<@Nullable String> supplier) {
        SubSupplier<@Nullable String> cast = (SubSupplier<@Nullable String>) supplier;
    }

    void downcastUnsafe(Supplier<@Nullable String> supplier) {
        // :: warning: (cast.unsafe)
        SubSupplier<String> cast = (SubSupplier<String>) supplier;
    }

    void downcastUnsafeNested(Supplier<List<@Nullable String>> supplier) {
        // :: warning: (cast.unsafe)
        SubSupplier<List<String>> cast = (SubSupplier<List<String>>) supplier;
    }

    void downcastFromObject(Object o) {
        // The cast type has type arguments that the expression's type does not have.
        // :: warning: (cast.unsafe)
        List<String> cast = (List<String>) o;
    }

    void downcastToSubclass(List<String> list) {
        ArrayList<String> cast = (ArrayList<String>) list;
    }

    void crossCast(Supplier<String> supplier) {
        // Nothing is known about the type arguments of an unrelated type.
        // :: warning: (cast.unsafe)
        Unrelated<String> cast = (Unrelated<String>) supplier;
    }

    void crossCastNoTypeArgs(Supplier<String> supplier) {
        // The warning is issued because the cast type and the expression's type have a different
        // number of type arguments.
        // :: warning: (cast.unsafe)
        UnrelatedNoTypeArgs cast = (UnrelatedNoTypeArgs) supplier;
    }
}

// An override whose receiver uses the class's own type parameters is never a narrowing, even when a
// default puts a qualifier on every use of a type variable, as the JSpecify reference checker does
// in unmarked code.  These overrides were reported as override.receiver.invalid.

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
public class ReceiverDefaultedTypeVarUse<E> {
    // Implicit receiver.
    @Override
    public String toString() {
        return "";
    }

    // Explicit receiver that writes no annotation.
    @Override
    public int hashCode(ReceiverDefaultedTypeVarUse<E> this) {
        return 0;
    }

    // Explicit receiver of an inner class that does not spell out the enclosing type.
    class Inner {
        @Override
        public int hashCode(Inner this) {
            return 0;
        }
    }
}

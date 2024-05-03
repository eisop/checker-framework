import org.checkerframework.checker.immutability.qual.*;

public class RDMField {

    @Mutable
    private static class MutableClass {
        int field = 0;
    }

    @ReceiverDependantMutable
    private static class RDMHolder {

        // :: error: (type.invalid.annotations.on.use)
        @ReceiverDependantMutable MutableClass field = new MutableClass();
        @Mutable MutableClass mutableField = new MutableClass();

        public @PolyMutable MutableClass getField(@PolyMutable RDMHolder this) {
            return field;
        }

        public void setField(@Mutable RDMHolder this, MutableClass field) {
            this.field = field;
        }

        void asImmutable(@Immutable RDMHolder r) {
            // :: error: (illegal.field.write)
            r.field.field = 1;
            // :: error: (illegal.field.write)
            r.getField().field = 1;
            // :: error: (method.invocation.invalid)
            r.setField(new MutableClass());
        }
    }

    @Immutable
    private static class ImmutableHolder {
        // :: error: (type.invalid.annotations.on.use)
        @ReceiverDependantMutable MutableClass field = new MutableClass();

        public @PolyMutable MutableClass getField(@PolyMutable ImmutableHolder this) {
            return field;
        }
    }
}

// A cast from a primitive type to a reference type boxes the value, and Integer.valueOf returns a
// @NewObject. The cast converts that boxed value, so it is not an unsafe downcast from the
// primitive's @GuardedBy({}).
public class BoxingCast {
    Object boxInt(int i) {
        return (Integer) i;
    }

    Object boxToObject(long l) {
        return (Object) l;
    }

    Integer boxThenUse(int i) {
        Integer b = (Integer) i;
        return b;
    }
}

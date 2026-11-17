// A cast from a primitive type to a reference type boxes the value. A cast to a primitive type is
// @Interned, and the boxing cast takes that qualifier, as boxing does in an assignment.
public class BoxingCast {
    Object boxCastPrimitive(double d) {
        return (Object) (float) d;
    }

    Object boxPrimitive(int i) {
        return (Object) i;
    }
}

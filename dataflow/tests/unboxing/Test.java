// The CFG models the boxing and unboxing conversions of casts (JLS 5.5), of the operands of && and
// || (JLS 15.23, 15.24), and of a return statement (JLS 14.17). Each is a synthetic call of
// valueOf, or of intValue and the like, on the converted value.
public class Test {
    public long test(Integer boxed, Object obj, Boolean b, boolean c) {
        int unboxCast = (int) boxed;
        long widenCast = (long) boxed;
        int objCast = (int) obj;
        Integer boxCast = (Integer) 5;
        Object objBox = (Object) 6;
        double unboxWidenDouble = (double) boxed;
        double objCastDouble = (double) obj;
        char objCastChar = (char) obj;
        Long boxLong = (Long) 7L;
        boolean and = b && c;
        boolean or = c || b;
        return boxed;
    }
}

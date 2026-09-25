import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.qual.UnknownVal;
import org.checkerframework.framework.testchecker.lib.UncheckedByteCode;

public class TestUncheckedByteCode {
    @UnknownVal Object field;

    void test(
            UncheckedByteCode<Object> param,
            @BottomVal int bottomInt,
            @BottomVal String bottomString,
            @BottomVal Object bottomObject) {
        // Return types default to top (@UnknownVal).
        @UnknownVal int r1 = param.getInt(bottomInt);
        @UnknownVal String r2 = param.getString(bottomString);
        @UnknownVal Object r3 = param.getObject(bottomObject);

        // Assigning return values to specific values fails because they default to @UnknownVal.
        // :: error: (assignment.type.incompatible)
        @IntVal(1) int specificInt = param.getInt(bottomInt);
        // :: error: (assignment.type.incompatible)
        @StringVal("hello") String specificString = param.getString(bottomString);

        // Parameters default to @BottomVal, so passing non-bottom values fails.
        // :: error: (argument.type.incompatible)
        param.getInt(1);

        // :: error: (argument.type.incompatible)
        param.getString("hello");

        // :: error: (argument.type.incompatible)
        param.getObject(new Object());

        // Fields default to top (@UnknownVal).
        // :: error: (assignment.type.incompatible)
        @IntVal(1) Object specificField = UncheckedByteCode.nonFinalPublicField;
        @UnknownVal Object unkField = UncheckedByteCode.nonFinalPublicField;
    }
}

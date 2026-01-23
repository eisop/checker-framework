import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

import viewpointtest.quals.*;

@DefaultQualifier(
        value = A.class,
        locations = {TypeUseLocation.TYPE})
public class DefaultQualifierTest {
    // :: error: (type.invalid.annotations.on.use) :: error: (super.invocation.invalid) :: warning:
    // (inconsistent.constructor.type)
    @B DefaultQualifierTest() {}
}

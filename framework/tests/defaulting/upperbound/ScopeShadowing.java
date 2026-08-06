import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.defaulting.UpperBoundQual.UbExplicit;
import org.checkerframework.framework.testchecker.defaulting.UpperBoundQual.UbImplicit;

@DefaultQualifier(value = UbExplicit.class, locations = TypeUseLocation.RETURN)
public class ScopeShadowing {

    @DefaultQualifier(value = UbImplicit.class, locations = TypeUseLocation.RETURN)
    public Object testShadowing() {
        return null;
    }

    public void test() {
        @UbImplicit Object result = testShadowing();
    }
}

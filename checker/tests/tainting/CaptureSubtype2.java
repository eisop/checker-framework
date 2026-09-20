import org.checkerframework.checker.tainting.qual.Untainted;

import java.util.function.Function;

public class CaptureSubtype2 {

    interface FFunction<T, R> extends Function<T, R> {}

    interface DInterface {}

    interface MInterface<P> {}

    interface QInterface<K extends MInterface<P>, V extends MInterface<P>, P> {}

    FFunction<String, QInterface<?, @Untainted ?, DInterface>> r;

    CaptureSubtype2(
            FFunction<
                            String,
                            QInterface<
                                    ? extends MInterface<DInterface>,
                                    ? extends MInterface<DInterface>,
                                    DInterface>>
                    r) {
        // :: error: (assignment.type.incompatible)
        this.r = r;
    }
}

import java.math.BigDecimal;
import java.math.MathContext;

public abstract class BigPrefix {

    public static final BigDecimal YOCTO =
            new BigDecimal("0.000000000000000000000001", MathContext.DECIMAL128);
}

abstract class PrimitivePrefix {

    public static final double YOCTO = BigPrefix.YOCTO.doubleValue();
}

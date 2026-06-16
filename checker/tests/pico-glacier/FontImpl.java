import org.checkerframework.checker.pico.qual.*;

@Immutable interface SColor {}

@Immutable abstract class AbstractColorAdv implements SColor {}

// ::error: (glacier.subclass.mutable)
class FI_ColorImpl extends AbstractColorAdv {
    // Arguably it would be preferable for this to not be an error.
    // ::error: (assignment.type.incompatible)
    public static final AbstractColorAdv BLACK = new FI_ColorImpl("#000000");

    FI_ColorImpl(String fontColor) {}
}

public class FontImpl {
    FontImpl(String fontColor) {
        // Arguably it would be preferable for this to not be an error.
        // ::error: (assignment.type.incompatible)
        SColor a = new FI_ColorImpl(fontColor);

        // Arguably it would be preferable for this to not be an error either.
        // ::error: (type.invalid.annotations.on.use)
        SColor c = fontColor != null ? new FI_ColorImpl(fontColor) : null;
    }
}

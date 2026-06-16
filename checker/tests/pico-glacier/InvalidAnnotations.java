import org.checkerframework.checker.pico.qual.*;

// ::error: (type.invalid)
// @PICOBottom // Javac error in PICO
class InvalidBottom {}
;

public class InvalidAnnotations {
    // ::error: (type.invalid)
    InvalidBottom b;
}

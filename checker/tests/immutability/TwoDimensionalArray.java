import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

// A (main modifier is here) [] []
public class TwoDimensionalArray {

    public double @ReceiverDependantMutable [][] test() {
        @Immutable
        double @ReceiverDependantMutable [] @Mutable [] C =
                new @Immutable double @ReceiverDependantMutable [0][0];
        for (@Immutable int i = 0; i < 0; i++) {
            for (@Immutable int j = 0; j < 0; j++) {
                // Array C's main modifier is @ReceiverDependantMutable, so mutating C is not
                // allowed
                // :: error: (illegal.array.write)
                C[i] = new double[] {1.0};
                // But C[i] is double @Mutable [](mutable array of double elements), so mutating
                // C[i] is ALLOWED
                C[i][j] = 1.0;
            }
        }
        return C;
    }
}

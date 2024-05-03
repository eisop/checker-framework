// As referred to NullnessPropagationTreeAnnotator#visitBinary() and
// NullnessPropagationTreeAnnotator#visitUnary(): result type for these two types
// are always @Initialized.
public class PritimitveReturn {

    // Shouldn't have return.type.incompatible: @UnderInitialization and @Initialized
    public static double binomial() {

        double binomial = 1.0;
        int i = 1;
        if (false) {
            // Here, previously before this commit, binomial becomes UnderInitialization@
            binomial *= 1.0 / (double) (i--);
        }
        // Shouldn't have @UnderInitialization anymore for binomial
        return binomial;
    }

    // Similar to above
    public static double fac1() {

        long d = 1;
        long i = 1;
        if (false) {
            // Similar to above
            d *= i--;
        }
        // Similar to above
        return -d;
    }
}

import org.checkerframework.common.value.qual.IntRange;

// Loops whose condition compares the loop variable with a bound that is not an integer literal.
// The loop variable jumps straight to the range the bound permits, so dataflow reaches the fixed
// point without climbing through the widening values; the result must be as precise as before.
public class LoopBoundRefinement {
    void shortVarLt() {
        short s = 0;
        while (s < Short.MAX_VALUE) {
            @IntRange(from = 0, to = 32766) short in = s;
            s++;
        }
        @IntRange(from = 32767, to = 32767) short after = s;
    }

    void charVarLt() {
        char c = 0;
        while (c < Character.MAX_VALUE) {
            @IntRange(from = 0, to = 65534) char in = c;
            c++;
        }
        @IntRange(from = 65535, to = 65535) char after = c;
    }

    void intVarLeShort() {
        int i = 0;
        while (i <= Short.MAX_VALUE) {
            @IntRange(from = 0, to = 32767) int in = i;
            i++;
        }
        @IntRange(from = 32768, to = 32768) int after = i;
    }

    void intVarLtChar() {
        int i = 0;
        while (i < Character.MAX_VALUE) {
            @IntRange(from = 0, to = 65534) int in = i;
            i++;
        }
        @IntRange(from = 65535, to = 65535) int after = i;
    }

    void intVarLeChar() {
        int i;
        for (i = 0; i <= Character.MAX_VALUE; i++) {
            @IntRange(from = 0, to = 65535) int in = i;
        }
        @IntRange(from = 65536, to = 65536) int after = i;
    }

    void intVarLtByte() {
        int i;
        for (i = 0; i < Byte.MAX_VALUE; i++) {
            @IntRange(from = 0, to = 126) int in = i;
        }
        @IntRange(from = 127, to = 127) int after = i;
    }

    void intVarLtInt() {
        int i;
        for (i = 0; i < Integer.MAX_VALUE; i++) {
            @IntRange(from = 0, to = Integer.MAX_VALUE - 1) int in = i;
        }
        @IntRange(from = Integer.MAX_VALUE, to = Integer.MAX_VALUE) int after = i;
    }

    void intVarGtShortMin() {
        int i = 0;
        while (i > Short.MIN_VALUE) {
            i--;
        }
        @IntRange(from = -32768, to = -32768) int after = i;
    }

    void longVarLeChar() {
        long l = 0;
        while (l <= Character.MAX_VALUE) {
            l++;
        }
        @IntRange(from = 65536, to = 65536) long after = l;
    }

    void intVarLtParam(@IntRange(from = 0, to = 1000) int n) {
        int i;
        for (i = 0; i < n; i++) {
            @IntRange(from = 0, to = 999) int in = i;
        }
        @IntRange(from = 0, to = 1000) int after = i;
    }

    void nestedVarBounds(@IntRange(from = 0, to = 100) int n, @IntRange(from = 0, to = 50) int m) {
        for (int i = 0; i < n; i++) {
            for (int j = i; j <= m; j++) {
                @IntRange(from = 0, to = 50) int inJ = j;
                for (int k = m; k > i; k--) {
                    @IntRange(from = 1, to = 50) int inK = k;
                }
            }
            @IntRange(from = 0, to = 99) int inI = i;
        }
    }
}

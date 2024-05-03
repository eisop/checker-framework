import java.util.concurrent.atomic.AtomicInteger;

// Test case for issue 1727: https://github.com/typetools/checker-framework/issues/1727

// @skip-test until the issue is fixed

class A {
    A(B b) {}
}

class B {}

public class LocalVariableUsedAsArgument {

    private A a;

    private A foo() {
        // Default type for local variable b is @UnknownInitialization @Readonly
        B b;
        // B b = null; Chaning to this line doesn't have errors

        //        // Simplified version of testcase that has the same effect as the while loop
        // below.
        //        if (true) {
        //            // Statically this is not guaranteed to be executed
        //            b = new B();
        //        }

        // Similar structure in exp4j#Tokenizer
        while (true) {
            B op = getB();
            if (op == null) {
                b = new B();
                break;
            } else {
                b = op;
                break;
            }
        }

        a = new A(b); // here b is still @UnknownInitialization @Readonly. Why? Is this
        // problem in CF generally? Nullness also has
        // the s
        return a;
    }

    private B getB() {
        return null;
    }

    public static int ThrsafeIncrementSizeUpToLimit(AtomicInteger storagePointer, int limitValue) {
        int resultValue;
        while (true) {
            resultValue = storagePointer.get();
            if (resultValue == limitValue) {
                break;
            }
            //            if (ThrsafeCompareExchange(storagePointer, resultValue, (resultValue +
            // 1))) {
            //                break;
            //            }
        }
        return resultValue;
    }

    // static boolean ThrsafeCompareExchange(AtomicInteger storagePointer, int val1, int val2) {
    //        return true;
    //    }
}

import org.checkerframework.checker.pico.qual.Readonly;

// I am not sure why this test case exist previous but adding here first.
public class Operators {
    // :: error: (initialization.field.uninitialized)
    @Readonly Object o;

    String testBinaryOperator() {
        return "Object is: " + o;
    }

    void testUnaryOperator() {
        int result = +1;
        int a = result--;
        int b = result++;
        result = -result;
        System.out.println(result);
        boolean success = false;
        System.out.println(success);
        Integer i = 0;
        i += 2;
    }

    class A {
        int size() {
            return 0;
        }
    }

    void foo(A a) {
        double mean1 = mean(a);
    }

    static double mean(A a) {
        return sum(a) / a.size();
    }

    static double sum(A a) {
        return 1.0;
    }

    class UnaryAndCompoundAssignment {

        int counter = 0;

        public void next(@Readonly UnaryAndCompoundAssignment this) {
            int lcouter = 0;
            lcouter++;
            // :: error: (illegal.field.write)
            counter++;
            lcouter += 5;
            // :: error: (illegal.field.write)
            counter += 5;
        }
    }
}

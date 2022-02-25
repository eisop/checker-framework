<<<<<<< HEAD:dataflow/tests/reach-definition/Test.java
// This file may not be renamed; it has to have the same filename as ../issue3447/Test.java .
=======
>>>>>>> a92791949 (rename reach definition to reach definitions;refine related task in build.gradle under dataflow):dataflow/tests/reach-definitions/Test.java
public class Test {
    public int test() {
        int a = 1, b = 2, c = 3;
        if (a > 0) {
            int d = a + c;
        } else {
            int e = a + b;
        }
        b = 0;
        a = b;
        return a;
    }
}

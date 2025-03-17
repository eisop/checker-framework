// A similar test is in
// framework/tests/all-systems/EnumSwitch.java
public class Issue6260 {
    enum MyE {
        FOO;

        MyE getIt() {
            return FOO;
        }

        String go() {
            MyE e = getIt();
            switch (e) {
                case FOO:
                    return "foo";
            }
            // This is not dead code!
            throw new AssertionError(e);
        }
    }
}

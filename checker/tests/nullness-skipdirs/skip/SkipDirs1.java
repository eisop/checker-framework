public class SkipDirs1 {

    static class DontSkipMe {
        static Object foo() {
            // :: error: (return.type.incompatible)
            return null;
        }
    }

    static class DontSkip {
        static Object foo() {
            // :: error: (return.type.incompatible)
            return null;
        }
    }
}

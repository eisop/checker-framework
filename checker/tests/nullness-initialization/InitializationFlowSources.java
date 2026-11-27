// The Initialization Checker skips the dataflow analysis of a method that cannot compute a type
// other than @Initialized. Local variables have the top type by default, so the methods of
// UsesLocals are only correct if their local variables still get @Initialized. Each method of
// Sources has a single source of a type other than @Initialized, so it must be analyzed.

import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

class InitializationFlowSources {

    static class UsesLocals {
        Object field = new Object();

        void receiver() {
            Object o = new Object();
            o.toString();
        }

        void argument(List<String> list) {
            String s = "a";
            list.add(s);
        }

        int primitive(String s) {
            int i = 0;
            i++;
            return s.charAt(i);
        }

        <T> T typeVariable(List<T> list) {
            T t = list.get(0);
            return t;
        }

        void chainedAssignment() {
            Object o;
            field = o = new Object();
            o.toString();
        }

        void loop(List<String> list) {
            List<String> copy = new ArrayList<>();
            for (String s : list) {
                copy.add(s);
            }
            copy.toString();
        }

        Function<String, Integer> lambda() {
            return s -> {
                String t = s.trim();
                return t.length();
            };
        }
    }

    static class Sources {
        @UnknownInitialization Object unknown() {
            return new Object();
        }

        void fromInvokedMethod() {
            Object o = unknown();
            // :: error: (method.invocation.invalid)
            o.toString();
        }

        void fromParameter(@UnknownInitialization Object p) {
            Object o = p;
            // :: error: (method.invocation.invalid)
            o.toString();
        }

        void fromReceiver(@UnderInitialization(Sources.class) Sources this) {
            Object o = this;
            // :: error: (method.invocation.invalid)
            o.toString();
        }
    }
}

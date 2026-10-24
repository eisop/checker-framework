// Test case for https://github.com/typetools/checker-framework/issues/5402

import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.calledmethods.qual.RequiresCalledMethods;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.checkerframework.dataflow.qual.Deterministic;

public class Issue5402 {}

class Issue5402_Ok1 {

    public @This Issue5402_Ok1 bar() {
        return this;
    }

    public void baz(@CalledMethods("bar") Issue5402_Ok1 this) {}

    public static void test() {
        final Issue5402_Ok1 foo = new Issue5402_Ok1();
        foo.bar().baz(); // No error
    }
}

class Issue5402_Ok2 {

    public @This Issue5402_Ok2 bar() {
        return this;
    }

    @RequiresCalledMethods(value = "this", methods = "bar")
    public void baz() {}

    public static void test() {
        final Issue5402_Ok2 foo = new Issue5402_Ok2();
        final Issue5402_Ok2 foo1 = foo.bar();
        foo1.baz(); // No error
    }
}

// The same as Issue5402_Ok2, with @Deterministic on bar().  Hypothesis: @Deterministic documents an
// earlier workaround for the chaining problem; it no longer changes the outcome of this test.
class Issue5402_Ok3 {

    @Deterministic
    public @This Issue5402_Ok3 bar() {
        return this;
    }

    @RequiresCalledMethods(value = "this", methods = "bar")
    public void baz() {}

    public static void test() {
        final Issue5402_Ok3 foo = new Issue5402_Ok3();
        final Issue5402_Ok3 foo1 = foo.bar();
        foo1.baz(); // No error
    }
}

class Issue5402_Bad {

    public @This Issue5402_Bad bar() {
        return this;
    }

    @RequiresCalledMethods(value = "this", methods = "bar")
    public void baz() {}

    public static void test() {
        final Issue5402_Bad foo = new Issue5402_Bad();
        // No error: the receiver of baz() is translated to foo, which has called bar().
        foo.bar().baz();
    }
}

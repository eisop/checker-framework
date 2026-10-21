/*
 * @test
 *
 * @summary Test that -Amode=jspecify handles field accesses without crashing (eisop#2259).  The
 * mode assumes that fields are initialized, so the Nullness Checker must not adapt a field access
 * to the initialization state of its receiver, whichever expression the receiver is.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify ModeFieldAccess.java
 */

import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class ModeFieldAccess {
    static String staticName = "";
    String name = "";
    ModeFieldAccess next = this;

    ModeFieldAccess() {
        // The receiver is under initialization here.
        String a = this.name;
        String b = name;
        String c = next.name;
    }

    String self() {
        return this.name;
    }

    String simpleName() {
        return name;
    }

    String other(ModeFieldAccess that) {
        return that.name;
    }

    String chain(ModeFieldAccess that) {
        return that.next.next.name;
    }

    String created() {
        return new ModeFieldAccess().name;
    }

    String fromStatic() {
        return ModeFieldAccess.staticName;
    }

    String fromCall() {
        return other(this).length() > 0 ? next().name : "";
    }

    ModeFieldAccess next() {
        return next;
    }

    class Inner {
        String outerField() {
            return name + ModeFieldAccess.this.name;
        }
    }

    static String outerAccess(ModeFieldAccess outer) {
        return outer.new Inner().outerField();
    }
}

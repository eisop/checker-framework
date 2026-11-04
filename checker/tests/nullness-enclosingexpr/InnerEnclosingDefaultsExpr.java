// With -AcheckEnclosingExpr, the enclosing expression of "o.new Inner()" is checked against the
// constructor's receiver.  That receiver is the type of "this" in the enclosing class unless it is
// written: a default of the inner class does not make it an InnerEnclosingDefaultsExpr<@NonNull O>.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

class InnerEnclosingDefaultsExpr<O> {
    O field;

    InnerEnclosingDefaultsExpr(O field) {
        this.field = field;
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
    class Inner {
        Inner() {
            // :: error: (dereference.of.nullable)
            field.toString();
        }

        Inner(InnerEnclosingDefaultsExpr<@NonNull O> InnerEnclosingDefaultsExpr.this, String s) {
            field.toString();
        }
    }

    static void use() {
        InnerEnclosingDefaultsExpr<@Nullable String> o = new InnerEnclosingDefaultsExpr<>(null);
        o.new Inner();
        // :: error: (enclosingexpr.type.incompatible)
        o.new Inner("");

        InnerEnclosingDefaultsExpr<@NonNull String> p = new InnerEnclosingDefaultsExpr<>("");
        p.new Inner();
        p.new Inner("");
    }
}

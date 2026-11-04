import java.util.List;

import viewpointtest.quals.*;

public class LostInvalidPositions {
    @ReceiverDependentQual Object f;
    @ReceiverDependentQual LostInvalidPositions f2;
    @ReceiverDependentQual int i;
    @A List<@ReceiverDependentQual Object> nested;

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @ReceiverDependentQual LostInvalidPositions(@ReceiverDependentQual Object args) {}

    @ReceiverDependentQual Object get() {
        return null;
    }

    @PolyVP LostInvalidPositions identity(@PolyVP LostInvalidPositions this) {
        return this;
    }

    void set(@ReceiverDependentQual Object o) {}

    @PolyVP Object polyGet(@PolyVP LostInvalidPositions this) {
        return null;
    }

    void polySet(@PolyVP LostInvalidPositions this, @PolyVP Object o) {}

    <T> void noParam() {}

    <T> T id(T t) {
        return t;
    }

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @ReceiverDependentQual <T> LostInvalidPositions() {}

    void test(@Top LostInvalidPositions obj, @Bottom Object bottomObj) {
        // :: error: (viewpointtest.lost.lhs)
        this.f = obj.f;
        // :: error: (viewpointtest.lost.lhs)
        this.f = bottomObj;

        // :: error: (assignment.type.incompatible)
        @A Object aObj = obj.get();
        // :: error: (assignment.type.incompatible)
        @B Object bObj = obj.get();
        // :: error: (assignment.type.incompatible)
        @Bottom Object botObj = obj.get();

        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.parameter)
        new LostInvalidPositions(obj.f);
        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.parameter)
        new LostInvalidPositions(bottomObj);

        // :: error: (viewpointtest.lost.parameter)
        this.set(obj.f);
        // :: error: (viewpointtest.lost.parameter)
        this.set(bottomObj);

        obj.f2.identity();

        // A polymorphic qualifier that resolves to Lost is not an invalid parameter.
        @Top Object polyResult = obj.f2.polyGet();
        obj.f2.polySet(aObj);
        obj.f2.polySet(bottomObj);
        // :: error: (viewpointtest.lost.parameter)
        obj.f2.set(bottomObj);

        // :: error: (viewpointtest.lost.type.argument)
        this.<@Lost Object>noParam();
        // :: error: (viewpointtest.lost.type.argument) :: error: (viewpointtest.lost.parameter)
        this.<@Lost Object>id(bottomObj);
        // :: error: (viewpointtest.lost.type.argument)
        this.<@A List<@Lost Object>>noParam();
        // An inferred Lost type argument is allowed, because Top is also a valid type argument.
        @Top Object inferred = this.id(obj.f);
        // :: error: (viewpointtest.lost.type.argument)
        new <@Lost Object>@A LostInvalidPositions();

        // :: error: (compound.assignment.type.incompatible) :: error: (viewpointtest.lost.lhs)
        obj.i += 1;
        // :: error: (unary.increment.type.incompatible) :: error: (viewpointtest.lost.lhs)
        obj.i++;

        // :: error: (viewpointtest.lost.lhs)
        obj.nested = null;
    }
}

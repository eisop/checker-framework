import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

import java.util.Date;

public @ReceiverDependentMutable class ReimStudy {
    // :: error: (initialization.field.uninitialized)
    @ReceiverDependentMutable Date date;

    @ReceiverDependentMutable Date getDate(@ReceiverDependentMutable ReimStudy this) {
        return this.date;
    }

    @SuppressWarnings({"deprecation"})
    void cellSetHours(@Mutable ReimStudy this) {
        // ReIm argues that viewpoint adapting to lhs(@Mutable here) trasmits the context to current
        // "this" via below type rules:
        // q(this-cellSetHours) <: q(md) |> q(this-getDate) Which is q(this-cellSetHours) <:
        // @Mutable |> @PolyImmutable = @Mutable
        // And it gives an counterexample that if we adapt to the receiver of the method invocation,
        // we get a not-useful constraint:
        // q(this-cellSetHours) <: q(this-cellSetHours) |> q(this-getDate) Which is
        // q(this-cellSetHours) <: q(this-cellSetHours)

        // But in fact, we can still transmit that mutability context into current "this" even
        // without adapting to lhs.
        // q(this-cellSetHours) |> q(ret-getDate) <: q(md) which becomes q(this-cellSetHours) <:
        // @Mutable. It still can make current "this"
        // to be mutable.
        // Truly, viewpoint adaptation to receiver doesn't impose additional constraint on receiver.
        // But this makes sense. Because poly
        // means that it can be substited by any qualifiers including poly itself. That's exactly
        // the purpose of method with poly "this" -
        // invocable on all possible types. ReIm also suffers this "not-useful" contraint problem on
        // return type adaptation:
        // q(md) |> q(ret-getDate) <: q(md) which becomes q(md) <: q(md). So there is no reason for
        // ReIm to argue against this "seems-like"
        // trivial constraint
        @Mutable Date md = this.getDate();
        md.setHours(1);
    }

    @SuppressWarnings({"deprecation"})
    void cellGetHours(@Readonly ReimStudy this) {
        // In receiver viewpoint adaptation:
        // q(this-cellGetHours) |> @PolyImmutable <: @Readonly => q(this-cellGetHours) <: @Readonly
        // So cellGetHours is invocable on any types of receiver.
        // In inference, if we prefer top(@Readonly), it still infers current "this" to @Readonly.
        // :: error: (method.invocation.invalid)
        @Readonly Date rd = this.getDate();
        int hour = rd.getHours();
    }

    @ReceiverDependentMutable class DateCell2 {
        // :: error: (initialization.field.uninitialized)
        @Immutable Date imdate;

        @Immutable Date getImmutableDate(@PolyMutable DateCell2 this) {
            return this.imdate;
        }

        /*Not allowed in ReIm. But allowed in PICO*/
        void test1(@Mutable DateCell2 this) {
            @Immutable Date imd = this.getImmutableDate();
        }

        void test2(@Immutable DateCell2 this) {
            @Immutable DateCell2 waht = new @Immutable DateCell2();
            @Immutable Date imd = this.getImmutableDate();
        }
    }
}

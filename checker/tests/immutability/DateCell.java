import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

import java.util.Date;

@ReceiverDependantMutable
public class DateCell {

    @ReceiverDependantMutable Date date;

    @ReceiverDependantMutable
    Date getDate(@ReceiverDependantMutable DateCell this) {
        return this.date;
    }

    @SuppressWarnings({"deprecation"})
    void cellSetHours(@Mutable DateCell this) {
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
    void cellGetHours(@Readonly DateCell this) {
        // In receiver viewpoint adaptation:
        // q(this-cellGetHours) |> @PolyImmutable <: @Readonly => q(this-cellGetHours) <: @Readonly
        // So cellGetHours is invocable on
        // any types of receiver. In inference, if we prefer top(@Readonly), it still infers current
        // "this" to @Readonly.
        @Readonly Date rd = this.getDate();
        int hour = rd.getHours();
    }

    // For a method declaration, if type of this, formal parameters and return type are annotated
    // with @Readonly, @Immutable, @Mutable,
    // adaptating to whatever doesn't make a difference. Only when they are @PolyImmutable, there is
    // real difference between the two.
    // The most tricky case - receiver, return type are @PolyImmutable is proven to be valid in
    // receiver adaptation methodology.
    // ReIm doesn't support instatiating poly and readonly objects, so if return type is poly,
    // method receiver is also poly. ReIm's
    // reasoning using the cellSetHourse, cellGetHours, getDate methods failed to show the necessity
    // of viewpoint adapting to lhs.
}

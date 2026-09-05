# A type system for lifetimes the JDK checks at run time

## The problem

This is the speculative one of the five. It is here because it is the only
place where a genuinely new type system has a specification already written for
it by somebody else.

Java's Foreign Function & Memory API (`java.lang.foreign`, final since JDK 22)
gives a `MemorySegment` two boundaries the JDK enforces: a **spatial** one, and
a **temporal** one tied to the `Arena` that allocated it. Access a segment after
its arena is closed and you get an `IllegalStateException`. Access a segment
from a thread other than the one that opened its confined arena, and likewise.
The API's own design accepts this: violations are "caught at runtime via
exceptions rather than relying on static analysis."

That is a use-after-free and a data race, detected one execution at a time.
It is exactly the class of property a pluggable type system exists to move to
compile time, and the hard part -- deciding what the rule *is* -- is already
done, in a JEP, precisely.

The Checker Framework has nothing for it. A search of this repository for
`java.lang.foreign`, `MemorySegment`, `StructuredTaskScope`, or `ScopedValue`
finds no annotation, no stub, and no mention outside a vendored JDK copy.

## Why this domain and not another

Because the machinery is already most of the way there, and because the
adjacent checker is the one users want most.

The Resource Leak Checker carries 26 open issues on the typetools tracker --
the largest of any checker-specific label, more than twice the next. People
are using it and hitting its edges. Its foundation is `@MustCall` and
`@CalledMethods`: an obligation attached to a value, discharged by calling a
method. "This segment must not be used after `arena.close()`" is the same shape
with the polarity flipped -- an obligation that *expires* rather than one that
must be discharged.

The open Resource Leak issues are also concentrated in exactly the places this
would have to work: ownership transfer into collections and varargs (#6029),
functional interfaces (#6823), resources allocated and closed across method
boundaries (#6270). Those are aliasing questions. A lifetime system has to
answer them too, and answering them once might serve both.

Structured concurrency (`StructuredTaskScope`) and `ScopedValue` have the same
shape again: a value valid only inside a lexical scope, enforced at run time.

## The three questions

1. **Is the FFM confinement rule expressible as a qualifier hierarchy at all?**
   The property is not a fact about a value but a relationship between a value
   and a *region* that is open or closed at a program point. Flow-sensitive
   refinement can express "this arena is open here." Whether a segment's type
   can name the arena it belongs to, in a system without dependent types, is
   the question that decides whether this is a checker or a paper. Answer it on
   paper, on ten real FFM code samples, before writing any code.

2. **Does anyone write enough FFM code for this to matter?** The API is final
   but young, and its users are disproportionately library authors doing native
   interop -- a small, expert population that may already be careful. A type
   system nobody needs is worse than no type system. Count the FFM usage in a
   corpus of real projects before going further; if it is negligible today, the
   same machinery aimed at `StructuredTaskScope` or at the existing Resource
   Leak Checker's aliasing gaps is the better target.

3. **Would solving the aliasing problem help the Resource Leak Checker more
   than a new checker would help anyone?** The honest possibility is that this
   investigation's real value is not a lifetime checker but a better answer to
   ownership transfer, and that the right move is to spend the effort on the 26
   open Resource Leak issues instead. That should be decided deliberately, not
   discovered halfway through.

## First experiment

Write, by hand, the ten smallest FFM programs that a lifetime checker would
have to accept and the ten it would have to reject. If the accept set cannot be
distinguished from the reject set by any assignment of qualifiers to types, the
answer to question 1 is no and this ends cheaply.

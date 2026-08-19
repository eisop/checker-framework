package nontopdefault;

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDBottom;
import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDMiddle;
import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDTop;

// Same as StubBounds.java's widen()/narrow(), but for a class declared in source (merged with
// the stub via -AmergeStubsWithSource) rather than purely stub-declared like java.util.Map/List.

@SuppressWarnings("inconsistent.constructor.type")
@DefaultQualifier(NTDMiddle.class)
class SourceWidenMe<E> {}

@SuppressWarnings("inconsistent.constructor.type")
class SourceNarrowMe<E> {}

@SuppressWarnings("inconsistent.constructor.type")
public class SourceStubBounds {
    void test() {
        SourceWidenMe<@NTDTop Object> okWiden = null;
        SourceWidenMe<@NTDMiddle Object> okMiddle = null;

        SourceNarrowMe<@NTDBottom Object> okNarrow = null;
        // :: error: (type.argument.type.incompatible)
        SourceNarrowMe<@NTDMiddle Object> errorMiddle = null;
    }
}

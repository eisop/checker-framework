/*
 * @test
 * @summary Test the source range that -Adetailedmsgtext reports for a tree.
 *   https://github.com/eisop/checker-framework/issues/1637
 *
 * @compile/fail/ref=DetailedMsgTextPositions.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker DetailedMsgTextPositions.java -Adetailedmsgtext
 */

import org.checkerframework.checker.nullness.qual.Nullable;

public class DetailedMsgTextPositions {
    Object foo(@Nullable Object nble) {
        return nble.toString() + nble;
    }
}

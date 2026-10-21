package org.checkerframework.dataflow.javacparse;

import com.sun.source.tree.ExpressionTree;

import org.checkerframework.javacutil.javacparse.JavacParse;
import org.checkerframework.javacutil.javacparse.JavacParseResult;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that {@link JavacParse#parseExpression(String)}, which reuses javac state between calls,
 * gives the same results as a fresh parse would.
 */
public class JavacParseReuseTest {

    /** Parse errors are reported every time, also after more errors than javac reports at most. */
    @Test
    public void parseErrorsAreAlwaysReported() {
        for (int i = 0; i < 300; i++) {
            try {
                JavacParse.parseExpression("a + " + i + " +");
                Assert.fail("expected an exception for iteration " + i);
            } catch (IllegalArgumentException e) {
                Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Invalid expression"));
            }
        }
    }

    /** A valid expression parses after a failed parse, and its result has no diagnostics. */
    @Test
    public void validAfterInvalid() {
        for (int i = 0; i < 50; i++) {
            try {
                JavacParse.parseExpression("x.(");
                Assert.fail("expected an exception");
            } catch (IllegalArgumentException e) {
                // expected
            }
            JavacParseResult<ExpressionTree> result = JavacParse.parseExpression("a.b" + i + "[0]");
            Assert.assertFalse(result.hasParseError());
            Assert.assertTrue(result.getDiagnostics().isEmpty());
            Assert.assertEquals("a.b" + i + "[0]", result.getTree().toString());
        }
    }

    /** Many parses with different identifiers, more than one reused parser handles. */
    @Test
    public void manyDistinctParses() {
        for (int i = 0; i < 3500; i++) {
            String s = "this.f" + i + ".length";
            Assert.assertEquals(s, JavacParse.parseExpression(s).getTree().toString());
        }
    }

    /** Trees from earlier parses are not changed by later parses. */
    @Test
    public void earlierTreesAreStable() {
        ExpressionTree first = JavacParse.parseExpression("p.q(1, r)").getTree();
        for (int i = 0; i < 100; i++) {
            JavacParse.parseExpression("p.q(" + i + ", s)");
        }
        Assert.assertEquals("p.q(1, r)", first.toString());
    }
}

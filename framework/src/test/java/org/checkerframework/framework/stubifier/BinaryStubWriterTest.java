package org.checkerframework.framework.stubifier;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import org.checkerframework.framework.stub.BinaryStubData;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/** Tests for {@link BinaryStubWriter}. */
public class BinaryStubWriterTest {

    /**
     * Regression test for a constant-pool sentinel bug: {@code ClassRecord#outerNameIndex} uses
     * {@code 0} to mean "top-level class" (see {@code BinaryStubWriter.makeClassRecord}'s {@code
     * outermostFqn.isEmpty() ? 0 : pool.addString(outermostFqn)}), but the constant pool does not
     * reserve index {@code 0} for an empty string -- the first string ever added to the pool gets
     * index {@code 0}. If an outer class happens to be the first class processed in the whole
     * writer's lifetime (as it is here, being the very first and only file processed by a fresh
     * writer), its own fully-qualified name is stored at pool index 0, so any of its nested classes
     * -- whose own {@code outerNameIndex} is that same, correct index 0 -- become indistinguishable
     * from an actual top-level class.
     */
    @Test
    public void nestedClassOfTheFirstProcessedClassIsNotMistakenForTopLevel() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse(
                        "public class OutermostFirst { public class Inner { public int x; } }");
        writer.process(cu);

        File tmp = File.createTempFile("binarystubwritertest", ".bin.gz");
        tmp.deleteOnExit();
        try {
            writer.writeTo(tmp);
            BinaryStubData data;
            try (InputStream in = Files.newInputStream(tmp.toPath())) {
                data = new BinaryStubData(in);
            }

            BinaryStubData.ClassRecord outer = data.classes.get("OutermostFirst");
            BinaryStubData.ClassRecord inner = data.classes.get("OutermostFirst.Inner");
            Assert.assertNotNull("OutermostFirst must be recorded", outer);
            Assert.assertNotNull("OutermostFirst.Inner must be recorded", inner);

            Assert.assertEquals("OutermostFirst is itself top-level", 0, outer.outerNameIndex);
            Assert.assertNotEquals(
                    "OutermostFirst.Inner's outer class is OutermostFirst, not top-level -- if"
                            + " this is 0, it will be indistinguishable from a top-level class",
                    0,
                    inner.outerNameIndex);
            Assert.assertEquals(
                    "OutermostFirst.Inner's outer name must resolve to OutermostFirst",
                    "OutermostFirst",
                    data.stringPool[inner.outerNameIndex]);
        } finally {
            tmp.delete();
        }
    }
}

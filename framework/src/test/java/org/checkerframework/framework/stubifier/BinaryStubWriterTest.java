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

    /**
     * Regression test for a varargs type-path bug: a declaration-position, type-use-applicable
     * annotation on a vararg parameter (e.g. {@code @Nullable String[]... args}) must apply to the
     * innermost array component, matching {@code AnnotationFileParser}'s {@code
     * annotateInnermostComponentType} (reached, on the text side, via {@code annotateAsArray}'s
     * call to it before descending the declared array levels). For a multidimensional vararg like
     * this one -- equivalent to {@code String[][] args} -- that is two levels below the overall
     * parameter type: one {@code ARRAY} step for the vararg's own implicit array level, plus one
     * more for the explicit {@code []} already present in the declared type ({@code String[]}). The
     * previous code hardcoded a single {@code ARRAY} step, which for this case pointed at the
     * middle {@code String[]} level instead of the innermost {@code String}.
     */
    @Test
    public void multidimensionalVarargsAnnotationPathHasOneStepPerArrayLevel() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse(
                        "import org.checkerframework.checker.nullness.qual.Nullable;\n"
                                + "public class VarargsDim {\n"
                                + "  public void m(@Nullable String[]... args) {}\n"
                                + "}\n");
        writer.process(cu);

        File tmp = File.createTempFile("binarystubwritertest", ".bin.gz");
        tmp.deleteOnExit();
        try {
            writer.writeTo(tmp);
            BinaryStubData data;
            try (InputStream in = Files.newInputStream(tmp.toPath())) {
                data = new BinaryStubData(in);
            }

            BinaryStubData.ClassRecord cr = data.classes.get("VarargsDim");
            Assert.assertNotNull("VarargsDim must be recorded", cr);
            BinaryStubData.MethodRecord mr = null;
            for (BinaryStubData.MethodRecord candidate : cr.methods) {
                if (data.stringPool[candidate.sigIndex].startsWith("m(")) {
                    mr = candidate;
                }
            }
            Assert.assertNotNull("method m must be recorded", mr);

            Assert.assertEquals("m has one parameter", 1, mr.paramAnnos.length);
            BinaryStubData.TypeAnno[] argAnnos = mr.paramAnnos[0];
            Assert.assertEquals(
                    "@Nullable is the only type annotation on the vararg parameter",
                    1,
                    argAnnos.length);
            BinaryStubData.TypePathStep[] path = argAnnos[0].path;
            Assert.assertEquals(
                    "path must have one ARRAY step for the vararg's own implicit array level plus"
                            + " one for the declared type's own [] -- not a single step, which"
                            + " would point at the middle String[] level instead of the"
                            + " innermost String",
                    2,
                    path.length);
            Assert.assertEquals("first step is ARRAY", 0, path[0].kind);
            Assert.assertEquals("second step is ARRAY", 0, path[1].kind);
        } finally {
            tmp.delete();
        }
    }
}

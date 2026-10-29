package org.checkerframework.framework.stub;

import org.checkerframework.framework.stubifier.BinaryStubFileGenerator;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Tests that every form of annotation element value that Java permits (JLS 9.7.1) -- constant
 * expressions (JLS 15.29), constants named in all the ways Java scopes them, text blocks, and
 * nested annotations -- has the same value in a stub file as in Java source, whether the stub file
 * is text-parsed ({@link AnnotationFileParser}) or read from its binary form ({@link
 * BinaryStubReader}).
 *
 * <p>Each stub method's return type carries a Value Checker annotation whose value is such an
 * expression, and the checked source assigns the method's result to a variable annotated with the
 * same expression, written in Java, so that javac computes the expected value. A wrong or missing
 * value is an {@code assignment.type.incompatible} error. The binary form is also compared with the
 * text by {@code -AbinaryStubDiffCheck}.
 */
public class StubAnnotationValuesTest {

    /** A fresh directory per test. */
    @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

    /** The library whose methods the stub file annotates. */
    private static final String LIB_SOURCE =
            "public class Lib {\n"
                    + "    public static final int BASE = 40;\n"
                    + "    public static final String NAME = \"n\";\n"
                    + "    public static int paren() { return 0; }\n"
                    + "    public static int unary() { return 0; }\n"
                    + "    public static boolean not() { return false; }\n"
                    + "    public static int arithmetic() { return 0; }\n"
                    + "    public static int intShift() { return 0; }\n"
                    + "    public static long longShift() { return 0; }\n"
                    + "    public static int bitwise() { return 0; }\n"
                    + "    public static boolean logical() { return false; }\n"
                    + "    public static String concat() { return null; }\n"
                    + "    public static int charArithmetic() { return 0; }\n"
                    + "    public static int conditional() { return 0; }\n"
                    + "    public static int casts() { return 0; }\n"
                    + "    public static String stringCast() { return null; }\n"
                    + "    public static int sameClass() { return 0; }\n"
                    + "    public static String sameClassString() { return null; }\n"
                    + "    public static int singleStaticImport() { return 0; }\n"
                    + "    public static int staticImportOnDemand() { return 0; }\n"
                    + "    public static int qualified() { return 0; }\n"
                    + "    public static String textBlock() { return null; }\n"
                    + "    public static double floating() { return 0; }\n"
                    + "    public static long minValues() { return 0; }\n"
                    + "    public static class Inner {\n"
                    + "        public static int enclosing() { return 0; }\n"
                    + "    }\n"
                    + "}\n";

    /**
     * The stub file. {@code MAX_VALUE} is imported both by a single-static-import (from {@code
     * Integer}) and on demand (from {@code Short}): the single-static-import shadows the other, as
     * in Java. {@code MIN_VALUE} is imported only on demand, from {@code Short}.
     */
    private static final String STUB =
            "import static java.lang.Integer.MAX_VALUE;\n"
                    + "import static java.lang.Short.*;\n"
                    + "import org.checkerframework.common.value.qual.*;\n"
                    + "public class Lib {\n"
                    + "    public static final int BASE = 40;\n"
                    + "    public static final String NAME = \"n\";\n"
                    + "    public static @IntVal((2)) int paren();\n"
                    + "    public static @IntVal({-(3), ~5, +7}) int unary();\n"
                    + "    public static @BoolVal(!true) boolean not();\n"
                    + "    public static @IntVal(2 * 3 + 4 % 3 - 10 / 4) int arithmetic();\n"
                    + "    public static @IntVal({1 << 40, -1 >>> 28, -16 >> 2}) int intShift();\n"
                    + "    public static @IntVal(1L << 40) long longShift();\n"
                    + "    public static @IntVal(0xF0 & 0x3C | 1 ^ 3) int bitwise();\n"
                    + "    public static @BoolVal(1 < 2 && 3 >= 3 || 2 == 2.0 & 1 != 1) boolean"
                    + " logical();\n"
                    + "    public static @StringVal(\"a\" + 1 + 'c' + 2.5 + true + (1 + 2)) String"
                    + " concat();\n"
                    + "    public static @IntVal('a' + 1) int charArithmetic();\n"
                    + "    public static @IntVal(1 < 2 ? 10 : 20) int conditional();\n"
                    + "    public static @IntVal({(byte) 300, (int) 3.9, (char) 65, (short) -1}) int"
                    + " casts();\n"
                    + "    public static @StringVal((String) \"s\") String stringCast();\n"
                    + "    public static @IntVal(BASE + 1) int sameClass();\n"
                    + "    public static @StringVal(NAME + NAME) String sameClassString();\n"
                    + "    public static @IntVal(MAX_VALUE) int singleStaticImport();\n"
                    + "    public static @IntVal(MIN_VALUE) int staticImportOnDemand();\n"
                    + "    public static @IntVal(Byte.MAX_VALUE * 2) int qualified();\n"
                    + "    public static @StringVal(\"\"\"\n"
                    + "            text\n"
                    + "              block\\tend\n"
                    + "            \"\"\") String textBlock();\n"
                    + "    public static @DoubleVal({1.1f, 1 / 2.0, 3}) double floating();\n"
                    + "    public static @IntVal({-2147483648, -9223372036854775808L, -9_223_372_036_854_775_808l}) long"
                    + " minValues();\n"
                    + "    public static class Inner {\n"
                    + "        public static @IntVal(BASE) int enclosing();\n"
                    + "    }\n"
                    + "}\n";

    /** The checked source, which states each expected value as the same expression in Java. */
    private static final String USE_SOURCE =
            "import org.checkerframework.common.value.qual.*;\n"
                    + "class Use {\n"
                    + "    void m() {\n"
                    + "        @IntVal((2)) int paren = Lib.paren();\n"
                    + "        @IntVal({-(3), ~5, +7}) int unary = Lib.unary();\n"
                    + "        @BoolVal(!true) boolean not = Lib.not();\n"
                    + "        @IntVal(2 * 3 + 4 % 3 - 10 / 4) int arithmetic = Lib.arithmetic();\n"
                    + "        @IntVal({1 << 40, -1 >>> 28, -16 >> 2}) int intShift = Lib.intShift();\n"
                    + "        @IntVal(1L << 40) long longShift = Lib.longShift();\n"
                    + "        @IntVal(0xF0 & 0x3C | 1 ^ 3) int bitwise = Lib.bitwise();\n"
                    + "        @BoolVal(1 < 2 && 3 >= 3 || 2 == 2.0 & 1 != 1) boolean logical ="
                    + " Lib.logical();\n"
                    + "        @StringVal(\"a\" + 1 + 'c' + 2.5 + true + (1 + 2)) String concat ="
                    + " Lib.concat();\n"
                    + "        @IntVal('a' + 1) int charArithmetic = Lib.charArithmetic();\n"
                    + "        @IntVal(1 < 2 ? 10 : 20) int conditional = Lib.conditional();\n"
                    + "        @IntVal({(byte) 300, (int) 3.9, (char) 65, (short) -1}) int casts ="
                    + " Lib.casts();\n"
                    + "        @StringVal((String) \"s\") String stringCast = Lib.stringCast();\n"
                    + "        @IntVal(Lib.BASE + 1) int sameClass = Lib.sameClass();\n"
                    + "        @StringVal(Lib.NAME + Lib.NAME) String sameClassString ="
                    + " Lib.sameClassString();\n"
                    + "        @IntVal(Integer.MAX_VALUE) int singleStaticImport ="
                    + " Lib.singleStaticImport();\n"
                    + "        @IntVal(Short.MIN_VALUE) int staticImportOnDemand ="
                    + " Lib.staticImportOnDemand();\n"
                    + "        @IntVal(Byte.MAX_VALUE * 2) int qualified = Lib.qualified();\n"
                    + "        @StringVal(\"text\\n  block\\tend\\n\") String textBlock ="
                    + " Lib.textBlock();\n"
                    + "        @DoubleVal({1.1f, 1 / 2.0, 3}) double floating = Lib.floating();\n"
                    + "        @IntVal({-2147483648, -9223372036854775808L, -9_223_372_036_854_775_808l}) long minValues ="
                    + " Lib.minValues();\n"
                    + "        @IntVal(Lib.BASE) int enclosing = Lib.Inner.enclosing();\n"
                    + "    }\n"
                    + "}\n";

    /**
     * Every value form has the value javac gives it, through the text parser and through the binary
     * form, and the two forms agree.
     */
    @Test
    public void constantExpressionValuesMatchJava() throws IOException {
        Path dir = tempFolder.getRoot().toPath();
        Path stubsDir = dir.resolve("stubs");
        BinaryStubsCommandLineTest.write(stubsDir.resolve("Lib.astub"), STUB);
        BinaryStubFileGenerator.main(new String[] {stubsDir.toString(), stubsDir.toString()});
        Assert.assertTrue(
                "generator must have written a sibling binary",
                Files.isRegularFile(stubsDir.resolve("Lib.astub.bin.gz")));

        // -AstubWarnIfNotFound forces text parsing; see useBinaryForCommandLineStubs.
        assertNoErrors(
                "text",
                compile(dir.resolve("text"), "-Astubs=" + stubsDir, "-AstubWarnIfNotFound"));
        assertNoErrors(
                "binary",
                compile(dir.resolve("binary"), "-Astubs=" + stubsDir, "-AbinaryStubDiffCheck"));
    }

    /**
     * Compiles {@link #LIB_SOURCE} with javac and {@link #USE_SOURCE} with the Value Checker.
     *
     * @param dir the directory to compile in
     * @param options options for the Value Checker
     * @return the diagnostics
     * @throws IOException if writing a file fails
     */
    private static List<Diagnostic<? extends JavaFileObject>> compile(Path dir, String... options)
            throws IOException {
        File libClasses = BinaryStubsCommandLineTest.compileLib(dir, LIB_SOURCE);
        File useFile = dir.resolve("Use.java").toFile();
        BinaryStubsCommandLineTest.write(useFile.toPath(), USE_SOURCE);
        TestConfiguration config =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        dir.toString(),
                        Collections.singletonList(useFile),
                        Collections.singletonList(libClasses.getAbsolutePath()),
                        Collections.singletonList("org.checkerframework.common.value.ValueChecker"),
                        new ArrayList<>(Arrays.asList(options)),
                        false);
        return new TypecheckExecutor().runTest(config).getActualDiagnostics();
    }

    /**
     * Asserts that {@code diagnostics} contains no error.
     *
     * @param path which path the diagnostics come from, for the failure message
     * @param diagnostics the diagnostics
     */
    private static void assertNoErrors(
            String path, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        List<String> errors = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                errors.add(d.getLineNumber() + ": " + d.getMessage(null));
            }
        }
        Assert.assertTrue(path + ": " + errors, errors.isEmpty());
    }
}

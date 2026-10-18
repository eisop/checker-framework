package org.checkerframework.framework.stub;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.stubifier.BinaryStubFileGenerator;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Checks, shape by shape, that a {@code -Astubs} file gives the same types in its text form and in
 * its binary form. Each {@link Row} is a library class, a stub that annotates one of its members
 * differently from the class file, and a use whose diagnostics show which annotations took effect.
 * For each row, the use is compiled three times: without the stub, with the stub text-parsed
 * ({@code -AstubWarnIfNotFound} disables the binary form), and with the stub's binary form. The
 * text and binary errors must equal the row's expected errors, and, so that a row cannot pass
 * because the stub was ignored, the errors without the stub must differ unless the row says the
 * stub keeps the class file's annotation.
 *
 * <p>Uses the Subtyping Checker with {@code @Unqualified} (top, the default) and {@code @Encrypted}
 * (bottom, the default for lower bounds).
 */
public class BinaryStubTypeShapesTest {

    /** A fresh directory per test. */
    @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

    /** Imports for every library, stub, and use source. */
    private static final String IMPORTS =
            "import org.checkerframework.framework.testchecker.util.Encrypted;\n"
                    + "import org.checkerframework.common.subtyping.qual.Unqualified;\n";

    /** A generic interface used by most rows, in the library and (if declared there) the stub. */
    private static final String BOX = "public interface Box<T> { T get(); void set(T t); }\n";

    /** Message key: an assignment's right-hand side is not a subtype of the left-hand side. */
    private static final String ASSIGN = "assignment.type.incompatible";

    /** Message key: an argument is not a subtype of its parameter. */
    private static final String ARG = "argument.type.incompatible";

    /** Message key: a type argument is not within the type parameter's bounds. */
    private static final String TYPE_ARG = "type.argument.type.incompatible";

    /** One shape: a library member, a stub for it, a use, and the expected error. */
    private static class Row {
        /** The row's name, for failure messages. */
        final String name;

        /** Members of the library class {@code Lib}, with bodies. */
        final String libMembers;

        /** Members of the stub for {@code Lib}, without bodies. */
        final String stubMembers;

        /** A method of class {@code Use}, which uses {@code Lib}. */
        final String useMethod;

        /** The message key of the one error expected with the stub, or null for none. */
        final @Nullable String expected;

        /** If true, the stub keeps the class file's annotation, so the errors without it agree. */
        final boolean keepsClassFile;

        /**
         * Creates a row.
         *
         * @param name the row's name
         * @param libMembers members of {@code Lib}, with bodies
         * @param stubMembers members of the stub for {@code Lib}, without bodies
         * @param useMethod a method of class {@code Use}
         * @param expected the message key of the one error expected with the stub, or null
         * @param keepsClassFile true if the errors without the stub agree with {@code expected}
         */
        Row(
                String name,
                String libMembers,
                String stubMembers,
                String useMethod,
                @Nullable String expected,
                boolean keepsClassFile) {
            this.name = name;
            this.libMembers = libMembers;
            this.stubMembers = stubMembers;
            this.useMethod = useMethod;
            this.expected = expected;
            this.keepsClassFile = keepsClassFile;
        }
    }

    /**
     * Creates a row whose stub changes the outcome.
     *
     * @param name the row's name
     * @param libMembers members of {@code Lib}, with bodies
     * @param stubMembers members of the stub for {@code Lib}, without bodies
     * @param useMethod a method of class {@code Use}
     * @param expected the message key of the one error expected with the stub, or null
     * @return the row
     */
    private static Row row(
            String name,
            String libMembers,
            String stubMembers,
            String useMethod,
            @Nullable String expected) {
        return new Row(name, libMembers, stubMembers, useMethod, expected, false);
    }

    /** Reads the extends bound of {@code Lib.make()}'s wildcard. */
    private static final String GET = "void m() { @Encrypted Object o = Lib.make().get(); }";

    /** Passes an unqualified value where {@code Lib.make()}'s wildcard's lower bound is needed. */
    private static final String SET = "void m(@Unqualified Object u) { Lib.make().set(u); }";

    /** Passes a box of encrypted values to {@code Lib.takeBox}. */
    private static final String PASS_ENCRYPTED_BOX =
            "void m(Lib.Box<@Encrypted Object> b) { Lib.takeBox(b); }";

    /** The rows. */
    private static final List<Row> ROWS =
            Arrays.asList(
                    // Wildcards. The class file's wildcard differs from the stub's.
                    row(
                            "plain ? replaces an explicit class-file bound",
                            BOX
                                    + "public static Box<? extends @Encrypted Object> make() {return null;}",
                            "public static Box<?> make();",
                            GET,
                            ASSIGN),
                    row(
                            "plain ? takes the type parameter bound from the stub",
                            "public interface EBox<T> { T get(); }\n"
                                    + "public static EBox<?> make() { return null; }",
                            "public interface EBox<T extends @Encrypted Object> { T get(); }\n"
                                    + "public static EBox<?> make();",
                            GET,
                            null),
                    new Row(
                            "plain ? takes the type parameter bound from the class file",
                            "public interface EBox<T extends @Encrypted Object> { T get(); }\n"
                                    + "public static EBox<? extends @Unqualified Object> make()"
                                    + " { return null; }",
                            "public static EBox<?> make();",
                            GET,
                            null,
                            true),
                    row(
                            "plain ? takes a stub's unannotated type parameter bound",
                            "public interface EBox<T extends @Encrypted Object> { T get(); }\n"
                                    + "public static EBox<? extends @Encrypted Object> make()"
                                    + " { return null; }",
                            "public interface EBox<T extends Object> { T get(); }\n"
                                    + "public static EBox<?> make();",
                            GET,
                            ASSIGN),
                    row(
                            "@A ? annotates the extends bound",
                            BOX + "public static Box<?> make() { return null; }",
                            "public static Box<@Encrypted ?> make();",
                            GET,
                            null),
                    row(
                            "@A ? annotates the super bound",
                            BOX + "public static void takeBox(Box<?> b) {}",
                            "public static void takeBox(Box<@Unqualified ?> b);",
                            PASS_ENCRYPTED_BOX,
                            ARG),
                    row(
                            "? extends X clears a class-file bound annotation",
                            BOX
                                    + "public static Box<? extends @Encrypted Object> make() {return null;}",
                            "public static Box<? extends Object> make();",
                            GET,
                            ASSIGN),
                    row(
                            "? extends @A X",
                            BOX + "public static Box<? extends Object> make() { return null; }",
                            "public static Box<? extends @Encrypted Object> make();",
                            GET,
                            null),
                    row(
                            "@A ? extends X annotates the super bound",
                            BOX + "public static void takeBox(Box<? extends Object> b) {}",
                            "public static void takeBox(Box<@Unqualified ? extends Object> b);",
                            PASS_ENCRYPTED_BOX,
                            ARG),
                    row(
                            "@A ? extends X does not annotate the extends bound",
                            BOX
                                    + "public static Box<? extends @Encrypted Object> make() {return null;}",
                            "public static Box<@Encrypted ? extends Object> make();",
                            GET,
                            ASSIGN),
                    // The class file's annotation on a super bound has no effect, so these rows
                    // compare text and binary only.
                    new Row(
                            "? super X",
                            BOX
                                    + "public static Box<? super @Unqualified Object> make() {return null;}",
                            "public static Box<? super Object> make();",
                            SET,
                            ARG,
                            true),
                    row(
                            "? super @A X",
                            BOX + "public static Box<? super Object> make() { return null; }",
                            "public static Box<? super @Unqualified Object> make();",
                            SET,
                            null),
                    new Row(
                            "@A ? super X does not annotate the super bound",
                            BOX + "public static Box<? super Object> make() { return null; }",
                            "public static Box<@Unqualified ? super Object> make();",
                            SET,
                            ARG,
                            true),
                    row(
                            "@A ? super X annotates the extends bound",
                            BOX + "public static void takeBox(Box<? super Object> b) {}",
                            "public static void takeBox(Box<@Encrypted ? super Object> b);",
                            "void m(Lib.Box<@Unqualified Object> b) { Lib.takeBox(b); }",
                            ARG),
                    // Type variable uses and declarations.
                    row(
                            "unannotated use T clears a class-file annotation",
                            "public static <U> @Encrypted U pick() { return null; }",
                            "public static <U> U pick();",
                            "void m() { @Encrypted Object o = Lib.<Object>pick(); }",
                            ASSIGN),
                    row(
                            "annotated use @A T",
                            "public static <U> U pick() { return null; }",
                            "public static <U> @Encrypted U pick();",
                            "void m() { @Encrypted Object o = Lib.<Object>pick(); }",
                            null),
                    row(
                            "annotated use @A T as a parameter",
                            "public static <U> void take(U u) {}",
                            "public static <U> void take(@Encrypted U u);",
                            "void m(@Unqualified Object u) { Lib.<Object>take(u); }",
                            ARG),
                    row(
                            "annotated use @A T as a type argument",
                            BOX + "public static <U> Box<U> boxOf() { return null; }",
                            "public static <U> Box<@Encrypted U> boxOf();",
                            "void m() { @Encrypted Object o = Lib.<Object>boxOf().get(); }",
                            null),
                    row(
                            "unannotated use T as a type argument",
                            BOX + "public static <U> Box<@Encrypted U> boxOf() { return null; }",
                            "public static <U> Box<U> boxOf();",
                            "void m() { @Encrypted Object o = Lib.<Object>boxOf().get(); }",
                            ASSIGN),
                    row(
                            "? extends @A T",
                            BOX + "public static <U> Box<? extends U> wb() { return null; }",
                            "public static <U> Box<? extends @Encrypted U> wb();",
                            "void m() { @Encrypted Object o = Lib.<Object>wb().get(); }",
                            null),
                    row(
                            "? extends T",
                            BOX
                                    + "public static <U> Box<? extends @Encrypted U> wb() {return null;}",
                            "public static <U> Box<? extends U> wb();",
                            "void m() { @Encrypted Object o = Lib.<Object>wb().get(); }",
                            ASSIGN),
                    row(
                            "annotated use of a class type variable",
                            "public interface CBox<T> { T get(); }",
                            "public interface CBox<T> { @Encrypted T get(); }",
                            "void m(Lib.CBox<Object> b) { @Encrypted Object o = b.get(); }",
                            null),
                    row(
                            "method type parameter bound annotated in the stub",
                            "public static <U extends Object> U pick() { return null; }",
                            "public static <U extends @Encrypted Object> U pick();",
                            "void m() { Lib.<Object>pick(); }",
                            TYPE_ARG),
                    row(
                            "method type parameter bound annotated only in the class file",
                            "public static <U extends @Encrypted Object> U pick() { return null; }",
                            "public static <U extends Object> U pick();",
                            "void m() { Lib.<Object>pick(); }",
                            null),
                    new Row(
                            "stub method type parameter without a bound keeps the class file's",
                            "public static <U extends @Encrypted Object> U pick() { return null; }",
                            "public static <U> @Encrypted U pick();",
                            "void m() { Lib.<Object>pick(); }",
                            TYPE_ARG,
                            true),
                    row(
                            "class type parameter bound annotated in the stub",
                            "public interface BBox<T extends Object> {}",
                            "public interface BBox<T extends @Encrypted Object> {}",
                            "void m(Lib.BBox<Object> b) {}",
                            TYPE_ARG),
                    row(
                            "class type parameter bound annotated only in the class file",
                            "public interface BBox<T extends @Encrypted Object> {}",
                            "public interface BBox<T extends Object> {}",
                            "void m(Lib.BBox<Object> b) {}",
                            null),
                    // Positions.
                    row(
                            "nested plain ?",
                            BOX
                                    + "public static Box<? extends Box<? extends @Encrypted Object>>"
                                    + " nested() { return null; }",
                            "public static Box<? extends Box<?>> nested();",
                            "void m() { @Encrypted Object o = Lib.nested().get().get(); }",
                            ASSIGN),
                    row(
                            "plain ? in a type parameter bound",
                            BOX
                                    + "public static <U extends Box<? extends @Encrypted Object>> U"
                                    + " tp() { return null; }",
                            "public static <U extends Box<?>> U tp();",
                            "void m() { Lib.<Lib.Box<Object>>tp(); }",
                            null),
                    row(
                            "plain ? in a vararg",
                            BOX + "public static void va(Box<? extends @Encrypted Object>... b) {}",
                            "public static void va(Box<?>... b);",
                            "void m(Lib.Box<Object> b) { Lib.va(b); }",
                            null),
                    row(
                            "plain ? in an array",
                            BOX + "public static void arr(Box<? extends @Encrypted Object>[] b) {}",
                            "public static void arr(Box<?>[] b);",
                            "void m(Lib.Box<Object>[] b) { Lib.arr(b); }",
                            null),
                    row(
                            "type argument annotation in an array",
                            BOX + "public static void arr(Box<Object>[] b) {}",
                            "public static void arr(Box<@Encrypted Object>[] b);",
                            "void m(Lib.Box<Object>[] b) { Lib.arr(b); }",
                            ARG),
                    row(
                            "plain ? in a field",
                            BOX + "public static Box<? extends @Encrypted Object> f;",
                            "public static Box<?> f;",
                            "void m() { @Encrypted Object o = Lib.f.get(); }",
                            ASSIGN),
                    row(
                            "receiver annotation",
                            "public static class R { public void r(@Encrypted R this) {} }",
                            "public static class R { public void r(R this); }",
                            "void m(Lib.R r) { r.r(); }",
                            null));

    /**
     * Compiles each row's use without the stub, with its text form, and with its binary form, and
     * checks the errors.
     *
     * @throws IOException if writing or compiling a fixture fails
     */
    @Test
    public void textAndBinaryAgree() throws IOException {
        List<String> failures = new ArrayList<>();
        StringBuilder table = new StringBuilder();
        int i = 0;
        for (Row row : ROWS) {
            Path dir = tempFolder.newFolder("row" + i++).toPath();
            String lib = IMPORTS + "public class Lib {\n" + row.libMembers + "\n}\n";
            String stub = IMPORTS + "public class Lib {\n" + row.stubMembers + "\n}\n";
            String use = IMPORTS + "class Use {\n" + row.useMethod + "\n}\n";
            Path stubsDir = dir.resolve("stubs");
            BinaryStubsCommandLineTest.write(stubsDir.resolve("Lib.astub"), stub);
            BinaryStubFileGenerator.main(new String[] {stubsDir.toString(), stubsDir.toString()});

            List<String> none = errors(BinaryStubsCommandLineTest.compileSources(dir, lib, use));
            List<String> text =
                    errors(
                            BinaryStubsCommandLineTest.compileSources(
                                    dir, lib, use, "-Astubs=" + stubsDir, "-AstubWarnIfNotFound"));
            List<String> binary =
                    errors(
                            BinaryStubsCommandLineTest.compileSources(
                                    dir, lib, use, "-Astubs=" + stubsDir));
            List<String> expected =
                    row.expected == null
                            ? new ArrayList<>()
                            : Arrays.asList("(" + row.expected + ")");
            table.append(
                    String.format(
                            "%s | none=%s | text=%s | binary=%s%n", row.name, none, text, binary));
            if (!text.equals(expected)) {
                failures.add(row.name + ": text " + text + ", expected " + expected);
            }
            if (!binary.equals(expected)) {
                failures.add(row.name + ": binary " + binary + ", expected " + expected);
            }
            if (row.keepsClassFile != none.equals(expected)) {
                failures.add(
                        row.name
                                + ": without the stub "
                                + none
                                + (row.keepsClassFile ? ", expected " : ", must differ from ")
                                + expected);
            }
        }
        System.out.print(table);
        Assert.assertTrue(String.join("\n", failures), failures.isEmpty());
    }

    /**
     * Returns the messages of the errors and warnings in {@code diagnostics}.
     *
     * @param diagnostics the diagnostics of a compilation
     * @return the messages of the errors and warnings
     */
    private static List<String> errors(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        List<String> result = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
            if (d.getKind() == Diagnostic.Kind.ERROR
                    || d.getKind() == Diagnostic.Kind.WARNING
                    || d.getKind() == Diagnostic.Kind.MANDATORY_WARNING) {
                result.add(d.getMessage(null));
            }
        }
        return result;
    }
}

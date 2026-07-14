package org.checkerframework.framework.stubifier;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import org.checkerframework.framework.stub.BinaryStubData;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/** Tests for {@link BinaryStubWriter}. */
public class BinaryStubWriterTest {

    /**
     * Writes {@code source} with the given writer mode and reads the result back.
     *
     * @param source the source text of one compilation unit
     * @param omitUnannotatedMembers whether the writer omits member records carrying no annotations
     * @return the binary stub data the writer produced
     * @throws IOException if the temporary file cannot be written or read
     */
    private static BinaryStubData roundTrip(String source, boolean omitUnannotatedMembers)
            throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter(omitUnannotatedMembers);
        writer.process(StaticJavaParser.parse(source));
        File tmp = File.createTempFile("binarystubwritertest", ".bin.gz");
        tmp.deleteOnExit();
        try {
            writer.writeTo(tmp);
            try (InputStream in = Files.newInputStream(tmp.toPath())) {
                return new BinaryStubData(in);
            }
        } finally {
            tmp.delete();
        }
    }

    /** Source declaring one annotated and one unannotated method, field, and enum constant. */
    private static final String MIXED_MEMBERS =
            "import org.checkerframework.checker.nullness.qual.Nullable;\n"
                    + "public class Mixed {\n"
                    + "  public Object plain;\n"
                    + "  public @Nullable Object annotated;\n"
                    + "  public void plainMethod(Object p) {}\n"
                    + "  public @Nullable Object annotatedMethod(Object p) { return null; }\n"
                    + "}\n";

    /**
     * The annotated-JDK writer omits field records carrying no annotations and demotes unannotated
     * method records to bare presence-only signature entries.
     *
     * <p>69.6% of the annotated JDK's method records and 85.6% of its field records are all-empty,
     * costing about a quarter of the compressed file and 22,398 {@code MethodRecord} objects per
     * load when written in full. An unannotated method's signature must still be recorded, though:
     * on a JDK where the class does not really declare the method, the declaration is a fake
     * override whose presence alone resets the member's type at that subtype (see {@code
     * BinaryStubReader#applyFakeOverride}).
     */
    @Test
    public void jdkWriterDemotesUnannotatedMethodRecords() throws IOException {
        BinaryStubData data = roundTrip(MIXED_MEMBERS, /* omitUnannotatedMembers= */ true);
        BinaryStubData.ClassRecord cr = data.classes.get("Mixed");
        Assert.assertNotNull("the class record is written even so", cr);
        Assert.assertEquals("only the annotated field", 1, cr.fields.length);
        Assert.assertEquals("annotated", data.stringPool[cr.fields[0].nameIndex]);
        Assert.assertEquals("only the annotated method in full", 1, cr.methods.length);
        Assert.assertEquals("annotatedMethod(Object)", data.stringPool[cr.methods[0].sigIndex]);
        Assert.assertEquals(
                "the unannotated method as a presence-only signature",
                1,
                cr.presenceOnlyMethodSigs.length);
        Assert.assertEquals("plainMethod(Object)", data.stringPool[cr.presenceOnlyMethodSigs[0]]);
    }

    /**
     * A built-in stub file's writer keeps every member record in full, annotated or not: {@code
     * BinaryStubReader} marks each matched member with {@code @FromStubFile}, and a member with no
     * record is never matched.
     */
    @Test
    public void builtinStubWriterKeepsUnannotatedMemberRecords() throws IOException {
        BinaryStubData data = roundTrip(MIXED_MEMBERS, /* omitUnannotatedMembers= */ false);
        BinaryStubData.ClassRecord cr = data.classes.get("Mixed");
        Assert.assertNotNull(cr);
        Assert.assertEquals("both fields", 2, cr.fields.length);
        Assert.assertEquals("both methods", 2, cr.methods.length);
        Assert.assertEquals(
                "no presence-only entries for a built-in stub",
                0,
                cr.presenceOnlyMethodSigs.length);
    }

    /**
     * An unannotated constructor is dropped entirely by the annotated-JDK writer, with no
     * presence-only entry either: a constructor can never be a fake override (both the binary
     * reader and the text parser skip the fake-override path for constructors), so an all-empty
     * constructor record is a true no-op.
     */
    @Test
    public void jdkWriterDropsUnannotatedConstructorRecords() throws IOException {
        String source = "public class Ctor {\n" + "  public Ctor(Object p) {}\n" + "}\n";
        BinaryStubData data = roundTrip(source, /* omitUnannotatedMembers= */ true);
        BinaryStubData.ClassRecord cr = data.classes.get("Ctor");
        Assert.assertNotNull(cr);
        Assert.assertEquals(0, cr.methods.length);
        Assert.assertEquals(0, cr.presenceOnlyMethodSigs.length);
    }

    /**
     * A class with no annotated members at all still gets a class record from the annotated-JDK
     * writer: its presence in {@code BinaryStubData.classes} is what stops {@code
     * AnnotationFileElementTypes} from falling back to text-parsing the class.
     */
    @Test
    public void jdkWriterKeepsTheClassRecordOfAnEntirelyUnannotatedClass() throws IOException {
        BinaryStubData data =
                roundTrip(
                        "public class Plain { public Object f; public void m() {} }",
                        /* omitUnannotatedMembers= */ true);
        BinaryStubData.ClassRecord cr = data.classes.get("Plain");
        Assert.assertNotNull("the class record must survive", cr);
        Assert.assertEquals(0, cr.fields.length);
        Assert.assertEquals(0, cr.methods.length);
        Assert.assertEquals(
                "the unannotated method survives as a presence-only signature",
                1,
                cr.presenceOnlyMethodSigs.length);
        Assert.assertEquals("m()", data.stringPool[cr.presenceOnlyMethodSigs[0]]);
    }

    /**
     * A count that does not fit its length prefix must fail the file, not truncate.
     *
     * <p>Every length prefix in the format is an unsigned 16-bit value, and {@code
     * DataOutputStream.writeShort} silently keeps only the low 16 bits. A count of 65536 would be
     * written as 0, and {@code BinaryStubData} -- which cannot tell a truncated count from a real
     * one -- would then read every subsequent field of the file from the wrong offset. Nothing in a
     * real stub file comes close to the limit; the guard is what makes the day one does a build
     * failure rather than a silently wrong annotated JDK.
     */
    @Test
    public void aCountTooLargeForItsLengthPrefixFailsRatherThanTruncating() throws IOException {
        DataOutputStream out = new DataOutputStream(new ByteArrayOutputStream());
        BinaryStubWriter.writeCount(out, 0xFFFF, "fields");
        IOException e =
                Assert.assertThrows(
                        IOException.class,
                        () -> BinaryStubWriter.writeCount(out, 0x10000, "fields"));
        Assert.assertTrue(
                "the failure must report the oversized count, but was: " + e.getMessage(),
                e.getMessage().contains("65536") && e.getMessage().contains("65535"));
    }

    /**
     * The 8-bit counterpart of {@link #aCountTooLargeForItsLengthPrefixFailsRatherThanTruncating},
     * reached through a real call site: a type path has one ARRAY step per array dimension, and its
     * length is a {@code u1} (JVMS 4.7.20.1). A 256-dimensional array would write a length of 0,
     * and the reader would then take the next annotation's bytes for the enclosing record's fields.
     *
     * <p>Java caps array types at 255 dimensions, so a stub declaring 256 is already invalid; the
     * point is that the writer says so rather than emitting a corrupt file.
     */
    @Test
    public void aTypePathTooLongForItsLengthPrefixFailsRatherThanTruncating() throws IOException {
        StringBuilder type = new StringBuilder("Object");
        for (int i = 0; i < 256; i++) {
            type.append("[]");
        }
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse(
                        "import org.checkerframework.checker.nullness.qual.Nullable;\n"
                                + "public class Uses { @Nullable "
                                + type
                                + " f; }");
        writer.process(cu);

        File tmp = File.createTempFile("binarystubwritertest", ".bin.gz");
        tmp.deleteOnExit();
        try {
            IOException e = Assert.assertThrows(IOException.class, () -> writer.writeTo(tmp));
            Assert.assertTrue(
                    "the failure must report the oversized path length, but was: " + e.getMessage(),
                    e.getMessage().contains("256") && e.getMessage().contains("255"));
        } finally {
            tmp.delete();
        }
    }

    /**
     * An annotation on a type argument of an <em>enclosing</em> type is dropped, not encoded.
     *
     * <p>{@code extractTypeAnnotations} descends into a {@code ClassOrInterfaceType}'s own type
     * arguments but not into its scope's, so the {@code @Deprecated} on {@code Outer}'s type
     * argument below produces no {@code TypeAnno}, while the one on {@code Inner}'s does. Encoding
     * it would need a JVMS nested-type (kind 1) path step, which neither side emits.
     *
     * <p>This is not a divergence from the text parser: {@code AnnotationFileParser.annotate}'s
     * {@code DECLARED} case reads only {@code getTypeArguments()}, never {@code getScope()}, so it
     * drops the same annotation. The test pins the gap so a future change to either side has to
     * confront it.
     */
    @Test
    public void annotationOnAnEnclosingTypesTypeArgumentIsDropped() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse(
                        "public class Uses {"
                                + " Outer<@Deprecated String>.Inner<@Deprecated Integer> f; }");
        writer.process(cu);

        File tmp = File.createTempFile("binarystubwritertest", ".bin.gz");
        tmp.deleteOnExit();
        try {
            writer.writeTo(tmp);
            BinaryStubData data;
            try (InputStream in = Files.newInputStream(tmp.toPath())) {
                data = new BinaryStubData(in);
            }
            BinaryStubData.FieldRecord fr = data.classes.get("Uses").fields[0];
            Assert.assertEquals(
                    "only Inner's own type argument is annotated; Outer's is dropped",
                    1,
                    fr.typeAnnos.length);
            BinaryStubData.TypePathStep[] path = fr.typeAnnos[0].path;
            Assert.assertEquals("a single TYPE_ARGUMENT step, no nested-type step", 1, path.length);
            Assert.assertEquals("kind 3 == TYPE_ARGUMENT", 3, path[0].kind);
            Assert.assertEquals(0, path[0].argIndex);
        } finally {
            tmp.delete();
        }
    }

    /**
     * A failed serialization must not consume an annotation-pool index.
     *
     * <p>{@code AnnotationPool.addAnnotation} used to record the new index before serializing the
     * annotation into it. When {@code writeAnnotationInline} then threw -- here on {@code 1 + x},
     * which {@code evaluateStringLiteralConcatenation} cannot fold -- the index was handed out but
     * {@code serializedAnnos} never received a corresponding entry. Every later annotation's index
     * shifted down by one relative to the pool it is written into, so a reader given such a file
     * would apply the wrong annotation, or none, rather than fail. Both production callers happen
     * to abort the whole writer on this exception, which is the only reason the desync has never
     * been observed.
     *
     * <p>Here the writer is reused across two {@code process} calls, as {@code JavaStubifier} does
     * across the files of one directory, so the surviving state is observable.
     */
    @Test
    public void aFailedAnnotationDoesNotConsumeAPoolIndex() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();

        CompilationUnit bad =
                StaticJavaParser.parse("public class Bad { @SuppressWarnings(1 + x) Object f; }");
        Assert.assertThrows(RuntimeException.class, () -> writer.process(bad));

        CompilationUnit good =
                StaticJavaParser.parse("public class Good { @Deprecated Object g; }");
        writer.process(good);

        File tmp = File.createTempFile("binarystubwritertest", ".bin.gz");
        tmp.deleteOnExit();
        try {
            writer.writeTo(tmp);
            BinaryStubData data;
            try (InputStream in = Files.newInputStream(tmp.toPath())) {
                data = new BinaryStubData(in);
            }
            BinaryStubData.ClassRecord cr = data.classes.get("Good");
            Assert.assertNotNull(cr);
            Assert.assertEquals(1, cr.fields.length);
            Assert.assertEquals(1, cr.fields[0].declAnnos.length);

            int annoIdx = cr.fields[0].declAnnos[0];
            Assert.assertTrue(
                    "@Deprecated's pool index "
                            + annoIdx
                            + " must be within the pool, whose size is "
                            + data.annotationPool.length,
                    annoIdx >= 0 && annoIdx < data.annotationPool.length);
            Assert.assertEquals(
                    "and it must resolve to @Deprecated, not to some other annotation the"
                            + " abandoned index shifted it onto",
                    "java.lang.Deprecated",
                    data.stringPool[data.annotationPool[annoIdx].nameIndex]);
        } finally {
            tmp.delete();
        }
    }

    /**
     * A fully-qualified annotation name whose class is not on the stubifier classpath must abort
     * the file rather than be routed by guesswork.
     *
     * <p>{@code hasTypeUse} and {@code isTypeUseOnly} both used to read "class did not load" as "no
     * {@code @Target}", which routes the annotation to {@code declAnnos} only. If the annotation is
     * really {@code TYPE_USE}-only, {@code BinaryStubReader.filterApplicable} then discards it
     * against the real {@code @Target} at checker runtime and the annotation disappears from the
     * binary form with no diagnostic anywhere. The stubifier's classpath (stubparser plus
     * checker-qual) is narrower than any checker's, so a stub naming a third-party annotation would
     * hit exactly this. Failing makes {@code BinaryStubFileGenerator} skip the file, which then
     * keeps its (correct) text parsing.
     */
    @Test
    public void unloadableQualifiedAnnotationFailsRatherThanBeingMisrouted() {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse(
                        "import com.example.NotOnClasspath;\n"
                                + "public class Uses { @NotOnClasspath Object f; }");
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> writer.process(cu));
        Assert.assertTrue(
                "the failure must name the annotation it could not load, but was: "
                        + e.getMessage(),
                e.getMessage().contains("com.example.NotOnClasspath"));
    }

    /**
     * An annotation whose <em>simple</em> name resolves to nothing, in a compilation unit with no
     * asterisk import, is not a failure: nothing (no {@code java.lang} match, no explicit import,
     * no asterisk import -- there is none) could have supplied the name, so neither {@code
     * BinaryStubReader} nor the text parser can resolve it either ({@code
     * AnnotationFileParser.getAnnotation} looks the as-written name up with {@code
     * Elements.getTypeElement}, which fails the same way on a bare, unqualified name), and the
     * binary form's routing of it cannot matter. The annotated JDK's own
     * {@code @SuppressFBWarnings} and {@code @ConstructorProperties} (used by same-package
     * visibility, with no import) reach this case today. Contrast with {@link
     * #unresolvableSimpleNameWithAsteriskImportFailsRatherThanBeingDropped}, where an asterisk
     * import exists and this IS a failure.
     */
    @Test
    public void unresolvableSimpleAnnotationNameIsNotAFailure() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse("public class Uses { @NeverImported Object f; }");
        writer.process(cu);

        File tmp = File.createTempFile("binarystubwritertest", ".bin.gz");
        tmp.deleteOnExit();
        try {
            writer.writeTo(tmp);
            BinaryStubData data;
            try (InputStream in = Files.newInputStream(tmp.toPath())) {
                data = new BinaryStubData(in);
            }
            Assert.assertNotNull("Uses must still be recorded", data.classes.get("Uses"));
        } finally {
            tmp.delete();
        }
    }

    /**
     * An annotation whose <em>simple</em> name resolves to nothing IS a failure when the
     * compilation unit has an asterisk import: {@link BinaryStubWriter#fullyQualifyAnnotationName}
     * tried the asterisk-imported package via {@code Class.forName} and failed, but that only
     * proves the package is absent from the stubifier's own build classpath -- not that a checker's
     * {@code Elements}, resolving against whatever classpath its invocation supplies, would also
     * fail. Silently treating this the same as the no-import case above would route the annotation
     * as a bare, unqualified name that {@code BinaryStubReader} can never resolve either, dropping
     * an annotation the text parser could have resolved, with no diagnostic on either side.
     */
    @Test
    public void unresolvableSimpleNameWithAsteriskImportFailsRatherThanBeingDropped() {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse(
                        "import com.example.*;\n"
                                + "public class Uses { @NeverImported Object f; }");
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> writer.process(cu));
        Assert.assertTrue(
                "the failure must name the annotation it could not resolve, but was: "
                        + e.getMessage(),
                e.getMessage().contains("NeverImported"));
    }

    /**
     * An annotation that loads but declares no {@code @Target} -- {@code
     * java.lang.SuppressWarnings} really is one, through reflection -- must be treated as a
     * declaration annotation, not as an unloadable class. This is the distinction between the
     * {@code NO_TARGET} and {@code NOT_LOADABLE} sentinels.
     */
    @Test
    public void annotationWithoutTargetIsADeclarationAnnotation() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse("public class Uses { @SuppressWarnings(\"x\") Object f; }");
        writer.process(cu);

        File tmp = File.createTempFile("binarystubwritertest", ".bin.gz");
        tmp.deleteOnExit();
        try {
            writer.writeTo(tmp);
            BinaryStubData data;
            try (InputStream in = Files.newInputStream(tmp.toPath())) {
                data = new BinaryStubData(in);
            }
            BinaryStubData.ClassRecord cr = data.classes.get("Uses");
            Assert.assertNotNull(cr);
            Assert.assertEquals(1, cr.fields.length);
            Assert.assertEquals(
                    "@SuppressWarnings has no runtime-visible @Target, so it is a declaration"
                            + " annotation",
                    1,
                    cr.fields[0].declAnnos.length);
            Assert.assertEquals(
                    "and it must not also be routed to the field's type",
                    0,
                    cr.fields[0].typeAnnos.length);
        } finally {
            tmp.delete();
        }
    }

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

    /**
     * Regression test for a varargs type-path off-by-one: an annotation embedded in the declared
     * type of a vararg parameter (the {@code @Nullable} of {@code List<@Nullable String>... args})
     * must carry a leading {@code ARRAY} step for the vararg's implicit array level. JavaParser's
     * {@code Parameter.getType()} is the element type ({@code List<String>} here), but the reader
     * ({@code BinaryStubReader.applyMethodRecords}) resolves the recorded paths against the full
     * array parameter type. The previous code extracted the paths with no prefix, so the reader's
     * {@code resolvePath} hit a {@code TYPE_ARGUMENT} step on an {@code AnnotatedArrayType},
     * returned null, and the annotation was silently dropped -- while the text parser ({@code
     * AnnotationFileParser.processParameters}) anchors the declared type at the array's component
     * type and keeps it.
     */
    @Test
    public void varargsDeclaredTypeAnnotationPathStartsWithTheImplicitArrayStep()
            throws IOException {
        BinaryStubData data =
                roundTrip(
                        "import java.util.List;\n"
                                + "import org.checkerframework.checker.nullness.qual.Nullable;\n"
                                + "public class VarargsEmbedded {\n"
                                + "  public void m(List<@Nullable String>... args) {}\n"
                                + "}\n",
                        /* omitUnannotatedMembers= */ true);

        BinaryStubData.ClassRecord cr = data.classes.get("VarargsEmbedded");
        Assert.assertNotNull("VarargsEmbedded must be recorded", cr);
        Assert.assertEquals("method m must be recorded", 1, cr.methods.length);
        BinaryStubData.MethodRecord mr = cr.methods[0];

        Assert.assertEquals("m has one parameter", 1, mr.paramAnnos.length);
        BinaryStubData.TypeAnno[] argAnnos = mr.paramAnnos[0];
        Assert.assertEquals(
                "@Nullable is the only type annotation on the vararg parameter",
                1,
                argAnnos.length);
        BinaryStubData.TypePathStep[] path = argAnnos[0].path;
        Assert.assertEquals(
                "path must be [ARRAY, TYPE_ARGUMENT]: one step for the vararg's implicit array"
                        + " level, then one into List's type argument -- without the leading"
                        + " ARRAY step the reader resolves TYPE_ARGUMENT against the array type"
                        + " and drops the annotation",
                2,
                path.length);
        Assert.assertEquals("first step is ARRAY", 0, path[0].kind);
        Assert.assertEquals("second step is TYPE_ARGUMENT", 3, path[1].kind);
        Assert.assertEquals("of the first type argument", 0, path[1].argIndex);
    }

    /**
     * The empty-path counterpart of {@link
     * #varargsDeclaredTypeAnnotationPathStartsWithTheImplicitArrayStep}: an annotation written
     * inside a fully-qualified vararg element type ({@code java.lang.@Nullable String... args}) is
     * attached by JavaParser to the type, not to the parameter declaration, so it reaches the
     * writer through {@code extractTypeAnnotations(p.getType(), ...)} with an empty path within the
     * declared type. Without the implicit array level's leading {@code ARRAY} step, the empty path
     * lands the annotation on the array type itself instead of the element type -- the text parser
     * puts it on the element type ({@code annotate} on the array's component type).
     */
    @Test
    public void varargsFullyQualifiedElementTypeAnnotationGetsTheImplicitArrayStep()
            throws IOException {
        BinaryStubData data =
                roundTrip(
                        "import org.checkerframework.checker.nullness.qual.Nullable;\n"
                                + "public class VarargsFqElement {\n"
                                + "  public void m(java.lang.@Nullable String... args) {}\n"
                                + "}\n",
                        /* omitUnannotatedMembers= */ true);

        BinaryStubData.ClassRecord cr = data.classes.get("VarargsFqElement");
        Assert.assertNotNull("VarargsFqElement must be recorded", cr);
        Assert.assertEquals("method m must be recorded", 1, cr.methods.length);
        BinaryStubData.MethodRecord mr = cr.methods[0];

        Assert.assertEquals("m has one parameter", 1, mr.paramAnnos.length);
        BinaryStubData.TypeAnno[] argAnnos = mr.paramAnnos[0];
        Assert.assertEquals(
                "@Nullable is the only type annotation on the vararg parameter",
                1,
                argAnnos.length);
        BinaryStubData.TypePathStep[] path = argAnnos[0].path;
        Assert.assertEquals(
                "path must be the single ARRAY step for the vararg's implicit array level -- an"
                        + " empty path would land the annotation on the array type itself instead"
                        + " of the element type",
                1,
                path.length);
        Assert.assertEquals("the step is ARRAY", 0, path[0].kind);
    }

    /**
     * Returns the sole element value of the sole declaration annotation on the method with the
     * given simple name.
     *
     * @param data the loaded binary stub data
     * @param cr the class containing the method
     * @param methodSimpleName the method's simple name (its signature must start with this followed
     *     by "(")
     * @return the sole element value
     */
    private static Object soleDeclAnnoValue(
            BinaryStubData data, BinaryStubData.ClassRecord cr, String methodSimpleName) {
        for (BinaryStubData.MethodRecord mr : cr.methods) {
            if (data.stringPool[mr.sigIndex].startsWith(methodSimpleName + "(")) {
                Assert.assertEquals(
                        "method " + methodSimpleName + " has one declaration annotation",
                        1,
                        mr.declAnnos.length);
                BinaryStubData.AnnotationRecord ar = data.annotationPool[mr.declAnnos[0]];
                Assert.assertEquals(
                        "annotation " + methodSimpleName + " has one element value",
                        1,
                        ar.elementValues.size());
                return ar.elementValues.values().iterator().next();
            }
        }
        throw new AssertionError("method " + methodSimpleName + " not found");
    }

    /**
     * Regression test for an {@code EnclosedExpr} crash: redundant parentheses around an annotation
     * value (e.g. {@code @SuppressWarnings(("unchecked"))}) are legal Java, but {@code writeValue}
     * had no case for the parenthesized-expression AST node and fell through to its "unsupported
     * annotation value" branch, throwing {@code IOException} -- which {@link
     * BinaryStubWriter#processTypes} rethrows as an uncaught {@code RuntimeException}, aborting the
     * entire binary stub generation run over one such value anywhere in the source tree.
     */
    @Test
    public void parenthesizedAnnotationValueDoesNotCrash() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse(
                        "public class EnclosedExprTest {\n"
                                + "  @SuppressWarnings((\"unchecked\")) void m() {}\n"
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

            BinaryStubData.ClassRecord cr = data.classes.get("EnclosedExprTest");
            Assert.assertNotNull("EnclosedExprTest must be recorded", cr);
            Assert.assertEquals("unchecked", soleDeclAnnoValue(data, cr, "m"));
        } finally {
            tmp.delete();
        }
    }

    /**
     * Confirms a mixed-target annotation (valid in both declaration and {@code TYPE_USE} positions,
     * e.g. {@code org.checkerframework.checker.mustcall.qual.MustCallAlias}, whose {@code @Target}
     * includes both {@code METHOD} and {@code TYPE_USE}) written in declaration-position before a
     * method's return type is stored in <em>both</em> {@code MethodRecord.declAnnos} and {@code
     * MethodRecord.returnTypeAnnos}, not just one -- matching the dual-storage pattern documented
     * at {@link BinaryStubWriter#isTypeUseOnly} and confirming a review claim that such annotations
     * are silently dropped for method return types does not reproduce against the current code.
     */
    @Test
    public void mixedTargetAnnotationOnReturnTypeIsStoredInBothPlaces() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse(
                        "import org.checkerframework.checker.mustcall.qual.MustCallAlias;\n"
                                + "public class MixedTargetTest {\n"
                                + "  public @MustCallAlias String m() { return null; }\n"
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

            BinaryStubData.ClassRecord cr = data.classes.get("MixedTargetTest");
            Assert.assertNotNull("MixedTargetTest must be recorded", cr);
            BinaryStubData.MethodRecord mr = null;
            for (BinaryStubData.MethodRecord candidate : cr.methods) {
                if (data.stringPool[candidate.sigIndex].startsWith("m(")) {
                    mr = candidate;
                }
            }
            Assert.assertNotNull("method m must be recorded", mr);

            Assert.assertEquals(
                    "@MustCallAlias must be stored as a declaration annotation on the method",
                    1,
                    mr.declAnnos.length);
            Assert.assertEquals(
                    "@MustCallAlias must ALSO be stored as a type annotation on the return type"
                            + " -- not dropped just because it is also a declaration annotation",
                    1,
                    mr.returnTypeAnnos.length);
            String declAnnoName = data.stringPool[data.annotationPool[mr.declAnnos[0]].nameIndex];
            String typeAnnoName =
                    data.stringPool[data.annotationPool[mr.returnTypeAnnos[0].annoIndex].nameIndex];
            Assert.assertEquals(
                    "org.checkerframework.checker.mustcall.qual.MustCallAlias", declAnnoName);
            Assert.assertEquals(
                    "org.checkerframework.checker.mustcall.qual.MustCallAlias", typeAnnoName);
        } finally {
            tmp.delete();
        }
    }

    /**
     * Confirms a record declaration is written as a {@code KIND_RECORD} class record with one
     * {@code ComponentRecord} per header component, in header order, each carrying its own type
     * annotations and {@code hasAccessor} flag: a component with an explicit zero-argument accessor
     * of the same name in the record body must have {@code hasAccessor == true}, and one without
     * must have {@code hasAccessor == false} -- matching {@code AnnotationFileParser}'s {@code
     * hasAccessorInStubs} semantics.
     */
    @Test
    public void recordComponentAnnotationsAndAccessorFlagAreWritten() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                parseRecord(
                        "import org.checkerframework.checker.nullness.qual.Nullable;\n"
                                + "public record PersonRecord(@Nullable String name, int age) {\n"
                                + "  public String name() { return name; }\n"
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

            BinaryStubData.ClassRecord cr = data.classes.get("PersonRecord");
            Assert.assertNotNull("PersonRecord must be recorded", cr);
            Assert.assertEquals(
                    "PersonRecord's kind must be KIND_RECORD",
                    BinaryStubData.ClassRecord.KIND_RECORD,
                    cr.kind);
            Assert.assertEquals("PersonRecord has two components", 2, cr.components.length);

            BinaryStubData.ComponentRecord name = cr.components[0];
            BinaryStubData.ComponentRecord age = cr.components[1];
            Assert.assertEquals(
                    "components must be in header order", "name", data.stringPool[name.nameIndex]);
            Assert.assertEquals(
                    "components must be in header order", "age", data.stringPool[age.nameIndex]);

            Assert.assertEquals(
                    "the @Nullable component has one type annotation", 1, name.typeAnnos.length);
            Assert.assertEquals(
                    "the unannotated component has no type annotations", 0, age.typeAnnos.length);

            Assert.assertTrue(
                    "name has an explicit zero-arg accessor in the record body", name.hasAccessor);
            Assert.assertFalse("age has no explicit accessor in the record body", age.hasAccessor);
        } finally {
            tmp.delete();
        }
    }

    /**
     * Confirms an explicit (non-compact) canonical constructor -- one whose parameter types match
     * the record header's components in count and order -- produces a {@code
     * canonicalConstructorParamAnnos} override carrying the constructor's own parameter
     * annotations, matching {@code AnnotationFileParser}'s {@code
     * RecordStub#componentsInCanonicalConstructor}.
     */
    @Test
    public void explicitCanonicalConstructorProducesOverride() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                parseRecord(
                        "import org.checkerframework.checker.nullness.qual.Nullable;\n"
                                + "public record Box(String value) {\n"
                                + "  public Box(@Nullable String value) {\n"
                                + "    this.value = value;\n"
                                + "  }\n"
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

            BinaryStubData.ClassRecord cr = data.classes.get("Box");
            Assert.assertNotNull("Box must be recorded", cr);
            Assert.assertNotNull(
                    "an explicit canonical constructor must produce a"
                            + " canonicalConstructorParamAnnos override",
                    cr.canonicalConstructorParamAnnos);
            Assert.assertEquals(1, cr.canonicalConstructorParamAnnos.length);
            Assert.assertEquals(
                    "the explicit constructor's own @Nullable must be recorded",
                    1,
                    cr.canonicalConstructorParamAnnos[0].length);
        } finally {
            tmp.delete();
        }
    }

    /**
     * Confirms a compact canonical constructor (no parameter list of its own to carry annotations)
     * does not produce a {@code canonicalConstructorParamAnnos} override, leaving the record
     * components' own annotations as the source of truth.
     */
    @Test
    public void compactCanonicalConstructorDoesNotProduceOverride() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                parseRecord("public record Box(String value) {\n  public Box {\n  }\n}\n");
        writer.process(cu);

        File tmp = File.createTempFile("binarystubwritertest", ".bin.gz");
        tmp.deleteOnExit();
        try {
            writer.writeTo(tmp);
            BinaryStubData data;
            try (InputStream in = Files.newInputStream(tmp.toPath())) {
                data = new BinaryStubData(in);
            }

            BinaryStubData.ClassRecord cr = data.classes.get("Box");
            Assert.assertNotNull("Box must be recorded", cr);
            Assert.assertNull(
                    "a compact canonical constructor has no parameter list of its own to override"
                            + " with, so no canonicalConstructorParamAnnos should be recorded",
                    cr.canonicalConstructorParamAnnos);
        } finally {
            tmp.delete();
        }
    }

    /**
     * A bare reference to a statically-imported constant (e.g. {@code MAX_VALUE} from {@code import
     * static java.lang.Integer.MAX_VALUE;}) must be written the same way a qualified reference
     * ({@code Integer.MAX_VALUE}) is -- the {@code 'e'} tag, qualified with the constant's
     * declaring class -- not as an unqualified {@code NameLiteralValue}. {@code
     * BinaryStubReader#findFieldInType}, which resolves an unqualified {@code NameLiteralValue},
     * only searches the annotation's enclosing class and its supertypes; a constant from an
     * unrelated class reached only through a static import is invisible to it and would otherwise
     * be silently dropped, unlike the text parser ({@code
     * AnnotationFileParser#findVariableElement(NameExpr)}), which resolves it through its {@code
     * importedConstants} table.
     */
    @Test
    public void staticImportedConstantIsWrittenQualified() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse(
                        "import static java.lang.Integer.MAX_VALUE;\n"
                                + "import org.checkerframework.common.value.qual.IntRange;\n"
                                + "public class StaticImportTest {\n"
                                + "  public @IntRange(to = MAX_VALUE) int f;\n"
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

            BinaryStubData.ClassRecord cr = data.classes.get("StaticImportTest");
            Assert.assertNotNull("StaticImportTest must be recorded", cr);
            Assert.assertEquals("f has one field record", 1, cr.fields.length);
            BinaryStubData.FieldRecord fr = cr.fields[0];
            Assert.assertEquals("@IntRange is a type annotation", 1, fr.typeAnnos.length);
            BinaryStubData.AnnotationRecord ar = data.annotationPool[fr.typeAnnos[0].annoIndex];
            Assert.assertEquals(
                    "org.checkerframework.common.value.qual.IntRange",
                    data.stringPool[ar.nameIndex]);
            Assert.assertEquals(
                    "@IntRange(to = ...) has one element value", 1, ar.elementValues.size());

            Object value = ar.elementValues.values().iterator().next();
            Assert.assertTrue(
                    "the statically-imported MAX_VALUE must be written as an EnumConstantValue"
                            + " (the 'e' tag), not a NameLiteralValue (the 'n' tag), but was: "
                            + value,
                    value instanceof BinaryStubData.EnumConstantValue);
            BinaryStubData.EnumConstantValue ev = (BinaryStubData.EnumConstantValue) value;
            Assert.assertEquals("java.lang.Integer", ev.enumClassName);
            Assert.assertEquals("MAX_VALUE", ev.constantName);
        } finally {
            tmp.delete();
        }
    }

    /**
     * T20(a): a class literal naming an unimported class in the stub file's own package must be
     * qualified against that package, mirroring the text parser's package-prefix fallback ({@code
     * AnnotationFileParser.findTypeOfName}). Before this fix, {@code fullyQualify(String,
     * CompilationUnit)} tried only the explicit imports and asterisk imports, leaving the class
     * literal as the bare simple name {@code "BinaryStubWriter"} -- which {@code
     * BinaryStubReader#resolveSingleValue}'s single {@code Elements.getTypeElement(fqName)} call
     * cannot resolve, silently dropping the class-literal annotation element.
     */
    @Test
    public void classLiteralInCurrentPackageIsFullyQualified() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse(
                        "package org.checkerframework.framework.stubifier;\n"
                                + "import org.checkerframework.framework.qual.RelevantJavaTypes;\n"
                                + "@RelevantJavaTypes(BinaryStubWriter.class)\n"
                                + "public class UsesSamePackageLiteral { }\n");
        writer.process(cu);

        File tmp = File.createTempFile("binarystubwritertest", ".bin.gz");
        tmp.deleteOnExit();
        try {
            writer.writeTo(tmp);
            BinaryStubData data;
            try (InputStream in = Files.newInputStream(tmp.toPath())) {
                data = new BinaryStubData(in);
            }
            BinaryStubData.ClassRecord cr =
                    data.classes.get(
                            "org.checkerframework.framework.stubifier.UsesSamePackageLiteral");
            Assert.assertNotNull("UsesSamePackageLiteral must be recorded", cr);
            Assert.assertEquals(1, cr.declAnnos.length);
            BinaryStubData.AnnotationRecord ar = data.annotationPool[cr.declAnnos[0]];
            Object value = ar.elementValues.values().iterator().next();
            Assert.assertTrue(
                    "the class literal must be written as a ClassLiteralValue, but was: " + value,
                    value instanceof BinaryStubData.ClassLiteralValue);
            Assert.assertEquals(
                    "an unimported class in the stub file's own package must be qualified"
                            + " against that package",
                    "org.checkerframework.framework.stubifier.BinaryStubWriter",
                    ((BinaryStubData.ClassLiteralValue) value).className);
        } finally {
            tmp.delete();
        }
    }

    /**
     * T20(b): a scoped class-literal type like {@code Map.Entry} must be qualified through its
     * outermost scope, not skipped just because {@code ClassOrInterfaceType.getScope()} is present.
     * Before this fix, {@code fullyQualify(Type, CompilationUnit)} qualified a {@code
     * ClassOrInterfaceType} only when {@code !cit.getScope().isPresent()}, so {@code Map.Entry} was
     * written as-is even with {@code import java.util.Map;} in scope.
     */
    @Test
    public void scopedClassLiteralIsFullyQualified() throws IOException {
        BinaryStubWriter writer = new BinaryStubWriter();
        CompilationUnit cu =
                StaticJavaParser.parse(
                        "import java.util.Map;\n"
                                + "import org.checkerframework.framework.qual.RelevantJavaTypes;\n"
                                + "@RelevantJavaTypes(Map.Entry.class)\n"
                                + "public class UsesScopedLiteral { }\n");
        writer.process(cu);

        File tmp = File.createTempFile("binarystubwritertest", ".bin.gz");
        tmp.deleteOnExit();
        try {
            writer.writeTo(tmp);
            BinaryStubData data;
            try (InputStream in = Files.newInputStream(tmp.toPath())) {
                data = new BinaryStubData(in);
            }
            BinaryStubData.ClassRecord cr = data.classes.get("UsesScopedLiteral");
            Assert.assertNotNull("UsesScopedLiteral must be recorded", cr);
            Assert.assertEquals(1, cr.declAnnos.length);
            BinaryStubData.AnnotationRecord ar = data.annotationPool[cr.declAnnos[0]];
            Object value = ar.elementValues.values().iterator().next();
            Assert.assertTrue(
                    "the class literal must be written as a ClassLiteralValue, but was: " + value,
                    value instanceof BinaryStubData.ClassLiteralValue);
            Assert.assertEquals(
                    "a scoped type like Map.Entry must be qualified through its outermost scope",
                    "java.util.Map.Entry",
                    ((BinaryStubData.ClassLiteralValue) value).className);
        } finally {
            tmp.delete();
        }
    }

    /**
     * Parses {@code source} with language level JAVA_21, required for record declarations.
     * StaticJavaParser's default language level is older; production code
     * (JavaStubifier.DEFAULT_LANGUAGE_LEVEL, BinaryStubFileGenerator.parseStubUnit) both configure
     * JAVA_21.
     *
     * @param source the source text to parse, containing a record declaration
     * @return the parsed compilation unit
     */
    private static CompilationUnit parseRecord(String source) {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        return new JavaParser(configuration).parse(source).getResult().get();
    }
}

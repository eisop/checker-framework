package org.checkerframework.framework.stub;

import com.sun.source.tree.CompilationUnitTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalNameOrEmpty;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.FromStubFile;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.stub.AnnotationFileParser.AnnotationFileAnnotations;
import org.checkerframework.framework.stub.AnnotationFileParser.RecordComponentStub;
import org.checkerframework.framework.stub.AnnotationFileUtil.AnnotationFileType;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.IPair;
import org.plumelib.util.SystemPlume;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

// import io.github.classgraph.ClassGraph;

/**
 * Holds information about types parsed from annotation files (stub files or ajava files). When
 * using an ajava file, only holds information on public elements as with stub files.
 */
public class AnnotationFileElementTypes {

    /** Name of the annotated JDK directory inside {@code checker.jar}. */
    private static final String ANNOTATED_JDK_PATH = "annotated-jdk";

    /** Annotations from annotation files (but not from annotated JDK files). */
    final AnnotationFileAnnotations annotationFileAnnos;

    /** The number of ongoing parsing tasks. */
    private int parsingCount;

    /** AnnotatedTypeFactory. */
    private final AnnotatedTypeFactory atypeFactory;

    /**
     * Mapping from fully-qualified class name to corresponding JDK stub file from the file system
     * that have not yet been read. When a file is read, its mapping is removed from this map.
     *
     * <p>By contrast, {@link #remainingJdkStubFilesJar} contains JDK stub files from checker.jar.
     */
    private final Map<String, Path> remainingJdkStubFiles = new HashMap<>();

    /**
     * Mapping from fully-qualified class name to the corresponding JDK stub file name inside
     * checker.jar that has not yet been read. When a file is read, its mapping is removed from this
     * map.
     *
     * <p>By contrast, {@link #remainingJdkStubFiles} contains JDK stub files from the file system.
     * When the binary stub path is active, both maps are also pruned for any class handled via
     * {@link BinaryStubReader}, so the text parser is not invoked for classes already loaded from
     * the binary format.
     */
    private final Map<String, String> remainingJdkStubFilesJar = new HashMap<>();

    /**
     * Cache for binary stub data and its associated metadata (e.g., inner classes mapping). This
     * cache is stored in the compilation context to be shared across all checker and factory
     * instances in the same compilation run.
     *
     * <p>The {@link BinaryStubData} itself contains no javac objects (only strings and primitives),
     * so it is additionally cached per JVM in {@link #loadedBinaryStubData}; only this wrapper,
     * whose caches hold javac {@code Element}s and {@code AnnotationMirror}s, is per-compilation.
     */
    static class BinaryStubDataCache {
        /** The loaded binary stub data. */
        final BinaryStubData data;

        /**
         * A map from the fully-qualified name of an outermost class to the records of its inner
         * classes. Computed lazily.
         */
        Map<String, List<BinaryStubData.ClassRecord>> innerClassesMap = null;

        /**
         * Cache of parsed annotation mirrors. Shared across all factories in the same compilation
         * to avoid repeatedly creating identical annotation mirrors for the same record. Keyed by
         * identity: each {@link BinaryStubData} creates one canonical {@code AnnotationRecord}
         * instance per pool entry, and structural equality must not be used because records from
         * different binary files (the annotated JDK, per-checker {@code .astub} binaries) contain
         * indices into different string pools.
         */
        final IdentityHashMap<BinaryStubData.AnnotationRecord, AnnotationMirror> annoCache =
                new IdentityHashMap<>();

        /** Cache of resolved {@code Class} literal types to avoid repeated element lookups. */
        final Map<String, TypeMirror> resolvedClassTypesCache = new HashMap<>();

        /** Cache of resolved constant values to avoid repeated class hierarchy lookups. */
        final Map<String, Object> resolvedConstantsCache = new HashMap<>();

        /**
         * Map from fully-qualified class name to the name of the jar entry containing its text
         * stub, shared across all factories in the compilation so the checker.jar entry list is
         * enumerated only once per compilation rather than once per factory. {@code null} until the
         * first factory performs the enumeration. Only used when the JDK stubs come from a jar; see
         * {@link #jdkStubPathsByClass} for the directory case.
         */
        @Nullable Map<String, String> jdkJarEntriesByClass = null;

        /**
         * Map from fully-qualified class name to the path of the file containing its text stub,
         * shared across all factories in the compilation so the JDK directory is walked only once
         * per compilation rather than once per factory. {@code null} until the first factory
         * performs the walk. Only used when the JDK stubs come from a directory; see {@link
         * #jdkJarEntriesByClass} for the jar case.
         */
        @Nullable Map<String, Path> jdkStubPathsByClass = null;

        /**
         * Constructs a new cache holding the given binary stub data.
         *
         * @param data the binary stub data to cache
         */
        BinaryStubDataCache(BinaryStubData data) {
            this.data = data;
        }
    }

    /**
     * Binary stub data parsed from {@code annotated-jdk.bin.gz}, cached for the lifetime of the
     * JVM, keyed by the URL of the binary stub resource. {@link BinaryStubData} is immutable after
     * construction and contains only strings and primitives (no javac objects), so it can safely be
     * shared across compilations — e.g. across the many in-process compilations of a test-suite
     * run, or across builds in a persistent Gradle worker — avoiding a re-read and re-parse of the
     * ~340 KB compressed file for every compilation.
     */
    private static final Map<String, BinaryStubData> loadedBinaryStubData =
            new ConcurrentHashMap<>(2);

    /**
     * The key used to store and retrieve the {@link BinaryStubDataCache} from the compilation
     * context.
     */
    private static final com.sun.tools.javac.util.Context.Key<BinaryStubDataCache>
            BINARY_STUB_DATA_KEY = new com.sun.tools.javac.util.Context.Key<>();

    /**
     * Locally cached reference to the compilation-context {@link BinaryStubDataCache}, avoiding
     * repeated casts through the processing environment on every call.
     */
    private @Nullable BinaryStubDataCache cachedBinaryStubDataCache = null;

    /**
     * Retrieves the {@link BinaryStubDataCache} from the compilation context.
     *
     * @return the cached binary stub data, or {@code null} if it has not been loaded yet
     */
    @Nullable BinaryStubDataCache getBinaryStubDataCache() {
        if (cachedBinaryStubDataCache != null) {
            return cachedBinaryStubDataCache;
        }
        com.sun.tools.javac.util.Context context =
                ((com.sun.tools.javac.processing.JavacProcessingEnvironment)
                                atypeFactory.getChecker().getProcessingEnvironment())
                        .getContext();
        cachedBinaryStubDataCache = context.get(BINARY_STUB_DATA_KEY);
        return cachedBinaryStubDataCache;
    }

    /**
     * Stores the {@link BinaryStubDataCache} in the compilation context.
     *
     * @param cache the cache to store
     */
    private void setBinaryStubDataCache(BinaryStubDataCache cache) {
        com.sun.tools.javac.util.Context context =
                ((com.sun.tools.javac.processing.JavacProcessingEnvironment)
                                atypeFactory.getChecker().getProcessingEnvironment())
                        .getContext();
        context.put(BINARY_STUB_DATA_KEY, cache);
        cachedBinaryStubDataCache = cache;
    }

    /**
     * Set of fully-qualified class names whose annotations have already been applied from the
     * binary JDK stub. Used to ensure each class is only processed once via the binary path,
     * without interfering with {@link #processingClasses} which is managed by {@link
     * AnnotationFileParser}.
     */
    private final Set<String> processedBinaryClasses = new HashSet<>();

    /**
     * Retrieves all inner class records for a given outermost class name from the binary data.
     *
     * @param outermostClass the fully-qualified name of the outermost class
     * @return the list of inner class records, or an empty list if none are found
     */
    List<BinaryStubData.ClassRecord> getInnerClassesFromBinary(String outermostClass) {
        BinaryStubDataCache cache = getBinaryStubDataCache();
        if (cache == null) {
            return Collections.emptyList();
        }
        if (cache.innerClassesMap == null) {
            cache.innerClassesMap = new HashMap<>();
            for (BinaryStubData.ClassRecord cr : cache.data.classes.values()) {
                if (cr.outerNameIndex != 0) {
                    String outerName = cache.data.stringPool[cr.outerNameIndex];
                    cache.innerClassesMap
                            .computeIfAbsent(outerName, x -> new ArrayList<>())
                            .add(cr);
                }
            }
        }
        return cache.innerClassesMap.getOrDefault(outermostClass, Collections.emptyList());
    }

    /** Which version number of the annotated JDK should be used? */
    private final String annotatedJdkVersion;

    /** Should the JDK be parsed? */
    private final boolean shouldParseJdk;

    /**
     * True if this is the stub-types AFET ({@code stubTypes}); false if it is an ajava-types AFET
     * ({@code ajavaTypes} or {@code currentFileAjavaTypes}). Binary JDK stub loading is only
     * performed for stub-types AFETs; loading via an ajava-types AFET would happen during {@code
     * AnnotatedTypeFactory.fromElement()} before user-supplied stub files are fully parsed, causing
     * the binary's annotations to override the user stubs.
     */
    private final boolean isStubTypes;

    /** Parse all JDK files at startup rather than as needed. */
    private final boolean parseAllJdkFiles;

    /** True if -ApermitMissingJdk was passed on the command line. */
    private final boolean permitMissingJdk;

    /** True if -Aignorejdkastub was passed on the command line. */
    private final boolean ignorejdkastub;

    /** True if -AstubDebug was passed on the command line. */
    private final boolean stubDebug;

    /**
     * Stores the fully qualified name of top-level classes (from any type of stub file) that are
     * currently being parsed. This can stop recursively parsing an annotated JDK class that is
     * currently being processed, which prevents conflicts of definition and infinite loops.
     */
    private final Set<String> processingClasses = new LinkedHashSet<>();

    /**
     * Cache from TypeElement to (method simple-signature → ExecutableElement) for methods declared
     * directly in that TypeElement. Shared across all {@link AnnotationFileParser} instances within
     * this factory so that the O(N) index-build cost is paid at most once per class per
     * compilation.
     */
    private final IdentityHashMap<TypeElement, Map<String, ExecutableElement>> methodSigIndexCache =
            new IdentityHashMap<>();

    /**
     * Cache from TypeElement to (constructor simple-signature → ExecutableElement) for constructors
     * declared directly in that TypeElement.
     */
    private final IdentityHashMap<TypeElement, Map<String, ExecutableElement>>
            constructorSigIndexCache = new IdentityHashMap<>();

    /**
     * Returns a map from simple signature to ExecutableElement for all methods declared directly in
     * {@code typeElt}. The map is built once per TypeElement and reused across stub files.
     *
     * @param typeElt the type element
     * @return map from method simple signature to ExecutableElement
     */
    Map<String, ExecutableElement> methodSigIndex(TypeElement typeElt) {
        return methodSigIndexCache.computeIfAbsent(
                typeElt, t -> buildSigIndex(ElementFilter.methodsIn(t.getEnclosedElements())));
    }

    /**
     * Returns a map from simple signature to ExecutableElement for all constructors declared
     * directly in {@code typeElt}. The map is built once per TypeElement and reused across stub
     * files.
     *
     * @param typeElt the type element
     * @return map from constructor simple signature to ExecutableElement
     */
    Map<String, ExecutableElement> constructorSigIndex(TypeElement typeElt) {
        return constructorSigIndexCache.computeIfAbsent(
                typeElt, t -> buildSigIndex(ElementFilter.constructorsIn(t.getEnclosedElements())));
    }

    /**
     * Builds a map from simple signature to element for the given executables. Package-private
     * (rather than private) so that {@code AnnotationFileElementTypesTest} can exercise it directly
     * with a constructed list of executables, without needing a full {@link AnnotatedTypeFactory}.
     *
     * @param executables the methods or constructors to index
     * @return map from simple signature to element
     */
    static Map<String, ExecutableElement> buildSigIndex(List<ExecutableElement> executables) {
        Map<String, ExecutableElement> index = new HashMap<>(executables.size() * 2);
        for (ExecutableElement executable : executables) {
            try {
                index.put(ElementUtils.getSimpleSignature(executable), executable);
            } catch (BugInCF e) {
                // Skip executables whose signature cannot be computed, e.g. because a parameter
                // type is an unresolvable (error) type -- TypesUtils.simpleTypeName throws
                // BugInCF for exactly that case, e.g. a JDK-internal parameter type like
                // sun.util.locale.provider.LocaleResources that is not exported to the annotation
                // processor's module. Such executables cannot be matched by any stub record anyway.
            }
        }
        return index;
    }

    /**
     * Creates an empty annotation source.
     *
     * @param atypeFactory a type factory
     * @param isStubTypes true if this AFET holds stub-file annotations (it is the stub-types field
     *     of {@link AnnotatedTypeFactory}); false if it holds ajava-file annotations ({@code
     *     ajavaTypes} or {@code currentFileAjavaTypes}). Binary JDK stub loading is only performed
     *     when this is true; see {@link #isStubTypes}.
     */
    public AnnotationFileElementTypes(AnnotatedTypeFactory atypeFactory, boolean isStubTypes) {
        this.atypeFactory = atypeFactory;
        this.isStubTypes = isStubTypes;
        this.annotationFileAnnos = new AnnotationFileAnnotations();
        this.parsingCount = 0;
        String release = SystemUtil.getReleaseValue(atypeFactory.getProcessingEnv());
        this.annotatedJdkVersion =
                release != null ? release : String.valueOf(SystemUtil.jreVersion);

        SourceChecker checker = atypeFactory.getChecker();
        this.ignorejdkastub = checker.hasOption("ignorejdkastub");
        this.shouldParseJdk = !ignorejdkastub;
        this.parseAllJdkFiles = checker.hasOption("parseAllJdk");
        this.permitMissingJdk = checker.hasOption("permitMissingJdk");
        this.stubDebug = checker.hasOption("stubDebug");
    }

    /**
     * Returns true if files are currently being parsed; otherwise, false.
     *
     * @return true if files are currently being parsed; otherwise, false
     */
    public boolean isParsing() {
        return parsingCount > 0;
    }

    /**
     * The {@code @FromStubFile} mirror used to mark elements loaded from binary built-in stub
     * files, built lazily by {@link #getFromStubFileAnno}.
     */
    private @Nullable AnnotationMirror fromStubFileAnno = null;

    /**
     * Returns the {@code @FromStubFile} mirror, building it on first use.
     *
     * @return the {@code @FromStubFile} mirror
     */
    private AnnotationMirror getFromStubFileAnno() {
        if (fromStubFileAnno == null) {
            fromStubFileAnno =
                    AnnotationBuilder.fromClass(atypeFactory.getElementUtils(), FromStubFile.class);
        }
        return fromStubFileAnno;
    }

    /**
     * Returns the parsed binary stub data for the given resource, reading and parsing it on the
     * first request in this JVM and returning the cached copy afterwards (see {@link
     * #loadedBinaryStubData}).
     *
     * @param binURL the URL of the {@code .bin.gz} resource
     * @return the parsed binary stub data
     * @throws IOException if the resource cannot be read or has an invalid format
     */
    private static BinaryStubData loadBinaryStubData(URL binURL) throws IOException {
        BinaryStubData data = loadedBinaryStubData.get(binURL.toString());
        if (data == null) {
            try (InputStream in = binURL.openStream()) {
                data = new BinaryStubData(in);
            }
            loadedBinaryStubData.putIfAbsent(binURL.toString(), data);
        }
        return data;
    }

    /**
     * Loads a built-in stub file from its pre-parsed binary form and applies all of it eagerly,
     * marking matched elements with {@code @FromStubFile} exactly as the text parser does for
     * built-in stub files. The parsed binary is cached per JVM.
     *
     * <p>When the {@code -AbinaryStubDiffCheck} option is set, the same stub file is also text
     * parsed into a scratch container and compared against the binary result; see {@link
     * BinaryStubDiffChecker#diffBuiltinStub}.
     *
     * @param binURL the URL of the {@code .bin.gz} resource
     * @param textResourceURL the URL of the corresponding {@code .astub} resource, used by the
     *     differential check; may be {@code null} if unknown
     * @param description resource description for diagnostics
     * @return true if the binary was loaded and applied; false if it could not be read (the caller
     *     should fall back to text parsing)
     */
    private boolean loadBuiltinBinaryStub(
            URL binURL, @Nullable URL textResourceURL, String description) {
        BinaryStubData data;
        try {
            data = loadBinaryStubData(binURL);
        } catch (IOException e) {
            atypeFactory
                    .getChecker()
                    .message(
                            Diagnostic.Kind.NOTE,
                            "Could not read "
                                    + binURL
                                    + ", falling back to text parsing. Error: "
                                    + e.getMessage());
            return false;
        }
        applyBinaryStubData(data, annotationFileAnnos);
        if (isStubTypes
                && textResourceURL != null
                && atypeFactory.getChecker().hasOption("binaryStubDiffCheck")) {
            BinaryStubDiffChecker.diffBuiltinStub(
                    description, textResourceURL, data, this, atypeFactory);
        }
        return true;
    }

    /**
     * Applies every record of a built-in stub file's binary data into {@code target}: package and
     * module annotations, then every class record, with {@code @FromStubFile} marking.
     *
     * <p>Unlike the annotated JDK, built-in stub files are small and are parsed eagerly by the text
     * parser, so the binary form is also applied eagerly rather than per requested class.
     *
     * @param data the binary stub data of one built-in stub file
     * @param target the annotation container to apply into
     */
    void applyBinaryStubData(BinaryStubData data, AnnotationFileAnnotations target) {
        BinaryStubReader.applyPackageAndModuleRecords(
                data, atypeFactory, this, target, /* fromLazyJdk= */ false);
        AnnotationMirror fromStubFile = getFromStubFileAnno();
        for (Map.Entry<String, BinaryStubData.ClassRecord> entry : data.classes.entrySet()) {
            BinaryStubReader.applyClassRecord(
                    entry.getValue(),
                    entry.getKey(),
                    atypeFactory,
                    this,
                    data,
                    target,
                    fromStubFile);
        }
    }

    /**
     * Parses the stub files in the following order:
     *
     * <ol>
     *   <li>{@code jdk.astub} in the same directory as the checker, if it exists and the {@code
     *       ignorejdkastub} option is not supplied;
     *   <li>{@code jdkN.astub} (where N is the current Java version or any higher value) in the
     *       same directory as the checker, if it exists and the {@code ignorejdkastub} option is
     *       not supplied;
     *   <li>If parsing the annotated JDK as stub files, all {@code package-info.java} files under
     *       the {@code jdk/} directory;
     *   <li>Stub files listed in a {@code @StubFiles} annotation on the checker; these files must
     *       be in same directory as the checker;
     *   <li>Stub files returned by {@link BaseTypeChecker#getExtraStubFiles} (treated like those
     *       listed in a {@code @StubFiles} annotation on the checker);
     *   <li>Stub files provided via the {@code -Astubs} compiler option.
     * </ol>
     *
     * <p>If a type is annotated with a qualifier from the same hierarchy in more than one stub
     * file, the qualifier in the last stub file is applied.
     *
     * <p>If using JDK 11, then the JDK stub files are only parsed if a type or declaration
     * annotation is requested from a class in that file.
     */
    // TODO: it's unclear for what Java versions a jdkN.astub is parsed.
    public void parseStubFiles() {
        assert parsingCount == 0;
        ++parsingCount;
        BaseTypeChecker checker = atypeFactory.getChecker();
        if (stubDebug) {
            System.out.printf(
                    "entered parseStubFiles() for %s, ignorejdkastub=%s%n",
                    atypeFactory.getClass().getSimpleName(), ignorejdkastub);
        }
        if (!ignorejdkastub) {
            // 1. Annotated JDK
            // This preps but does not parse the JDK files (except package-info.java files).
            // The JDK source code files will be parsed later, on demand.
            prepJdkStubs();

            // 2. jdk.astub
            // Only look in .jar files, and parse it right away.
            String[] jdkVersions = {"", annotatedJdkVersion};
            for (String jdkVersion : jdkVersions) {
                String jdkVersionStub = "jdk" + jdkVersion + ".astub";
                parseOneStubFile(this.getClass(), jdkVersionStub);
                parseOneStubFile(checker.getClass(), jdkVersionStub);
            }
            // This needs to be special-cased for every jdkX.astub for which files exist. :-(
            // TODO: not clear what this is supposed to mean - if we are on Java 8, why parse Java
            // 11 stub files?
            // It would make more sense to parse this if we're e.g. on Java 12.
            if (annotatedJdkVersion.equals("8")) {
                String jdk11Stub = "jdk11.astub";
                parseOneStubFile(this.getClass(), jdk11Stub);
                parseOneStubFile(checker.getClass(), jdk11Stub);
            }
        }

        // 3. Stub files listed in @StubFiles annotation on the checker
        StubFiles stubFilesAnnotation = checker.getClass().getAnnotation(StubFiles.class);
        if (stubFilesAnnotation != null) {
            parseAnnotationFiles(
                    Arrays.asList(stubFilesAnnotation.value()), AnnotationFileType.BUILTIN_STUB);
        }

        // 4. Stub files returned by the `getExtraStubFiles()` method
        parseAnnotationFiles(checker.getExtraStubFiles(), AnnotationFileType.BUILTIN_STUB);

        // 5. Stub files provided via -Astubs command-line option
        String stubsOption = checker.getOption("stubs");
        if (stubsOption != null) {
            parseAnnotationFiles(
                    SystemUtil.PATH_SEPARATOR_SPLITTER.splitToList(stubsOption),
                    AnnotationFileType.COMMAND_LINE_STUB);
        }

        --parsingCount;
        assert parsingCount == 0;
        atypeFactory.clearParsePhaseCache();

        if (stubDebug) {
            System.out.printf(
                    "exited parseStubFiles() for %s%n", atypeFactory.getClass().getSimpleName());
        }
    }

    /**
     * Parse one .astub file.
     *
     * @param checkerClass the location of the resource in the checker.jar file
     * @param stubFileName the basename of the .astub file
     */
    private void parseOneStubFile(Class<?> checkerClass, String stubFileName) {
        BaseTypeChecker checker = atypeFactory.getChecker();
        ProcessingEnvironment processingEnv = atypeFactory.getProcessingEnv();
        URL binURL = checkerClass.getResource(stubFileName + BinaryStubData.BIN_SUFFIX);
        if (binURL != null
                && loadBuiltinBinaryStub(
                        binURL,
                        checkerClass.getResource(stubFileName),
                        checkerClass.getSimpleName() + "/" + stubFileName)) {
            return;
        }
        try (InputStream jdkVersionStubIn = checkerClass.getResourceAsStream(stubFileName)) {
            if (jdkVersionStubIn != null) {
                if (stubDebug) {
                    AnnotationFileParser.stubDebugStatic(
                            processingEnv,
                            "parseOneStubFile(%s, %s): jdkVersionStubIn = %s%n",
                            checkerClass.getSimpleName(),
                            stubFileName,
                            jdkVersionStubIn);
                }
                AnnotationFileParser.parseStubFile(
                        checkerClass.getResource(stubFileName).toString(),
                        jdkVersionStubIn,
                        atypeFactory,
                        processingEnv,
                        annotationFileAnnos,
                        AnnotationFileType.BUILTIN_STUB,
                        this);
            }
        } catch (IOException e) {
            checker.message(
                    Diagnostic.Kind.NOTE,
                    "Could not read annotation resource from "
                            + checkerClass
                            + ": "
                            + stubFileName);
        }
    }

    /** Parses the ajava files passed through the -Aajava command-line option. */
    public void parseAjavaFiles() {
        assert parsingCount == 0;
        ++parsingCount;
        // TODO: Error if this is called more than once?
        SourceChecker checker = atypeFactory.getChecker();
        List<String> ajavaFiles = checker.getStringsOption("ajava", File.pathSeparator);

        parseAnnotationFiles(ajavaFiles, AnnotationFileType.AJAVA);
        --parsingCount;
        assert parsingCount == 0;
        atypeFactory.clearParsePhaseCache();
    }

    /**
     * Parses the ajava file at {@code ajavaPath} assuming {@code root} represents the compilation
     * unit of that file. Uses {@code root} to get information from javac on specific elements of
     * {@code ajavaPath}, enabling storage of more detailed annotation information than with just
     * the ajava file.
     *
     * @param ajavaPath path to an ajava file
     * @param root javac tree for the compilation unit stored in {@code ajavaFile}
     */
    public void parseAjavaFileWithTree(String ajavaPath, CompilationUnitTree root) {
        assert parsingCount == 0;
        ++parsingCount;
        SourceChecker checker = atypeFactory.getChecker();
        ProcessingEnvironment processingEnv = atypeFactory.getProcessingEnv();
        try (InputStream in = new FileInputStream(ajavaPath)) {
            if (stubDebug) {
                AnnotationFileParser.stubDebugStatic(
                        processingEnv,
                        "parseAjavaFileWithTree(%s, %s): checker = %s, in = %s%n",
                        ajavaPath,
                        System.identityHashCode(root),
                        checker.getClass().getSimpleName(),
                        in);
            }
            AnnotationFileParser.parseAjavaFile(
                    ajavaPath, in, root, atypeFactory, processingEnv, annotationFileAnnos, this);
        } catch (IOException e) {
            checker.message(Diagnostic.Kind.NOTE, "Could not read ajava file: " + ajavaPath);
        }

        --parsingCount;
        assert parsingCount == 0;
        atypeFactory.clearParsePhaseCache();
    }

    /**
     * Parses the files in {@code annotationFiles} of the given file type. This includes files
     * listed directly in {@code annotationFiles} and for each listed directory, also includes all
     * files located in that directory (recursively).
     *
     * @param annotationFiles list of files and directories to parse
     * @param fileType the file type of files to parse
     */
    @SuppressWarnings("builder:required.method.not.called" // `allFiles` may contain multiple
    // JarEntryAnnotationFileResource.  Each of those references a zip file entry resource, which
    // itself references a ZipFile resource -- the same ZipFile for multiple zip file entries.
    // Closing any one of the zip file entries will close the ZipFile, which invalidates the
    // other zipfile entries.  Therefore, this code does not close any of them.  This code may
    // leak resources.
    )
    private void parseAnnotationFiles(List<String> annotationFiles, AnnotationFileType fileType) {
        SourceChecker checker = atypeFactory.getChecker();
        ProcessingEnvironment processingEnv = atypeFactory.getProcessingEnv();
        if (stubDebug) {
            AnnotationFileParser.stubDebugStatic(
                    processingEnv, "AFET.parseAnnotationFiles(%s, %s)", annotationFiles, fileType);
        }
        for (String path : annotationFiles) {
            // Special case when running in jtreg.
            String base = System.getProperty("test.src");
            String fullPath = (base == null) ? path : base + "/" + path;

            List<AnnotationFileResource> allFiles =
                    AnnotationFileUtil.allAnnotationFiles(fullPath, fileType);
            if (allFiles != null) {
                for (AnnotationFileResource resource : allFiles) {
                    // See note with the SuppressWarnings on this method for why this is not a
                    // try-with-resources.
                    BufferedInputStream annotationFileStream;
                    try {
                        annotationFileStream = new BufferedInputStream(resource.getInputStream());
                    } catch (IOException e) {
                        checker.message(
                                Diagnostic.Kind.NOTE,
                                "Could not read annotation resource: " + resource.getDescription());
                        continue;
                    }
                    // We use parseStubFile here even for ajava files because at this stage
                    // ajava files are parsed as stub files. The extra annotation data in an
                    // ajava file is parsed when type-checking the ajava file's corresponding
                    // Java file.
                    AnnotationFileParser.parseStubFile(
                            resource.getDescription(),
                            annotationFileStream,
                            atypeFactory,
                            processingEnv,
                            annotationFileAnnos,
                            fileType == AnnotationFileType.AJAVA
                                    ? AnnotationFileType.AJAVA_AS_STUB
                                    : fileType,
                            this);
                }
            } else {
                // We didn't find the files.
                // If the file has a prefix of "checker.jar/" then look for the file in the top
                // level directory of the jar that contains the checker.
                if (path.startsWith("checker.jar/")) {
                    // Note the missing `/` here - this makes sure that `path` starts with `/`.
                    path = path.substring("checker.jar".length());
                }
                boolean issueWarning;
                URL builtinBinURL =
                        fileType == AnnotationFileType.BUILTIN_STUB
                                ? checker.getClass().getResource(path + BinaryStubData.BIN_SUFFIX)
                                : null;
                try (InputStream in = checker.getClass().getResourceAsStream(path)) {
                    if (in != null) {
                        if (builtinBinURL == null
                                || !loadBuiltinBinaryStub(
                                        builtinBinURL,
                                        checker.getClass().getResource(path),
                                        path)) {
                            AnnotationFileParser.parseStubFile(
                                    path,
                                    in,
                                    atypeFactory,
                                    processingEnv,
                                    annotationFileAnnos,
                                    fileType,
                                    this);
                        }
                        issueWarning = false;
                    } else {
                        issueWarning = true;
                    }
                } catch (IOException e) {
                    issueWarning = true;
                    checker.message(
                            Diagnostic.Kind.NOTE, "Could not read annotation resource: " + path);
                }

                if (issueWarning) {
                    // Didn't find the file.  Possibly issue a warning.

                    // When using a compound checker, the target file may be found by the
                    // current checker's parent checkers. Also check this to avoid a false
                    // warning. Currently, only the original checker will try to parse the
                    // target file, the parent checkers are only used to reduce false
                    // warnings.
                    SourceChecker currentChecker = checker;
                    boolean findByParentCheckers = false;
                    while (currentChecker != null) {
                        URL normalResource = currentChecker.getClass().getResource(path);
                        if (normalResource != null) {
                            // If the parent checker supports the stub file, there is no need
                            // for a warning.
                            findByParentCheckers = true;
                            break;
                        }
                        // See whether the stub file is mis-placed and issue a helpful warning.
                        URL topLevelResource = currentChecker.getClass().getResource("/" + path);
                        if (topLevelResource != null) {
                            currentChecker.message(
                                    Diagnostic.Kind.WARNING,
                                    path
                                            + " should be in the same directory as "
                                            + currentChecker.getClass().getSimpleName()
                                            + ".class, but is at the top level of a jar file: "
                                            + topLevelResource);
                            findByParentCheckers = true;
                            break;
                        } else {
                            currentChecker = currentChecker.getParentChecker();
                        }
                    }
                    // If there exists one parent checker that can find this file, don't report
                    // a warning.
                    if (!findByParentCheckers) {
                        File parentPath = new File(path).getParentFile();
                        String parentPathDescription =
                                (parentPath == null
                                        ? "current directory"
                                        : "directory " + parentPath.getAbsolutePath());
                        String msg =
                                checker.getClass().getSimpleName()
                                        + " did not find annotation file or directory "
                                        + path
                                        + " on classpath or within "
                                        + parentPathDescription
                                        + (fullPath.equals(path) ? "" : (" or at " + fullPath));
                        StringJoiner sj = new StringJoiner(System.lineSeparator() + "  ");
                        sj.add(msg);
                        /*
                          sj.add("Classpath:");
                          for (URI uri : new ClassGraph().getClasspathURIs()) {
                              sj.add(uri.toString());
                          }
                        */
                        checker.message(Diagnostic.Kind.WARNING, sj.toString());
                    }
                }
            }
        }
    }

    /**
     * Returns the annotated type for {@code e} containing only annotations explicitly written in an
     * annotation file. Returns {@code null} if {@code e} does not appear in an annotation file.
     *
     * @param e an Element whose type is returned
     * @return an AnnotatedTypeMirror for {@code e} containing only annotations explicitly written
     *     in the annotation file and in the element. Returns {@code null} if {@code element} does
     *     not appear in an annotation file.
     */
    public @Nullable AnnotatedTypeMirror getAnnotatedTypeMirror(Element e) {
        maybeParseEnclosingJdkClass(e);
        AnnotatedTypeMirror type = annotationFileAnnos.atypes.get(e);
        return type == null ? null : type.deepCopy();
    }

    /**
     * Returns the set of declaration annotations for {@code e} containing only annotations
     * explicitly written in an annotation file or the empty set if {@code e} does not appear in an
     * annotation file.
     *
     * @param elt element for which annotations are returned
     * @return an AnnotatedTypeMirror for {@code e} containing only annotations explicitly written
     *     in the annotation file and in the element. {@code null} is returned if {@code element}
     *     does not appear in an annotation file.
     */
    public @Nullable AnnotationMirrorSet getDeclAnnotations(Element elt) {
        if (stubDebug) {
            if (isParsing()) {
                System.out.printf(
                        "AFET.getDeclAnnotations(%s [%s]): isParsing() => true, returning emptySet.%n",
                        elt, elt.getClass());
            } else {
                System.out.printf(
                        "AFET.getDeclAnnotations(%s [%s]): isParsing() => false, proceeding.%n",
                        elt, elt.getClass());
            }
        }

        maybeParseEnclosingJdkClass(elt);
        String eltName = ElementUtils.getQualifiedName(elt);
        AnnotationMirrorSet stored = annotationFileAnnos.declAnnos.get(eltName);
        if (stored != null) {
            return stored;
        }
        // Handle annotations on record declarations.
        boolean canTransferAnnotationsToSameName;
        Element enclosingType; // Do nothing unless this element is a record.
        switch (elt.getKind()) {
            case METHOD:
                // Annotations transfer to zero-arg accessor methods of same name:
                canTransferAnnotationsToSameName =
                        ((ExecutableElement) elt).getParameters().isEmpty();
                enclosingType = elt.getEnclosingElement();
                break;
            case FIELD:
                // Annotations transfer to fields of same name:
                canTransferAnnotationsToSameName = true;
                enclosingType = elt.getEnclosingElement();
                break;
            case PARAMETER:
                // Annotations transfer to compact canonical constructor parameter of same name:
                canTransferAnnotationsToSameName =
                        ElementUtils.isCompactCanonicalRecordConstructor(elt.getEnclosingElement())
                                && elt.getEnclosingElement().getKind() == ElementKind.CONSTRUCTOR;
                enclosingType = elt.getEnclosingElement().getEnclosingElement();
                break;
            default:
                canTransferAnnotationsToSameName = false;
                enclosingType = null;
                break;
        }

        if (canTransferAnnotationsToSameName && ElementUtils.isRecordElement(enclosingType)) {
            AnnotationFileParser.RecordStub recordStub =
                    annotationFileAnnos.records.get(ElementUtils.getQualifiedName(enclosingType));
            if (recordStub != null) {
                RecordComponentStub recordComponentStub =
                        recordStub.componentsByName.get(elt.getSimpleName().toString());
                if (recordComponentStub != null) {
                    return recordComponentStub.getAnnotationsForTarget(elt.getKind());
                }
            }
        }
        return AnnotationMirrorSet.emptySet();
    }

    /**
     * Adds annotations from stub files for the corresponding record components (if the given
     * constructor/method is the canonical constructor or a record accessor). Such transfer is
     * automatically done by javac usually, but not from stubs.
     *
     * @param types a Types instance used for checking type equivalence
     * @param elt a member. This method does nothing if it's not a method or constructor.
     * @param memberType the type corresponding to the element elt; side-effected by this method
     */
    public void injectRecordComponentType(
            Types types, Element elt, AnnotatedExecutableType memberType) {
        if (isParsing()) {
            throw new BugInCF("parsing while calling injectRecordComponentType");
        }

        if (elt.getKind() == ElementKind.METHOD) {
            if (((ExecutableElement) elt).getParameters().isEmpty()) {
                String recordName = ElementUtils.getQualifiedName(elt.getEnclosingElement());
                AnnotationFileParser.RecordStub recordComponentType =
                        annotationFileAnnos.records.get(recordName);
                if (recordComponentType != null) {
                    // If the record component has an annotation in the stub, the component
                    // annotation replaces any from the same hierarchy on the accessor method,
                    // unless there is an accessor in the stubs file (which may or may not have an
                    // annotation in the same hierarchy; the user may want to specify the annotation
                    // or deliberately not annotate the accessor).
                    // We thus only replace the method annotation with the component annotation
                    // if there is no accessor in the stubs file:
                    RecordComponentStub recordComponentStub =
                            recordComponentType.componentsByName.get(
                                    elt.getSimpleName().toString());
                    if (recordComponentStub != null && !recordComponentStub.hasAccessorInStubs()) {
                        memberType
                                .getReturnType()
                                .replaceAnnotations(recordComponentStub.type.getAnnotations());
                    }
                }
            }
        } else if (elt.getKind() == ElementKind.CONSTRUCTOR) {
            if (AnnotationFileUtil.isCanonicalConstructor((ExecutableElement) elt, types)) {
                TypeElement enclosing = (TypeElement) elt.getEnclosingElement();
                AnnotationFileParser.RecordStub recordComponentType =
                        annotationFileAnnos.records.get(ElementUtils.getQualifiedName(enclosing));
                if (recordComponentType != null) {
                    List<AnnotatedTypeMirror> componentsInCanonicalConstructor =
                            recordComponentType.getComponentsInCanonicalConstructor();
                    if (componentsInCanonicalConstructor != null) {
                        for (int i = 0; i < componentsInCanonicalConstructor.size(); i++) {
                            memberType
                                    .getParameterTypes()
                                    .get(i)
                                    .replaceAnnotations(
                                            componentsInCanonicalConstructor
                                                    .get(i)
                                                    .getAnnotations());
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the method type of the most specific fake override for the given element, when used
     * as a member of the given type.
     *
     * @param elt element for which annotations are returned
     * @param receiverType the type of the class that contains member (or a subtype of it)
     * @return the most specific AnnotatedTypeMirror for {@code elt} that is a fake override, or
     *     null if there are no fake overrides
     */
    public @Nullable AnnotatedExecutableType getFakeOverride(
            Element elt, AnnotatedTypeMirror receiverType) {
        if (isParsing()) {
            throw new BugInCF("parsing while calling getFakeOverride");
        }

        if (elt.getKind() != ElementKind.METHOD) {
            return null;
        }

        ExecutableElement method = (ExecutableElement) elt;

        // This is a list of pairs of (where defined, method type) for fake overrides.  The second
        // element of each pair is currently always an AnnotatedExecutableType.
        List<IPair<TypeMirror, AnnotatedTypeMirror>> candidates =
                annotationFileAnnos.fakeOverrides.get(method);

        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        TypeMirror receiverTypeMirror = receiverType.getUnderlyingType();

        // A list of fake receiver types.
        List<TypeMirror> applicableClasses = new ArrayList<>();
        List<TypeMirror> applicableInterfaces = new ArrayList<>();
        for (IPair<TypeMirror, AnnotatedTypeMirror> candidatePair : candidates) {
            TypeMirror fakeLocation = candidatePair.first;
            AnnotatedExecutableType candidate = (AnnotatedExecutableType) candidatePair.second;
            if (atypeFactory.types.isSameType(receiverTypeMirror, fakeLocation)) {
                return candidate;
            } else if (atypeFactory.types.isSubtype(receiverTypeMirror, fakeLocation)) {
                TypeElement fakeElement = TypesUtils.getTypeElement(fakeLocation);
                switch (fakeElement.getKind()) {
                    case CLASS:
                    case ENUM:
                        applicableClasses.add(fakeLocation);
                        break;
                    case INTERFACE:
                    case ANNOTATION_TYPE:
                        applicableInterfaces.add(fakeLocation);
                        break;
                    default:
                        throw new BugInCF(
                                "What type? %s %s %s",
                                fakeElement.getKind(), fakeElement.getClass(), fakeElement);
                }
            }
        }

        if (applicableClasses.isEmpty() && applicableInterfaces.isEmpty()) {
            return null;
        }
        TypeMirror fakeReceiverType =
                TypesUtils.mostSpecific(
                        !applicableClasses.isEmpty() ? applicableClasses : applicableInterfaces,
                        atypeFactory.getProcessingEnv());
        if (fakeReceiverType == null) {
            StringJoiner message = new StringJoiner(System.lineSeparator());
            message.add(
                    String.format(
                            "No most specific fake override found for %s with receiver %s."
                                    + " These fake overrides are applicable:",
                            elt, receiverTypeMirror));
            for (TypeMirror candidate : applicableClasses) {
                message.add("  class candidate: " + candidate);
            }
            for (TypeMirror candidate : applicableInterfaces) {
                message.add("  interface candidate: " + candidate);
            }
            throw new BugInCF(message.toString());
        }

        for (IPair<TypeMirror, AnnotatedTypeMirror> candidatePair : candidates) {
            TypeMirror candidateReceiverType = candidatePair.first;
            if (atypeFactory.types.isSameType(fakeReceiverType, candidateReceiverType)) {
                return (AnnotatedExecutableType) candidatePair.second;
            }
        }

        throw new BugInCF(
                "No match for %s in %s %s %s",
                fakeReceiverType, candidates, applicableClasses, applicableInterfaces);
    }

    //
    // End of public methods, private helper methods follow
    //

    /**
     * Parses the outermost enclosing class of {@code e} if it is in the annotated JDK and it has
     * not already been parsed.
     *
     * @param e element whose outermost enclosing class might be parsed, if it is in the JDK and has
     *     not already been parsed
     */
    private void maybeParseEnclosingJdkClass(Element e) {
        if (stubDebug) {
            System.out.printf(
                    "maybeParseEnclosingJdkClass(%s encloses %s), shouldParseJdk=%s%n",
                    getOutermostEnclosingClass(e), e, shouldParseJdk);
        }

        if (!shouldParseJdk
                || e.getKind() == ElementKind.PACKAGE
                || e.getKind() == ElementKind.MODULE) {
            return;
        }

        String className = getOutermostEnclosingClass(e);
        // `className` can be null if `e` is a package or module element.
        if (className == null || className.isEmpty()) {
            // TODO: maybe investigate other situations where the enclosing class is missing
            //            if (e.getKind() != ElementKind.PACKAGE && e.getKind() !=
            // ElementKind.MODULE) {
            //                atypeFactory.getChecker().reportWarning(e, "unexpected element " + e +
            // " of
            // kind " + e.getKind());
            //            }

            return;
        }

        if (processingClasses.contains(className)) {
            // TODO: some declaration annotations in the enclosing class may still
            //  be missing, we can revisit this part if it's causing issues
            return;
        }

        BinaryStubDataCache cache = getBinaryStubDataCache();
        if (cache != null) {
            // Only load binary JDK stubs from the stub-types AFET (stubTypes), not from the
            // ajava-types AFET (ajavaTypes). Loading from ajavaTypes would happen during
            // AnnotatedTypeFactory.fromElement() before user-supplied stub files are fully
            // parsed, causing the binary's annotations to override the user stubs.
            if (!isStubTypes) {
                return;
            }
            BinaryStubData.ClassRecord cr = cache.data.classes.get(className);
            if (cr != null) {
                // Use a separate set to track binary-processed classes, so we do not interfere
                // with processingClasses which is managed by AnnotationFileParser and throws
                // BugInCF if a class is added twice.
                if (!processedBinaryClasses.add(className)) {
                    // Already processed via binary path; nothing more to do.
                    return;
                }
                remainingJdkStubFiles.remove(className);
                remainingJdkStubFilesJar.remove(className);
                BinaryStubReader.applyClassRecord(
                        cr, className, atypeFactory, this, cache.data, annotationFileAnnos);

                // Also apply binary records for all inner classes in the same file.
                // This mirrors the text parser's behavior of parsing the entire .java file at
                // once (which includes all inner classes). Without this, inner-class annotations
                // (e.g. @InternedDistinct on Symbol.Completer.NULL_COMPLETER) are missed.
                for (BinaryStubData.ClassRecord innerCr : getInnerClassesFromBinary(className)) {
                    String innerName = cache.data.stringPool[innerCr.nameIndex];
                    if (processedBinaryClasses.add(innerName)) {
                        remainingJdkStubFiles.remove(innerName);
                        remainingJdkStubFilesJar.remove(innerName);
                        BinaryStubReader.applyClassRecord(
                                innerCr,
                                innerName,
                                atypeFactory,
                                this,
                                cache.data,
                                annotationFileAnnos);
                    }
                }
                return;
            }
        }

        Path stubPath = remainingJdkStubFiles.remove(className);
        if (stubPath != null) {
            parseJdkStubFile(stubPath);
        } else {
            String jarEntry = remainingJdkStubFilesJar.remove(className);
            if (jarEntry != null) {
                parseJdkJarEntry(jarEntry);
            } else {
                if (stubDebug) {
                    System.out.printf("  not in remaining JDK stub files: %s%n", className);
                }
            }
        }
    }

    /**
     * Returns the fully qualified name of the outermost enclosing class of {@code e} or {@code
     * null} if no such class exists for {@code e}, such as when {@code e} is a package or module
     * element.
     *
     * @param e an element whose outermost enclosing class to return
     * @return the canonical name of the outermost enclosing class of {@code e} or {@code null} if
     *     no class encloses {@code e}
     */
    private @Nullable @CanonicalNameOrEmpty String getOutermostEnclosingClass(Element e) {
        TypeElement enclosingClass = ElementUtils.enclosingTypeElement(e);
        if (enclosingClass == null) {
            return null;
        }
        while (true) {
            Element element = enclosingClass.getEnclosingElement();
            if (element == null || element.getKind() == ElementKind.PACKAGE) {
                break;
            }
            TypeElement t = ElementUtils.enclosingTypeElement(element);
            if (t == null) {
                break;
            }
            enclosingClass = t;
        }
        @CanonicalNameOrEmpty String result = ElementUtils.getQualifiedName(enclosingClass);
        return result;
    }

    /**
     * Parses the stub file in {@code path}.
     *
     * @param path path to file to parse
     */
    private void parseJdkStubFile(Path path) {
        ++parsingCount;
        try (FileInputStream jdkStub = new FileInputStream(path.toFile())) {
            AnnotationFileParser.parseJdkFileAsStub(
                    path.toFile().getName(),
                    jdkStub,
                    atypeFactory,
                    atypeFactory.getProcessingEnv(),
                    annotationFileAnnos,
                    this);
        } catch (IOException e) {
            throw new BugInCF("cannot open the jdk stub file " + path, e);
        } finally {
            --parsingCount;
            if (parsingCount == 0) {
                atypeFactory.clearParsePhaseCache();
            }
        }
    }

    /**
     * Parses the stub file in the given jar entry.
     *
     * @param jarEntryName name of the jar entry to parse
     */
    private void parseJdkJarEntry(String jarEntryName) {
        if (stubDebug) {
            System.out.printf("entered parseJdkJarEntry(%s)%n", jarEntryName);
        }

        JarURLConnection connection = getJarURLConnectionToJdk();
        ++parsingCount;
        try (JarFile jarFile = connection.getJarFile()) {
            try (InputStream jdkStub = jarFile.getInputStream(jarFile.getJarEntry(jarEntryName))) {
                AnnotationFileParser.parseJdkFileAsStub(
                        jarEntryName,
                        jdkStub,
                        atypeFactory,
                        atypeFactory.getProcessingEnv(),
                        annotationFileAnnos,
                        this);
            } catch (IOException e) {
                throw new BugInCF("cannot open the jdk stub file " + jarEntryName, e);
            }
        } catch (IOException e) {
            throw new BugInCF("cannot open the Jar file " + connection.getEntryName(), e);
        } catch (BugInCF e) {
            throw new BugInCF("Exception while parsing " + jarEntryName + ": " + e.getMessage(), e);
        } finally {
            --parsingCount;
            if (parsingCount == 0) {
                atypeFactory.clearParsePhaseCache();
            }
        }

        if (stubDebug) {
            System.out.printf("exited parseJdkJarEntry(%s)%n", jarEntryName);
        }
    }

    /**
     * Returns a JarURLConnection to "/jdk*".
     *
     * @return a JarURLConnection to "/jdk*"
     */
    private JarURLConnection getJarURLConnectionToJdk() {
        URL resourceURL = atypeFactory.getClass().getResource("/" + ANNOTATED_JDK_PATH);
        JarURLConnection connection;
        try {
            connection = (JarURLConnection) resourceURL.openConnection();

            // disable caching / connection sharing of the low level URLConnection to the Jarfile
            connection.setDefaultUseCaches(false);
            connection.setUseCaches(false);

            connection.connect();
        } catch (IOException e) {
            throw new BugInCF(
                    "cannot open a connection to the Jar file " + resourceURL.getFile(), e);
        }
        return connection;
    }

    /**
     * Walk through the JDK directory and create a mapping, {@link #remainingJdkStubFiles}, from
     * file name to the class contained with in it. Also, parses all package-info.java files.
     */
    private void prepJdkStubs() {
        if (!shouldParseJdk) {
            return;
        }
        URL resourceURL = atypeFactory.getClass().getResource("/" + ANNOTATED_JDK_PATH);
        URL binURL =
                atypeFactory
                        .getClass()
                        .getResource("/" + ANNOTATED_JDK_PATH + "/" + BinaryStubData.FILENAME);
        boolean binaryLoaded = false;
        if (binURL != null) {
            try {
                BinaryStubDataCache cache = getBinaryStubDataCache();
                if (cache == null) {
                    // The BinaryStubData contains no javac objects, so it is cached per JVM;
                    // only the BinaryStubDataCache wrapper is per-compilation.
                    cache = new BinaryStubDataCache(loadBinaryStubData(binURL));
                    setBinaryStubDataCache(cache);
                }
                binaryLoaded = true;
                // Apply package and module annotations eagerly, as they are global rather than
                // per-class.
                BinaryStubReader.applyPackageAndModuleRecords(
                        cache.data,
                        atypeFactory,
                        this,
                        annotationFileAnnos,
                        /* fromLazyJdk= */ true);
            } catch (IOException e) {
                atypeFactory
                        .getChecker()
                        .message(
                                Diagnostic.Kind.NOTE,
                                "Could not read "
                                        + BinaryStubData.FILENAME
                                        + ", falling back to JavaParser. Error: "
                                        + e.getMessage());
            }
        } else {
            atypeFactory
                    .getChecker()
                    .message(
                            Diagnostic.Kind.NOTE,
                            BinaryStubData.FILENAME + " not found, falling back to JavaParser.");
        }

        if (stubDebug) {
            System.out.printf(
                    "Loading JDK from class %s and url: %s%n",
                    atypeFactory.getClass(), resourceURL);
        }
        if (resourceURL == null) {
            if (permitMissingJdk) {
                return;
            }
            throw new BugInCF(
                    "JDK not found for type factory " + atypeFactory.getClass().getSimpleName());
        } else if (resourceURL.getProtocol().contentEquals("jar")) {
            prepJdkFromJar(binaryLoaded);
        } else if (resourceURL.getProtocol().contentEquals("file")) {
            prepJdkFromFile(resourceURL, binaryLoaded);
        } else {
            if (permitMissingJdk) {
                return;
            }
            throw new BugInCF(
                    "JDK not found in "
                            + resourceURL
                            + ". Unsupported protocol: "
                            + resourceURL.getProtocol());
        }

        if (binaryLoaded
                && isStubTypes
                && atypeFactory.getChecker().hasOption("binaryStubDiffCheck")) {
            // Test-only mode: compare, for every class in the binary stub, the annotations the
            // binary path produces against the annotations the text parser produces, and report
            // any disagreement as an error. See BinaryStubDiffChecker.
            //
            // Runs once per factory (prepJdkStubs, and so this method, runs exactly once per
            // AnnotationFileElementTypes instance), not once per compilation: a compound checker
            // like Nullness has several sub-checker factories (e.g. NullnessNoInit, KeyFor), each
            // supporting a different, mostly-disjoint set of qualifiers. addAnnotation silently
            // drops an annotation a factory's own atypeFactory does not support -- on both the
            // text and binary sides equally -- so comparing under only one factory (formerly
            // gated by BinaryStubDataCache.diffCheckDone, "whichever factory gets here first")
            // made the check blind to every qualifier that factory does not support. If that
            // factory happens to be, say, KeyFor, which does not support @Nullable at all, the
            // entire annotated JDK's nullness annotations go uncompared on both sides -- exactly
            // how a dropped @Nullable on Class.getMethod's varargs array (a real bug, found only
            // by testing the checker directly, not by this comparison) went unreported.
            BinaryStubDataCache cache = getBinaryStubDataCache();
            if (cache != null) {
                BinaryStubDiffChecker.run(this, cache, atypeFactory);
            }
        }
    }

    /**
     * Walk through the JDK directory and create a mapping, {@link #remainingJdkStubFiles}, from
     * file name to the class contained with in it. Also, parses all package-info.java files.
     *
     * @param jdkDirectory the URL pointing to the JDK directory
     * @param binaryLoaded {@code true} if package and module annotations were successfully loaded
     *     from the binary stub file
     */
    private void prepJdkFromFile(URL jdkDirectory, boolean binaryLoaded) {
        BinaryStubDataCache cache = getBinaryStubDataCache();
        // The directory walk is shareable across the factories of a compilation when the binary
        // stub is loaded: no per-factory parsing (of package-info files) happens during the walk
        // then, so the walk's only output is the class-to-path map.
        boolean shareWalk = binaryLoaded && !parseAllJdkFiles && cache != null;
        if (shareWalk && cache.jdkStubPathsByClass != null) {
            // Another factory in this compilation already walked the JDK directory.
            remainingJdkStubFiles.putAll(cache.jdkStubPathsByClass);
            if (stubDebug) {
                printRemainingJdkStubFilesDebug(jdkDirectory);
            }
            return;
        }

        Path root;
        try {
            root = Paths.get(jdkDirectory.toURI());
        } catch (URISyntaxException e) {
            throw new BugInCF("Cannot parse URL: " + jdkDirectory.toString(), e);
        }

        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> paths =
                    walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                            .collect(Collectors.toList());
            paths.sort(Path::compareTo);
            for (Path path : paths) {
                if (path.getFileName().toString().equals("package-info.java")) {
                    if (!binaryLoaded) {
                        // When the binary stub is loaded, package annotations come from its
                        // package records instead.
                        parseJdkStubFile(path);
                    }
                    continue;
                }
                if (path.getFileName().toString().equals("module-info.java")) {
                    // JavaParser can't parse module-info files, so skip them.
                    continue;
                }
                if (parseAllJdkFiles) {
                    parseJdkStubFile(path);
                    continue;
                }
                Path relativePath = root.relativize(path);
                // The number 4 is to strip off "/src/<module>/share/classes".
                Path savepath = relativePath.subpath(4, relativePath.getNameCount());
                String savepathString = savepath.toString();
                // The number 5 is to remove trailing ".java".
                String savepathWithoutExtension =
                        savepathString.substring(0, savepathString.length() - 5);
                String fqName = savepathWithoutExtension.replace(File.separatorChar, '.');
                remainingJdkStubFiles.put(fqName, path);
            }
            if (shareWalk) {
                cache.jdkStubPathsByClass = new HashMap<>(remainingJdkStubFiles);
            }
            if (stubDebug) {
                printRemainingJdkStubFilesDebug(jdkDirectory);
            }
        } catch (IOException e) {
            throw new BugInCF("prepJdkFromFile(" + jdkDirectory + ")", e);
        }
    }

    /**
     * Text-parses the JDK stub source for {@code className} into {@code target}, if a stub source
     * for it is known. Used only by {@link BinaryStubDiffChecker} to obtain the text parser's view
     * of a class for comparison against the binary path.
     *
     * <p>Looks up the class's source location in the {@link BinaryStubDataCache}'s {@link
     * BinaryStubDataCache#jdkStubPathsByClass}/{@link BinaryStubDataCache#jdkJarEntriesByClass}
     * snapshots, not in {@link #remainingJdkStubFiles}/{@link #remainingJdkStubFilesJar}: those two
     * are consumed (entries removed) by {@link #maybeParseEnclosingJdkClass} as a side effect of
     * ordinary lazy resolution, including resolution triggered by the diff check's own comparisons
     * of an <em>earlier</em> class in the same run (e.g., a field or parameter whose type is {@code
     * java.lang.Class} resolves {@code Class} lazily, removing it from these maps before the outer
     * loop ever reaches {@code Class} as its own top-level entry). Looking it up in the cache's
     * snapshots instead means every class with a text stub source is compared exactly once,
     * regardless of what else has already been resolved.
     *
     * @param className fully-qualified name of an outermost class
     * @param target the annotation container to parse into
     * @return true if a stub source for the class was found and parsed
     */
    boolean parseJdkSourceInto(String className, AnnotationFileAnnotations target) {
        BinaryStubDataCache cache = getBinaryStubDataCache();
        Map<String, Path> stubPathsByClass =
                cache != null && cache.jdkStubPathsByClass != null
                        ? cache.jdkStubPathsByClass
                        : remainingJdkStubFiles;
        Path path = stubPathsByClass.get(className);
        if (path != null) {
            try (InputStream in = new FileInputStream(path.toFile())) {
                parseJdkStreamInto(path.toFile().getName(), in, target);
            } catch (IOException e) {
                throw new BugInCF("cannot open the jdk stub file " + path, e);
            }
            return true;
        }
        Map<String, String> jarEntriesByClass =
                cache != null && cache.jdkJarEntriesByClass != null
                        ? cache.jdkJarEntriesByClass
                        : remainingJdkStubFilesJar;
        String jarEntryName = jarEntriesByClass.get(className);
        if (jarEntryName != null) {
            JarURLConnection connection = getJarURLConnectionToJdk();
            try (JarFile jarFile = connection.getJarFile();
                    InputStream in = jarFile.getInputStream(jarFile.getJarEntry(jarEntryName))) {
                parseJdkStreamInto(jarEntryName, in, target);
            } catch (IOException e) {
                throw new BugInCF("cannot open the jdk stub file " + jarEntryName, e);
            }
            return true;
        }
        return false;
    }

    /**
     * Text-parses one JDK stub source stream into {@code target}, with the usual parse-phase
     * bookkeeping ({@link #parsingCount}, parse-phase cache clearing).
     *
     * @param name the stub source's name, for diagnostics
     * @param in the stream to parse
     * @param target the annotation container to parse into
     */
    private void parseJdkStreamInto(String name, InputStream in, AnnotationFileAnnotations target) {
        ++parsingCount;
        try {
            AnnotationFileParser.parseJdkFileAsStub(
                    name, in, atypeFactory, atypeFactory.getProcessingEnv(), target, this);
        } finally {
            --parsingCount;
            if (parsingCount == 0) {
                atypeFactory.clearParsePhaseCache();
            }
        }
    }

    /**
     * Prints the contents of {@link #remainingJdkStubFiles} for debugging (option {@code
     * -AstubDebug}).
     *
     * @param jdkDirectory the URL the JDK stub files were found under
     */
    private void printRemainingJdkStubFilesDebug(URL jdkDirectory) {
        System.out.printf(
                "Contents of remainingJdkStubFiles for %s from %s:%n",
                atypeFactory.getClass().getSimpleName(), jdkDirectory);
        printSortedIndented(remainingJdkStubFiles.keySet());
        System.out.printf(
                "End of remainingJdkStubFiles for %s from %s.%n",
                atypeFactory.getClass().getSimpleName(), jdkDirectory);
    }

    /**
     * Prints the contents of {@link #remainingJdkStubFilesJar} for debugging (option {@code
     * -AstubDebug}). Used on the fast path of {@link #prepJdkFromJar} where the map was copied from
     * the shared per-compilation cache rather than enumerated from the jar.
     */
    private void printRemainingJdkStubFilesJarDebug() {
        String factoryClass = atypeFactory.getClass().getSimpleName();
        System.out.printf(
                "Contents of remainingJdkStubFilesJar for %s (from shared per-compilation"
                        + " cache):%n",
                factoryClass);
        printSortedIndented(remainingJdkStubFilesJar.keySet());
        System.out.printf("End of remainingJdkStubFilesJar for %s.%n", factoryClass);
    }

    /**
     * Walk through the JDK directory and create a mapping, {@link #remainingJdkStubFilesJar}, from
     * file name to the class contained with in it. Also, parses all package-info.java files.
     *
     * @param binaryLoaded {@code true} if package and module annotations were successfully loaded
     *     from the binary stub file
     */
    private void prepJdkFromJar(boolean binaryLoaded) {
        BinaryStubDataCache cache = getBinaryStubDataCache();
        // The jar enumeration is shareable across the factories of a compilation when the binary
        // stub is loaded: no per-factory parsing (of package-info files) happens during the
        // enumeration then, so its only output is the class-to-jar-entry map.
        boolean shareScan = binaryLoaded && !parseAllJdkFiles && cache != null;
        if (shareScan && cache.jdkJarEntriesByClass != null) {
            // Another factory in this compilation already enumerated the jar.
            remainingJdkStubFilesJar.putAll(cache.jdkJarEntriesByClass);
            if (stubDebug) {
                printRemainingJdkStubFilesJarDebug();
            }
            return;
        }

        JarURLConnection connection = getJarURLConnectionToJdk();

        try (JarFile jarFile = connection.getJarFile()) {
            ArrayList<JarEntry> entries = CollectionsPlume.makeArrayList(jarFile.entries());
            entries.sort(Comparator.comparing(Object::toString));
            for (JarEntry jarEntry : entries) {
                // filter out directories and non-Java files
                if (jarEntry.isDirectory()) {
                    continue;
                }
                String jarEntryName = jarEntry.getName();
                if (!(jarEntryName.startsWith(ANNOTATED_JDK_PATH) && jarEntryName.endsWith(".java"))
                        // JavaParser can't parse module-info files, so skip them.
                        || jarEntryName.endsWith("module-info.java")) {
                    continue;
                }
                if (jarEntryName.endsWith("package-info.java")) {
                    if (parseAllJdkFiles || !binaryLoaded) {
                        // When the binary stub is loaded, package annotations come from its
                        // package records instead.
                        parseJdkJarEntry(jarEntryName);
                    }
                    continue;
                }
                if (parseAllJdkFiles) {
                    parseJdkJarEntry(jarEntryName);
                    continue;
                }
                int index = jarEntryName.indexOf("/share/classes/") + "/share/classes/".length();
                // "-5" is to remove ".java" from end of file name
                String fqClassName =
                        jarEntryName.substring(index, jarEntryName.length() - 5).replace('/', '.');
                remainingJdkStubFilesJar.put(fqClassName, jarEntryName);
            }
            if (shareScan) {
                cache.jdkJarEntriesByClass = new HashMap<>(remainingJdkStubFilesJar);
            }
            if (stubDebug) {
                String factoryClass = atypeFactory.getClass().getSimpleName().toString();
                String jarFileURL = connection.getJarFileURL().toString();
                System.out.printf(
                        "Contents of remainingJdkStubFilesJar for %s from %s:%n",
                        factoryClass, jarFileURL);
                printSortedIndented(remainingJdkStubFilesJar.keySet());
                System.out.printf(
                        "End of remainingJdkStubFilesJar for %s from %s.%n",
                        factoryClass, jarFileURL);

                System.out.printf("Contents of %s:%n", jarFileURL);
                assert jarFileURL.startsWith("file:");
                ProcessBuilder pb =
                        new ProcessBuilder(
                                "/bin/sh",
                                "-c",
                                "jar tf '" + jarFileURL.substring(5) + "' | LC_ALL=C sort");
                pb.redirectOutput(Redirect.INHERIT);
                pb.redirectError(Redirect.INHERIT);
                // Process implements AutoCloseable in JDK 26, but we compile against older JDKs
                // where close() is not available.
                @SuppressWarnings({
                    "resourceleak:required.method.not.called",
                    "resourceleak:unneeded.suppression"
                })
                Process p = pb.start();
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    // do nothing
                }
                System.out.flush();
                SystemPlume.sleep(1);
                System.out.printf("End of %s.%n", jarFileURL);
            }
        } catch (IOException e) {
            throw new BugInCF("Cannot open the jar file " + connection.getJarFileURL(), e);
        }
    }

    /**
     * This method is invoked each time before {@link AnnotationFileParser} processes a top-level
     * type.
     *
     * @param typeName the fully qualified name of the top-level type
     */
    void preProcessTopLevelType(String typeName) {
        boolean added = processingClasses.add(typeName);
        if (!added) {
            throw new BugInCF(
                    "Trying to process type " + typeName + " which is already being processed.");
        }
    }

    /**
     * This method is invoked each time after {@link AnnotationFileParser} processes a top-level
     * type.
     *
     * @param typeName the fully qualified name of the top-level type
     */
    void postProcessTopLevelType(String typeName) {
        boolean removed = processingClasses.remove(typeName);
        if (!removed) {
            throw new BugInCF("Cannot find the processing record for type " + typeName);
        }
    }

    /**
     * Print the strings, in order, each on its own line, indented by two spaces.
     *
     * @param strings a collection of strings
     */
    private void printSortedIndented(Collection<String> strings) {
        List<String> stringList = new ArrayList<>(strings);
        stringList.sort(String::compareTo);
        for (String s : stringList) {
            System.out.printf("  %s%n", s);
        }
    }
}

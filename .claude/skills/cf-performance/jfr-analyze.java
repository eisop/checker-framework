// Single-file JFR analyzer for EISOP Checker Framework profiling.
//
// Run with the JDK source launcher (no compile step needed):
//
//   java .claude/skills/cf-performance/jfr-analyze.java <mode> [args] <file.jfr> [more.jfr ...]
//
// Pass *every* per-PID worker file from one record-jfr.sh run; record-jfr.sh
// writes filename-<pid>.jfr for each JVM and the type-checking worker is the
// largest one, but aggregating them all is harmless (launcher/daemon files
// contribute almost no ExecutionSamples).
//
// Modes:
//   top    <file...>                       Top self-time (leaf), total-time, and
//                                          allocation-by-class tables. The default.
//   self   <leafSubstr>  <pkg> <file...>   Attribute a hot JDK leaf (e.g.
//                                          java.util.HashMap.getNode) to the nearest
//                                          frame whose class name starts with <pkg>.
//   alloc  <classSubstr> <pkg> <file...>   Attribute allocations of a class (e.g.
//                                          "[Ljava.lang.Object;" or "ArrayList$Itr")
//                                          to the nearest <pkg> frame.
//   inclusive <pkg> <file...>              Inclusive (total) time for frames under
//                                          <pkg> (e.g. org.checkerframework): the
//                                          fraction of samples whose stack contains
//                                          each method. Good for finding which
//                                          high-level operation dominates.
//   under  <ctxSubstr> <file...>           Leaf self-time histogram restricted to
//                                          samples whose stack contains <ctxSubstr>
//                                          (e.g. performFlowAnalysis): where does a
//                                          given subsystem spend its self-time.
//   cooccur <target> <ctx> <file...>       Of samples whose stack contains <target>,
//                                          what fraction also contains <ctx>. Attributes
//                                          a cost to a calling context (e.g. is
//                                          getDeclAnnotation mostly under stub parsing?).
//   phase  <file...>                       High-level wall-clock breakdown: on-CPU Java
//                                          time bucketed by CF subsystem (innermost
//                                          marker), plus GC pause time and the native/
//                                          idle share. Answers "where does checkNullness
//                                          spend wall-clock". Bucket markers are
//                                          CF-specific; see phaseBucket().
//
// WHY THIS EXISTS: on JDK 25 the stock `jfr print` and `jfr view` commands crash
// with a StringIndexOutOfBoundsException in ValueFormatter.formatMethod /
// PrettyWriter.formatMethod on some mangled method signatures, so the entire
// documented `jfr view hot-methods` pipeline is unusable. Reading the recording
// directly via jdk.jfr.consumer.RecordingFile avoids that formatter.
//
// Self-time is taken from jdk.ExecutionSample ONLY. jdk.NativeMethodSample
// catches threads parked in native (sun.nio.ch.EPoll.wait and friends on the
// Gradle worker's messaging thread) and is pure idle noise for CF profiling.

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class jfr_analyze {

    static String m(RecordedFrame f) {
        RecordedMethod me = f.getMethod();
        if (me == null) {
            return "<null>";
        }
        return me.getType().getName() + "." + me.getName();
    }

    static boolean isAlloc(String type) {
        return type.equals("jdk.ObjectAllocationInNewTLAB")
                || type.equals("jdk.ObjectAllocationOutsideTLAB");
    }

    static String allocClass(RecordedEvent e) {
        try {
            return e.getClass("objectClass").getName();
        } catch (Exception ex) {
            return null;
        }
    }

    static void printTable(String title, Map<String, Long> map, long denom, int limit) {
        System.out.println("\n=== " + title + " ===");
        long d = denom == 0 ? 1 : denom;
        map.entrySet().stream()
                .sorted((x, y) -> Long.compare(y.getValue(), x.getValue()))
                .limit(limit)
                .forEach(en -> System.out.printf(
                        "%6.2f%%  %7d  %s%n", 100.0 * en.getValue() / d, en.getValue(), en.getKey()));
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println(
                    "usage: java jfr-analyze.java"
                            + " <top|self|alloc|inclusive|under|cooccur|phase> [args] <file.jfr...>");
            System.exit(2);
        }
        String mode = args[0];
        switch (mode) {
            case "top" -> top(args, 1);
            case "self", "alloc" -> nearest(mode, args[1], args[2], args, 3);
            case "inclusive" -> inclusive(args[1], args, 2);
            case "under" -> under(args[1], args, 2);
            case "cooccur" -> cooccur(args[1], args[2], args, 3);
            case "phase" -> phase(args, 1);
            // No subcommand: treat all args as files, run "top".
            default -> top(args, 0);
        }
    }

    /** Inclusive (total) time for frames under a package prefix. */
    static void inclusive(String pkg, String[] args, int start) throws Exception {
        Map<String, Long> total = new HashMap<>();
        long n = 0;
        for (int i = start; i < args.length; i++) {
            try (RecordingFile rf = new RecordingFile(Paths.get(args[i]))) {
                while (rf.hasMoreEvents()) {
                    RecordedEvent e = rf.readEvent();
                    if (!e.getEventType().getName().equals("jdk.ExecutionSample")) {
                        continue;
                    }
                    RecordedStackTrace st = e.getStackTrace();
                    if (st == null || st.getFrames().isEmpty()) {
                        continue;
                    }
                    n++;
                    Set<String> seen = new HashSet<>();
                    for (RecordedFrame f : st.getFrames()) {
                        String name = m(f);
                        if (name.startsWith(pkg) && seen.add(name)) {
                            total.merge(name, 1L, Long::sum);
                        }
                    }
                }
            }
        }
        System.out.println("ExecutionSamples=" + n + "  (inclusive % = fraction whose stack contains the method)");
        printTable("INCLUSIVE TIME under " + pkg, total, n, 55);
    }

    /** Leaf self-time histogram restricted to samples whose stack contains ctx. */
    static void under(String ctx, String[] args, int start) throws Exception {
        Map<String, Long> self = new HashMap<>();
        long n = 0;
        for (int i = start; i < args.length; i++) {
            try (RecordingFile rf = new RecordingFile(Paths.get(args[i]))) {
                while (rf.hasMoreEvents()) {
                    RecordedEvent e = rf.readEvent();
                    if (!e.getEventType().getName().equals("jdk.ExecutionSample")) {
                        continue;
                    }
                    RecordedStackTrace st = e.getStackTrace();
                    if (st == null || st.getFrames().isEmpty()) {
                        continue;
                    }
                    boolean has = false;
                    for (RecordedFrame f : st.getFrames()) {
                        if (m(f).contains(ctx)) {
                            has = true;
                            break;
                        }
                    }
                    if (!has) {
                        continue;
                    }
                    n++;
                    self.merge(m(st.getFrames().get(0)), 1L, Long::sum);
                }
            }
        }
        System.out.println("samples under '" + ctx + "' = " + n);
        printTable("SELF-TIME under " + ctx, self, n, 30);
    }

    /** Of samples whose stack contains target, the fraction that also contains ctx. */
    static void cooccur(String target, String ctx, String[] args, int start) throws Exception {
        long n = 0, tgt = 0, both = 0;
        for (int i = start; i < args.length; i++) {
            try (RecordingFile rf = new RecordingFile(Paths.get(args[i]))) {
                while (rf.hasMoreEvents()) {
                    RecordedEvent e = rf.readEvent();
                    if (!e.getEventType().getName().equals("jdk.ExecutionSample")) {
                        continue;
                    }
                    RecordedStackTrace st = e.getStackTrace();
                    if (st == null) {
                        continue;
                    }
                    n++;
                    boolean ht = false, hc = false;
                    for (RecordedFrame f : st.getFrames()) {
                        String name = m(f);
                        if (name.contains(target)) {
                            ht = true;
                        }
                        if (name.contains(ctx)) {
                            hc = true;
                        }
                    }
                    if (ht) {
                        tgt++;
                        if (hc) {
                            both++;
                        }
                    }
                }
            }
        }
        System.out.printf(
                "samples=%d  '%s' in %d (%.2f%%)  of those, '%s' also present in %d (%.1f%% of target)%n",
                n, target, tgt, 100.0 * tgt / (n == 0 ? 1 : n), ctx, both,
                100.0 * both / (tgt == 0 ? 1 : tgt));
    }

    static void top(String[] args, int start) throws Exception {
        Map<String, Long> self = new HashMap<>();
        Map<String, Long> total = new HashMap<>();
        Map<String, Long> allocCount = new HashMap<>();
        Map<String, Long> allocBytes = new HashMap<>();
        long execTotal = 0, allocTotal = 0;
        for (int i = start; i < args.length; i++) {
            try (RecordingFile rf = new RecordingFile(Paths.get(args[i]))) {
                while (rf.hasMoreEvents()) {
                    RecordedEvent e = rf.readEvent();
                    String type = e.getEventType().getName();
                    if (type.equals("jdk.ExecutionSample")) {
                        RecordedStackTrace st = e.getStackTrace();
                        if (st == null || st.getFrames().isEmpty()) {
                            continue;
                        }
                        List<RecordedFrame> fr = st.getFrames();
                        execTotal++;
                        self.merge(m(fr.get(0)), 1L, Long::sum);
                        Set<String> seen = new HashSet<>();
                        for (RecordedFrame f : fr) {
                            String name = m(f);
                            if (seen.add(name)) {
                                total.merge(name, 1L, Long::sum);
                            }
                        }
                    } else if (isAlloc(type)) {
                        String cls = allocClass(e);
                        if (cls == null) {
                            continue;
                        }
                        long sz = 0;
                        try {
                            sz = e.getLong("allocationSize");
                        } catch (Exception ex) {
                            try {
                                sz = e.getLong("tlabSize");
                            } catch (Exception ex2) {
                                // leave 0
                            }
                        }
                        allocCount.merge(cls, 1L, Long::sum);
                        allocBytes.merge(cls, sz, Long::sum);
                        allocTotal++;
                    }
                }
            }
        }
        System.out.println(
                "Files: " + (args.length - start) + "   ExecutionSamples: " + execTotal
                        + "   TLAB events: " + allocTotal);
        printTable("TOP SELF-TIME (leaf frame, ExecutionSample)", self, execTotal, 45);
        printTable("TOP TOTAL-TIME (method appears anywhere in stack)", total, execTotal, 40);
        printTable("ALLOCATION BY CLASS (TLAB event count)", allocCount, allocTotal, 35);
    }

    static void nearest(String mode, String target, String pkg, String[] args, int start)
            throws Exception {
        Map<String, Long> tally = new HashMap<>();
        long matched = 0;
        for (int i = start; i < args.length; i++) {
            try (RecordingFile rf = new RecordingFile(Paths.get(args[i]))) {
                while (rf.hasMoreEvents()) {
                    RecordedEvent e = rf.readEvent();
                    String type = e.getEventType().getName();
                    boolean want;
                    if (mode.equals("self")) {
                        want = type.equals("jdk.ExecutionSample");
                    } else {
                        want = isAlloc(type);
                    }
                    if (!want) {
                        continue;
                    }
                    RecordedStackTrace st = e.getStackTrace();
                    if (st == null || st.getFrames().isEmpty()) {
                        continue;
                    }
                    List<RecordedFrame> fr = st.getFrames();
                    if (mode.equals("self")) {
                        if (!m(fr.get(0)).contains(target)) {
                            continue;
                        }
                    } else {
                        String cls = allocClass(e);
                        if (cls == null || !cls.contains(target)) {
                            continue;
                        }
                    }
                    matched++;
                    for (RecordedFrame f : fr) {
                        String name = m(f);
                        if (name.startsWith(pkg)) {
                            tally.merge(name, 1L, Long::sum);
                            break;
                        }
                    }
                }
            }
        }
        System.out.println(mode + " '" + target + "' nearest '" + pkg + "'  matched=" + matched);
        printTable("NEAREST " + pkg + " FRAME", tally, matched, 30);
    }

    /**
     * Classifies one stack into a high-level CF subsystem by the INNERMOST (leaf-ward) marker it
     * hits, giving a mutually-exclusive split: the type computation that dataflow/the visitor
     * trigger is attributed to the type factory, not to dataflow/the visitor. Markers are
     * CF-specific and approximate; adjust as the code moves.
     */
    static String phaseBucket(List<RecordedFrame> frames) {
        for (RecordedFrame f : frames) {
            String s = m(f);
            if (s.contains("AnnotationFileParser")
                    || s.contains("AnnotationFileElementTypes")
                    || s.contains("com.github.javaparser")
                    || s.contains("JavaParserUtil")) {
                return "Stub/JDK annotation loading";
            }
            if (s.startsWith("org.checkerframework.dataflow.")
                    || s.startsWith("org.checkerframework.framework.flow.")) {
                return "Dataflow (CFG+fixpoint+transfer)";
            }
            if (s.contains("QualifierDefaults")
                    || s.contains("DefaultQualifierForUse")
                    || s.contains(".typeannotator.")
                    || s.contains("AnnotatedTypeCopier")
                    || s.contains("AnnotatedTypeScanner")
                    || s.contains("SupertypeFinder")
                    || s.contains("TypeVariableSubstitutor")
                    || s.contains("dependenttypes")
                    || s.contains("ElementAnnotationApplier")
                    || s.contains(".util.element.")
                    || s.contains("TypesIntoElements")
                    || s.contains("AnnotatedTypeFactory")
                    || s.contains("AnnotatedTypeMirror")
                    || s.contains("org.checkerframework.framework.util.AnnotatedTypes")
                    || s.contains("AsSuperVisitor")
                    || s.contains("typeinference")) {
                return "Type factory (getAnnotatedType/defaults/supertypes)";
            }
            if (s.contains("BaseTypeVisitor")
                    || s.contains("SourceVisitor")
                    || s.contains("Visitor")
                    || s.contains("BaseTypeValidator")
                    || s.contains("commonAssignmentCheck")) {
                return "Visitor checks";
            }
            if (s.startsWith("org.checkerframework.")) {
                return "Other CF (utils/setup/source)";
            }
            if (s.startsWith("com.sun.tools.javac.") || s.startsWith("com.sun.source.")) {
                return "javac internals (often CF-triggered: complete/decode/walk)";
            }
        }
        return "JDK/runtime (uncategorized)";
    }

    /** High-level wall-clock breakdown: on-CPU Java by subsystem, plus GC pause and native/idle. */
    static void phase(String[] args, int start) throws Exception {
        Map<String, Long> bucket = new HashMap<>();
        long exec = 0, nativeIdle = 0, nativeReal = 0, gcPauseNanos = 0;
        int gcCount = 0;
        for (int i = start; i < args.length; i++) {
            try (RecordingFile rf = new RecordingFile(Paths.get(args[i]))) {
                while (rf.hasMoreEvents()) {
                    RecordedEvent e = rf.readEvent();
                    String type = e.getEventType().getName();
                    if (type.equals("jdk.ExecutionSample")) {
                        RecordedStackTrace st = e.getStackTrace();
                        if (st == null || st.getFrames().isEmpty()) {
                            continue;
                        }
                        exec++;
                        bucket.merge(phaseBucket(st.getFrames()), 1L, Long::sum);
                    } else if (type.equals("jdk.NativeMethodSample")) {
                        RecordedStackTrace st = e.getStackTrace();
                        String leaf = st == null || st.getFrames().isEmpty() ? "" : m(st.getFrames().get(0));
                        if (leaf.contains("EPoll") || leaf.contains("Net.poll") || leaf.contains("park")) {
                            nativeIdle++;
                        } else {
                            nativeReal++;
                        }
                    } else if (type.equals("jdk.GarbageCollection")) {
                        gcCount++;
                        try {
                            gcPauseNanos += e.getDuration("sumOfPauses").toNanos();
                        } catch (Exception ex) {
                            try {
                                gcPauseNanos += e.getDuration().toNanos();
                            } catch (Exception ex2) {
                                // leave unchanged
                            }
                        }
                    }
                }
            }
        }
        // ExecutionSample period is 10 ms (the .jfc floor), so #samples * 10ms approximates on-CPU time.
        System.out.printf(
                "On-CPU Java: %d ExecutionSamples (~%.1f s at 10ms).  GC: %d collections, %.0f ms summed pause.%n",
                exec, exec * 0.010, gcCount, gcPauseNanos / 1e6);
        System.out.printf(
                "Native samples: %d idle (EPoll/park -- not work) + %d real I/O.%n", nativeIdle, nativeReal);
        printTable("ON-CPU JAVA BY SUBSYSTEM (innermost marker, mutually exclusive)", bucket, exec, 15);
    }
}

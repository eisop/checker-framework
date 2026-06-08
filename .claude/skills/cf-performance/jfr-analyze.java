// Single-file JFR analyzer for EISOP Checker Framework profiling.
//
// Run with the JDK source launcher (no compile step needed):
//
//   java checker/bin-devel/jfr-analyze.java <mode> [args] <file.jfr> [more.jfr ...]
//
// Pass *every* per-PID worker file from one record-jfr.sh run; record-jfr.sh
// writes filename-<pid>.jfr for each JVM and the type-checking worker is the
// largest one, but aggregating them all is harmless (launcher/daemon files
// contribute almost no ExecutionSamples).
//
// Modes:
//   top   <file...>                     Top self-time (leaf), total-time, and
//                                        allocation-by-class tables. The default.
//   self  <leafSubstr>  <pkg> <file...>  Attribute a hot JDK leaf (e.g.
//                                        java.util.HashMap.getNode) to the nearest
//                                        frame whose class name starts with <pkg>.
//   alloc <classSubstr> <pkg> <file...>  Attribute allocations of a class (e.g.
//                                        "[Ljava.lang.Object;" or "ArrayList$Itr")
//                                        to the nearest <pkg> frame.
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
            System.err.println("usage: java jfr-analyze.java <top|self|alloc> [args] <file.jfr...>");
            System.exit(2);
        }
        String mode = args[0];
        if (mode.equals("top")) {
            top(args, 1);
        } else if (mode.equals("self") || mode.equals("alloc")) {
            nearest(mode, args[1], args[2], args, 3);
        } else {
            // No subcommand: treat all args as files, run "top".
            top(args, 0);
        }
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
}

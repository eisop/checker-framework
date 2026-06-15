import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Deterministic total-allocation reader for a single forked-javac JFR recording.
 *
 * <p>JFR's {@code jdk.ObjectAllocationSample} / TLAB events are *sampled*: on these workloads two
 * identical runs vary by ~2x in per-class allocation counts, which drowns any sub-2% allocation
 * change (see the {@code alloc} mode of jfr-analyze, which is for *attribution*, not magnitude).
 * {@code jdk.ThreadAllocationStatistics} instead reports each thread's *cumulative* bytes
 * allocated — it is not sampled, so for a deterministic single-process workload it reproduces to
 * ~0.15%. That makes it the right tool for an allocation A/B of a small change.
 *
 * <p>Capture (the forked compiler JVM, single source set so there is one compile thread):
 *
 * <pre>
 *   checker/bin/javac \
 *     -J-XX:StartFlightRecording=settings=profile,filename=run.jfr,dumponexit=true \
 *     -processor nullness -d /tmp/out  Foo.java ...
 *   java .claude/skills/cf-performance/alloc-total.java run.jfr
 * </pre>
 *
 * Take the median of >=3 runs per side; a real change is the one that clears the ~0.5% run-to-run
 * band. This measures the single compilation thread (javac is single-threaded), so it is the exact
 * bytes the checker + javac allocated, independent of GC/JIT scheduling.
 */
public class alloc_total {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: java alloc-total.java <recording.jfr> [more.jfr ...]");
            System.exit(2);
        }
        // Sum the maximum cumulative ThreadAllocationStatistics.allocated per thread = total bytes.
        Map<Long, Long> maxByThread = new HashMap<>();
        long events = 0;
        for (String path : args) {
            for (RecordedEvent e : RecordingFile.readAllEvents(Paths.get(path))) {
                if (e.getEventType().getName().equals("jdk.ThreadAllocationStatistics")) {
                    long allocated = e.getLong("allocated");
                    long tid = e.getThread() != null ? e.getThread().getId() : -1;
                    maxByThread.merge(tid, allocated, Math::max);
                    events++;
                }
            }
        }
        long total = maxByThread.values().stream().mapToLong(Long::longValue).sum();
        System.out.printf(
                "ThreadAllocationStatistics events=%d threads=%d  TOTAL ALLOCATED = %,d bytes ="
                        + " %.1f MB%n",
                events, maxByThread.size(), total, total / 1048576.0);
    }
}

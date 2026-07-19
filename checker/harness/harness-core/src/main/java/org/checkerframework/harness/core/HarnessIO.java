package org.checkerframework.harness.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * IO utilities for persisting harness results and generating human-readable reports.
 *
 * <p>Responsibilities: - Serialize/deserialize single {@link Driver.RunResult} snapshots (JSON). -
 * Emit concise A/B Markdown reports for a baseline/update pair. - Emit aggregated series reports
 * used by protocol orchestration (SINGLE/CROSS).
 */
public final class HarnessIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private HarnessIO() {}

    /**
     * Writes the given object as JSON. If {@code data} is a {@link Driver.RunResult}, it is
     * converted to a lean serializable DTO to avoid leaking implementation types.
     *
     * @param file destination path; parent directories are created as needed
     * @param data a POJO or {@link Driver.RunResult}
     * @throws IOException if writing fails
     */
    public static void writeJson(Path file, Object data) throws IOException {
        Files.createDirectories(file.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            Object toWrite = data;
            if (data instanceof Driver.RunResult) {
                toWrite = toSerializable((Driver.RunResult) data);
            }
            w.write(GSON.toJson(toWrite));
        }
    }

    /**
     * Writes a unified Markdown report for SINGLE/CROSS protocols.
     *
     * @param file destination Markdown file
     * @param baseline representative baseline run result (used for flags/diagnostics display)
     * @param update representative update run result (used for flags/diagnostics display)
     * @param context optional key/value context (protocol, runs, engine, generator params)
     * @param seriesA timing samples for baseline variant (milliseconds)
     * @param runsA number of requested runs for baseline
     * @param successA number of successful runs for baseline
     * @param seriesB timing samples for update variant (milliseconds)
     * @param runsB number of requested runs for update
     * @param successB number of successful runs for update
     * @throws IOException if writing fails
     */
    public static void writeUnifiedReport(
            Path file,
            Driver.RunResult baseline,
            Driver.RunResult update,
            Map<String, String> context,
            java.util.List<Long> seriesA,
            int runsA,
            int successA,
            java.util.List<Long> seriesB,
            int runsB,
            int successB)
            throws IOException {
        Files.createDirectories(file.getParent());
        List<String> out = new ArrayList<>();

        out.add("## Test Results");
        out.add(
                "Generated: "
                        + ZonedDateTime.now(ZoneId.systemDefault())
                                .format(DateTimeFormatter.RFC_1123_DATE_TIME));
        out.add("");

        // Description
        out.add("### Description");
        out.add("| Key | Value |");
        out.add("| --- | --- |");
        out.add("| Generator | " + escapeMd(getStringMeta(baseline, "generator")) + " |");
        out.add("| Label A | " + escapeMd(baseline.label()) + " |");
        out.add("| Label B | " + escapeMd(update.label()) + " |");
        out.add(
                "| Baseline result.json | "
                        + code(baseline.workDir().resolve("result.json").toString())
                        + " |");
        out.add(
                "| Update result.json | "
                        + code(update.workDir().resolve("result.json").toString())
                        + " |");
        if (context != null) {
            String proto = context.getOrDefault("protocol", "");
            String runs = context.getOrDefault("runs", "");
            String engine = context.getOrDefault("engine", "");
            String sampleCount = context.getOrDefault("sampleCount", "");
            String seed = context.getOrDefault("seed", "");
            String groupsPerFile = context.getOrDefault("groupsPerFile", "");
            if (!proto.isEmpty()) out.add("| Protocol | " + escapeMd(proto) + " |");
            if (!runs.isEmpty()) out.add("| Runs requested | " + escapeMd(runs) + " |");
            if (!engine.isEmpty()) out.add("| Engine | " + escapeMd(engine) + " |");
            if (!sampleCount.isEmpty()) out.add("| sampleCount | " + escapeMd(sampleCount) + " |");
            if (!seed.isEmpty()) out.add("| seed | " + escapeMd(seed) + " |");
            if (!groupsPerFile.isEmpty())
                out.add("| groupsPerFile | " + escapeMd(groupsPerFile) + " |");
        }
        out.add("");

        // Environment
        out.add("### Environment");
        out.add("| Key | Value |");
        out.add("| --- | --- |");
        out.add("| JDK Version | " + code(System.getProperty("java.version", "")) + " |");
        out.add("| JDK Home | " + code(System.getProperty("java.home", "")) + " |");
        out.add(
                "| OS | "
                        + escapeMd(System.getProperty("os.name", ""))
                        + " "
                        + escapeMd(System.getProperty("os.version", ""))
                        + " ("
                        + escapeMd(System.getProperty("os.arch", ""))
                        + ") |");
        out.add("| CPU Cores | " + Runtime.getRuntime().availableProcessors() + " |");
        out.add("");

        // Diagnostics (placed first per request)
        out.add("### Diagnostics");
        // Short legend for common kinds
        out.add("NOTE: informational message; does not affect compilation result");
        out.add("WARNING: potential issue; compilation still succeeds");
        out.add("ERROR: compilation error; typically causes build failure");
        boolean eq = equalDiagnostics(baseline, update);
        // By-kind summary
        java.util.Map<String, Integer> kindA = countDiagnosticsByKind(baseline);
        java.util.Map<String, Integer> kindB = countDiagnosticsByKind(update);
        java.util.TreeSet<String> allKinds = new java.util.TreeSet<String>();
        allKinds.addAll(kindA.keySet());
        allKinds.addAll(kindB.keySet());
        if (!allKinds.isEmpty()) {
            out.add("");
            out.add("| Kind | Baseline | Update | Delta |");
            out.add("| --- | ---:| ---:| ---:|");
            for (String k : allKinds) {
                int va = kindA.getOrDefault(k, Integer.valueOf(0)).intValue();
                int vb = kindB.getOrDefault(k, Integer.valueOf(0)).intValue();
                String delta =
                        (va == 0)
                                ? (vb == 0 ? "0" : String.valueOf(vb))
                                : String.format(
                                        java.util.Locale.ROOT, "%.3f%%", (vb - va) * 100.0 / va);
                out.add("| " + escapeMd(k) + " | " + va + " | " + vb + " | " + delta + " |");
            }
        }
        if (!eq) {
            out.add("");
            out.add("Top differences (first 5 of each):");
            out.add("| Index | Baseline | Update |");
            out.add("| ---:| --- | --- |");
            int n =
                    Math.min(
                            5,
                            Math.max(baseline.diagnostics().size(), update.diagnostics().size()));
            for (int i = 0; i < n; i++) {
                String ba =
                        i < baseline.diagnostics().size()
                                ? escapeMd(formatDiag(baseline.diagnostics().get(i)))
                                : "";
                String ub =
                        i < update.diagnostics().size()
                                ? escapeMd(formatDiag(update.diagnostics().get(i)))
                                : "";
                out.add("| " + i + " | " + ba + " | " + ub + " |");
            }
        }
        out.add("");
        out.add("Match status of diagnostics are: " + (eq ? "**IDENTICAL**" : "**DIFFERENT**"));
        out.add("");

        // Summary (aggregated)
        out.add("### Summary (aggregated over successful samples)");
        out.add(
                "| Variant | median (ms) | average (ms) | min (ms) | max (ms) | samples | success | succ% |");
        out.add("| --- | ---:| ---:| ---:| ---:| ---:| ---:| ---:|");
        out.add(formatSeriesRow("A", seriesA, runsA, successA));
        out.add(formatSeriesRow("B", seriesB, runsB, successB));
        out.add("");

        // Baseline vs Update comparison (aggregated)
        out.add("### Performance Comparison");
        if (seriesA != null && !seriesA.isEmpty() && seriesB != null && !seriesB.isEmpty()) {
            double medA = medianOf(seriesA);
            double medB = medianOf(seriesB);
            double avgA = averageOf(seriesA);
            double avgB = averageOf(seriesB);
            out.add("| Metric | Baseline (ms) | Update (ms) | Delta |");
            out.add("| --- | ---:| ---:| ---:|");
            out.add(
                    "| median | "
                            + fmt2(medA)
                            + " ms | "
                            + fmt2(medB)
                            + " ms | "
                            + pctD(medA, medB)
                            + " |");
            out.add(
                    "| average | "
                            + fmt2(avgA)
                            + " ms | "
                            + fmt2(avgB)
                            + " ms | "
                            + pctD(avgA, avgB)
                            + " |");
        } else {
            out.add("n/a (no samples)");
        }
        out.add("");

        out.add("### Reproduction Commands");
        String proto = (context == null) ? "" : context.getOrDefault("protocol", "");
        String runs = (context == null) ? "" : context.getOrDefault("runs", "");
        String engine = (context == null) ? "" : context.getOrDefault("engine", "");
        String sampleCount = (context == null) ? "" : context.getOrDefault("sampleCount", "");
        String seed = (context == null) ? "" : context.getOrDefault("seed", "");
        String gpf = (context == null) ? "" : context.getOrDefault("groupsPerFile", "");
        String baselineFlags = (context == null) ? "" : context.getOrDefault("baselineFlags", "");
        String updateFlags = (context == null) ? "" : context.getOrDefault("updateFlags", "");
        String proc = (context == null) ? "" : context.getOrDefault("processor", "");
        String ppath = (context == null) ? "" : context.getOrDefault("processorPath", "");
        String release = (context == null) ? "" : context.getOrDefault("release", "");
        String jtreg = (context == null) ? "" : context.getOrDefault("jtreg", "");
        String jtregTest = (context == null) ? "" : context.getOrDefault("jtregTest", "");
        if (proc.isEmpty()) proc = getStringMeta(baseline, "processor");
        if (ppath.isEmpty()) {
            ppath = extractPathFromFlags(getStringMeta(baseline, "flags"), "-processorpath");
            if (ppath.isEmpty())
                ppath = extractPathFromFlags(getStringMeta(baseline, "flags"), "-processorPath");
        }
        if (proc.isEmpty()) proc = extractProcessorFromFlags(getStringMeta(baseline, "flags"));
        if (release.isEmpty()) release = extractReleaseFromFlags(getStringMeta(baseline, "flags"));
        String unifiedCmd =
                buildUnifiedReproCommand(
                        proto,
                        runs,
                        engine,
                        sampleCount,
                        seed,
                        gpf,
                        proc,
                        ppath,
                        release,
                        jtreg,
                        jtregTest,
                        baselineFlags,
                        updateFlags);
        out.add("\n```bash");
        out.add(unifiedCmd);
        out.add("```\n");
        out.add("");

        // Flags (moved later per request)
        out.add("### Flags");
        out.add("- Baseline javac args:");
        out.add("\n```");
        for (String f : splitArgsForDisplay(getStringMeta(baseline, "flags"))) out.add(f);
        out.add("```\n");
        out.add("- Update javac args:");
        out.add("\n```");
        for (String f : splitArgsForDisplay(getStringMeta(update, "flags"))) out.add(f);
        out.add("```\n");

        Files.write(file, out, StandardCharsets.UTF_8);
    }

    private static List<String> splitArgsForDisplay(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return out;
        for (String tok : trimmed.split("\\s+")) {
            if (!tok.isEmpty()) out.add(tok);
        }
        return out;
    }

    private static String formatSeriesRow(
            String label, java.util.List<Long> series, int runs, int success) {
        int samples = (series == null) ? 0 : series.size();
        String medStr =
                (samples == 0)
                        ? "n/a"
                        : String.format(
                                Locale.ROOT,
                                "%.2f",
                                new Object[] {Double.valueOf(medianOf(series))});
        String avgStr =
                (samples == 0)
                        ? "n/a"
                        : String.format(
                                Locale.ROOT,
                                "%.2f",
                                new Object[] {Double.valueOf(averageOf(series))});
        String minStr = (samples == 0) ? "n/a" : String.valueOf(minOf(series));
        String maxStr = (samples == 0) ? "n/a" : String.valueOf(maxOf(series));
        String pctStr =
                (runs <= 0)
                        ? "n/a"
                        : String.format(
                                Locale.ROOT,
                                "%.2f%%",
                                new Object[] {Double.valueOf(success * 100.0 / runs)});
        return "| " + label + " | " + medStr + " | " + avgStr + " | " + minStr + " | " + maxStr
                + " | " + samples + " | " + success + " | " + pctStr + " |";
    }

    private static double averageOf(java.util.List<Long> series) {
        if (series == null || series.isEmpty()) return 0.0;
        long sum = 0L;
        for (Long v : series) {
            if (v != null) sum += v.longValue();
        }
        return ((double) sum) / series.size();
    }

    private static String fmt2(double v) {
        return String.format(Locale.ROOT, "%.2f", new Object[] {Double.valueOf(v)});
    }

    private static String pctD(double base, double upd) {
        if (base <= 0.0) return "n/a";
        double d = (upd - base) / base * 100.0;
        return String.format(Locale.ROOT, "%.3f%%", new Object[] {Double.valueOf(d)});
    }

    private static double medianOf(java.util.List<Long> series) {
        if (series == null || series.isEmpty()) return 0.0;
        java.util.ArrayList<Long> copy = new java.util.ArrayList<Long>(series);
        java.util.Collections.sort(copy);
        int n = copy.size();
        if ((n & 1) == 1) return copy.get(n / 2);
        return (copy.get(n / 2 - 1) + copy.get(n / 2)) / 2.0;
    }

    private static long minOf(java.util.List<Long> series) {
        long m = Long.MAX_VALUE;
        for (Long v : series) {
            if (v != null && v.longValue() < m) m = v.longValue();
        }
        return m == Long.MAX_VALUE ? 0L : m;
    }

    private static long maxOf(java.util.List<Long> series) {
        long m = Long.MIN_VALUE;
        for (Long v : series) {
            if (v != null && v.longValue() > m) m = v.longValue();
        }
        return m == Long.MIN_VALUE ? 0L : m;
    }

    /** Return true if two diagnostic lists are identical under stable ordering. */
    private static boolean equalDiagnostics(Driver.RunResult a, Driver.RunResult b) {
        if (a.diagnostics().size() != b.diagnostics().size()) return false;
        for (int i = 0; i < a.diagnostics().size(); i++) {
            Driver.DiagnosticEntry da = a.diagnostics().get(i);
            Driver.DiagnosticEntry db = b.diagnostics().get(i);
            if (!da.file().equals(db.file())) return false;
            if (da.line() != db.line()) return false;
            if (da.column() != db.column()) return false;
            if (!da.kind().equals(db.kind())) return false;
            if (!da.message().equals(db.message())) return false;
        }
        return true;
    }

    private static String formatDiag(Driver.DiagnosticEntry d) {
        return d.file()
                + ":"
                + d.line()
                + ":"
                + d.column()
                + " "
                + d.kind()
                + " "
                + summarize(d.message());
    }

    private static String summarize(String s) {
        if (s == null) return "";
        String trimmed = s.replace('\n', ' ').replace("\r", " ");
        if (trimmed.length() > 160) {
            return trimmed.substring(0, 160) + "...";
        }
        return trimmed;
    }

    private static String pct(long base, long upd) {
        if (base <= 0) return "n/a";
        double d = ((double) (upd - base)) / base * 100.0;
        return String.format(Locale.ROOT, "%.3f%%", new Object[] {Double.valueOf(d)});
    }

    /** Convert simple values/maps to compact strings for tables (maps are key-sorted). */
    private static String valueToString(Object o) {
        if (o == null) return "";
        if (o instanceof Map) {
            // Stable order by key string
            java.util.List<Map.Entry<?, ?>> entries =
                    new java.util.ArrayList<Map.Entry<?, ?>>(((Map<?, ?>) o).entrySet());
            java.util.Collections.sort(
                    entries,
                    (e1, e2) -> String.valueOf(e1.getKey()).compareTo(String.valueOf(e2.getKey())));
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<?, ?> e : entries) {
                if (!first) sb.append(',');
                first = false;
                sb.append(String.valueOf(e.getKey()))
                        .append(':')
                        .append(String.valueOf(e.getValue()));
            }
            return sb.toString();
        }
        return String.valueOf(o);
    }

    private static String escapeMd(String s) {
        if (s == null) return "";
        String r = s.replace("|", "\\|");
        r = r.replace("\n", " ").replace("\r", " ");
        return r;
    }

    private static String code(String s) {
        if (s == null || s.isEmpty()) return "";
        // Use backticks to improve readability in tables
        String cleaned = s.replace("`", "'");
        return "`" + cleaned + "`";
    }

    private static String getStringMeta(Driver.RunResult r, String key) {
        if (r == null || r.meta() == null) return "";
        Object v = r.meta().get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private static SerializableRunResult toSerializable(Driver.RunResult r) {
        return new SerializableRunResult(
                r.label(),
                r.wallMillis(),
                r.success(),
                r.diagnostics(),
                r.metrics(),
                r.workDir() == null ? null : r.workDir().toString(),
                r.meta());
    }

    private static String extractPathFromFlags(String flags, String key) {
        if (flags == null || flags.isEmpty()) return "";
        String[] toks = flags.trim().split("\\s+");
        for (int i = 0; i < toks.length - 1; i++) {
            if (toks[i].equals(key) && !toks[i + 1].startsWith("-")) {
                return toks[i + 1];
            }
        }
        return "";
    }

    private static String extractProcessorFromFlags(String flags) {
        if (flags == null || flags.isEmpty()) return "";
        String[] toks = flags.trim().split("\\s+");
        for (int i = 0; i < toks.length - 1; i++) {
            if (toks[i].equals("-processor") && !toks[i + 1].startsWith("-")) {
                return toks[i + 1];
            }
        }
        return "";
    }

    private static String extractReleaseFromFlags(String flags) {
        if (flags == null || flags.isEmpty()) return "";
        String[] toks = flags.trim().split("\\s+");
        for (int i = 0; i < toks.length - 1; i++) {
            if (toks[i].equals("--release") && !toks[i + 1].startsWith("-")) {
                return toks[i + 1];
            }
        }
        return "";
    }

    private static String buildReproCommand(
            String protocol,
            String runs,
            String engine,
            String sampleCount,
            String seed,
            String groupsPerFile,
            String processor,
            String processorPath,
            String release,
            String jtreg,
            String jtregTest,
            String variantFlags,
            boolean isUpdate) {
        StringBuilder sb = new StringBuilder();
        sb.append("./gradlew :harness-driver-cli:run --no-daemon --console=plain --args=\"");
        sb.append("--generator NewAndArray");
        if (!sampleCount.isEmpty()) sb.append(" --sampleCount ").append(sampleCount);
        if (!seed.isEmpty()) sb.append(" --seed ").append(seed);
        if (!processor.isEmpty()) sb.append(" --processor ").append(processor);
        if (!processorPath.isEmpty()) sb.append(" --processor-path ").append(processorPath);
        if (!release.isEmpty()) sb.append(" --release ").append(release);
        if (!protocol.isEmpty()) sb.append(" --protocol ").append(protocol);
        if (!runs.isEmpty()) sb.append(" --runs ").append(runs);
        if (!engine.isEmpty()) sb.append(" --engine ").append(engine);
        if (!jtreg.isEmpty()) sb.append(" --jtreg ").append(jtreg);
        if (!jtregTest.isEmpty()) sb.append(" --jtreg-test ").append(jtregTest);
        if (!groupsPerFile.isEmpty()) sb.append(" --extra.groupsPerFile ").append(groupsPerFile);
        if (variantFlags != null && !variantFlags.isEmpty()) {
            sb.append(isUpdate ? " --update-flags " : " --baseline-flags ");
            sb.append(variantFlags);
        }
        sb.append("\"");
        return sb.toString();
    }

    /** Build a unified reproduction command that includes both baseline and update flags. */
    private static String buildUnifiedReproCommand(
            String protocol,
            String runs,
            String engine,
            String sampleCount,
            String seed,
            String groupsPerFile,
            String processor,
            String processorPath,
            String release,
            String jtreg,
            String jtregTest,
            String baselineFlags,
            String updateFlags) {
        StringBuilder sb = new StringBuilder();
        sb.append("../../gradlew :harness-driver-cli:run --no-daemon --console=plain --args=\"");
        sb.append("--generator NewAndArray");
        if (!sampleCount.isEmpty()) sb.append(" --sampleCount ").append(sampleCount);
        if (!seed.isEmpty()) sb.append(" --seed ").append(seed);
        if (!processor.isEmpty()) sb.append(" --processor ").append(processor);
        if (!processorPath.isEmpty()) sb.append(" --processor-path ").append(processorPath);
        if (!release.isEmpty()) sb.append(" --release ").append(release);
        if (!protocol.isEmpty()) sb.append(" --protocol ").append(protocol);
        if (!runs.isEmpty()) sb.append(" --runs ").append(runs);
        if (!engine.isEmpty()) sb.append(" --engine ").append(engine);
        if (!jtreg.isEmpty()) sb.append(" --jtreg ").append(jtreg);
        if (!jtregTest.isEmpty()) sb.append(" --jtreg-test ").append(jtregTest);
        if (!groupsPerFile.isEmpty()) sb.append(" --extra.groupsPerFile ").append(groupsPerFile);
        if (baselineFlags != null && !baselineFlags.isEmpty()) {
            sb.append(" --baseline-flags ").append(baselineFlags);
        }
        if (updateFlags != null && !updateFlags.isEmpty()) {
            sb.append(" --update-flags ").append(updateFlags);
        }
        sb.append("\"");
        return sb.toString();
    }

    private static java.util.Map<String, Integer> countDiagnosticsByKind(Driver.RunResult r) {
        java.util.Map<String, Integer> m = new java.util.HashMap<String, Integer>();
        if (r != null && r.diagnostics() != null) {
            for (Driver.DiagnosticEntry d : r.diagnostics()) {
                String k = d.kind();
                Integer prev = m.get(k);
                m.put(k, Integer.valueOf(prev == null ? 1 : (prev.intValue() + 1)));
            }
        }
        return m;
    }

    static final class SerializableRunResult {
        final String label;
        final long wallMillis;
        final List<Driver.DiagnosticEntry> diagnostics;
        final java.util.Map<String, Object> metrics;
        final String workDir;
        final java.util.Map<String, Object> meta;
        final boolean success;

        SerializableRunResult(
                String label,
                long wallMillis,
                boolean success,
                List<Driver.DiagnosticEntry> diagnostics,
                java.util.Map<String, Object> metrics,
                String workDir,
                java.util.Map<String, Object> meta) {
            this.label = label;
            this.wallMillis = wallMillis;
            this.success = success;
            this.diagnostics = diagnostics;
            this.metrics = metrics;
            this.workDir = workDir;
            this.meta = meta;
        }
    }
}

package org.checkerframework.framework.test;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringJoiner;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.SystemUtil;
import org.junit.Assert;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.StringsPlume;
import org.plumelib.util.SystemPlume;

/** Utilities for testing. */
public class TestUtilities {

  /** True if the JVM is version 9 or above. */
  public static final boolean IS_AT_LEAST_9_JVM = SystemUtil.getJreVersion() >= 9;
  /** True if the JVM is version 11 or above. */
  public static final boolean IS_AT_LEAST_11_JVM = SystemUtil.getJreVersion() >= 11;
  /** True if the JVM is version 11 or lower. */
  public static final boolean IS_AT_MOST_11_JVM = SystemUtil.getJreVersion() <= 11;
  /** True if the JVM is version 17 or above. */
  public static final boolean IS_AT_LEAST_17_JVM = SystemUtil.getJreVersion() >= 17;
  /** True if the JVM is version 17 or lower. */
  public static final boolean IS_AT_MOST_17_JVM = SystemUtil.getJreVersion() <= 17;

  static {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    OutputStream err = new ByteArrayOutputStream();
    compiler.run(null, null, err, "-version");
  }

  public static List<File> findNestedJavaTestFiles(String... dirNames) {
    return findRelativeNestedJavaFiles(new File("tests"), dirNames);
  }

  public static List<File> findRelativeNestedJavaFiles(String parent, String... dirNames) {
    return findRelativeNestedJavaFiles(new File(parent), dirNames);
  }

  public static List<File> findRelativeNestedJavaFiles(File parent, String... dirNames) {
    File[] dirs = new File[dirNames.length];

    int i = 0;
    for (String dirName : dirNames) {
      dirs[i] = new File(parent, dirName);
      i += 1;
    }

    return getJavaFilesAsArgumentList(dirs);
  }

  /**
   * Returns a list where each item is a list of Java files, excluding any skip tests, for each
   * directory given by dirName and also a list for any subdirectory.
   *
   * @param parent parent directory of the dirNames directories
   * @param dirNames names of directories to search
   * @return list where each item is a list of Java test files grouped by directory
   */
  public static List<List<File>> findJavaFilesPerDirectory(File parent, String... dirNames) {
    if (!parent.exists()) {
      throw new BugInCF(
          "test parent directory does not exist: %s %s", parent, parent.getAbsoluteFile());
    }
    if (!parent.isDirectory()) {
      throw new BugInCF(
          "test parent directory is not a directory: %s %s", parent, parent.getAbsoluteFile());
    }

    List<List<File>> filesPerDirectory = new ArrayList<>();

    for (String dirName : dirNames) {
      File dir = new File(parent, dirName).toPath().toAbsolutePath().normalize().toFile();
      if (!dir.isDirectory()) {
        // For "ainfer-*" tests, their sources do not necessarily
        // exist yet but will be created by a test that runs earlier than they do.
        if (!(dir.getName().equals("annotated")
            && dir.getParentFile() != null
            && dir.getParentFile().getName().startsWith("ainfer-"))) {
          throw new BugInCF("test directory does not exist: %s", dir);
        }
      }
      if (dir.isDirectory()) {
        filesPerDirectory.addAll(findJavaTestFilesInDirectory(dir));
      }
    }

    return filesPerDirectory;
  }

  /**
   * Returns a list where each item is a list of Java files, excluding any skip tests. There is one
   * list for {@code dir}, and one list for each subdirectory of {@code dir}.
   *
   * @param dir directory in which to search for Java files
   * @return a list of list of Java test files
   */
  private static List<List<File>> findJavaTestFilesInDirectory(File dir) {
    List<List<File>> fileGroupedByDirectory = new ArrayList<>();
    List<File> filesInDir = new ArrayList<>();

    fileGroupedByDirectory.add(filesInDir);
    String[] dirContents = dir.list();
    if (dirContents == null) {
      throw new Error("Not a directory: " + dir);
    }
    Arrays.sort(dirContents);
    for (String fileName : dirContents) {
      File file = new File(dir, fileName);
      if (file.isDirectory()) {
        fileGroupedByDirectory.addAll(findJavaTestFilesInDirectory(file));
      } else if (isJavaTestFile(file)) {
        filesInDir.add(file);
      }
    }
    if (filesInDir.isEmpty()) {
      fileGroupedByDirectory.remove(filesInDir);
    }
    return fileGroupedByDirectory;
  }

  /**
   * Prepends a file to the beginning of each filename.
   *
   * @param parent a file to prepend to each filename
   * @param fileNames file names
   * @return the file names, each with {@code parent} prepended
   */
  public static List<Object[]> findFilesInParent(File parent, String... fileNames) {
    return CollectionsPlume.mapList(
        (String fileName) -> new Object[] {new File(parent, fileName)}, fileNames);
  }

  /**
   * Traverses the directories listed looking for Java test files.
   *
   * @param dirs directories in which to search for Java test files
   * @return a list of Java test files found in the directories
   */
  public static List<File> getJavaFilesAsArgumentList(File... dirs) {
    List<File> arguments = new ArrayList<>();
    for (File dir : dirs) {
      arguments.addAll(deeplyEnclosedJavaTestFiles(dir));
    }
    return arguments;
  }

  /**
   * Returns all the Java files that are descendants of the given directory.
   *
   * @param directory a directory
   * @return all the Java files that are descendants of the given directory
   */
  public static List<File> deeplyEnclosedJavaTestFiles(File directory) {
    if (!directory.exists()) {
      throw new IllegalArgumentException(
          "directory does not exist: " + directory + " " + directory.getAbsolutePath());
    }
    if (!directory.isDirectory()) {
      throw new IllegalArgumentException("found file instead of directory: " + directory);
    }

    List<File> javaFiles = new ArrayList<>();

    @SuppressWarnings("nullness") // checked above that it's a directory
    File @NonNull [] in = directory.listFiles();
    Arrays.sort(
        in,
        new Comparator<File>() {
          @Override
          public int compare(File o1, File o2) {
            return o1.getName().compareTo(o2.getName());
          }
        });
    for (File file : in) {
      if (file.isDirectory()) {
        javaFiles.addAll(deeplyEnclosedJavaTestFiles(file));
      } else if (isJavaTestFile(file)) {
        javaFiles.add(file);
      }
    }

    return javaFiles;
  }

  public static boolean isJavaFile(File file) {
    return file.isFile() && file.getName().endsWith(".java");
  }

  public static boolean isJavaTestFile(File file) {
    if (!isJavaFile(file)) {
      return false;
    }

    Scanner in = null;
    try {
      in = new Scanner(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }

    while (in.hasNext()) {
      String nextLine = in.nextLine();
      if (nextLine.contains("@skip-test")
          || (!IS_AT_LEAST_9_JVM && nextLine.contains("@below-java9-jdk-skip-test"))
          || (!IS_AT_LEAST_11_JVM && nextLine.contains("@below-java11-jdk-skip-test"))
          || (!IS_AT_MOST_11_JVM && nextLine.contains("@above-java11-skip-test"))
          || (!IS_AT_LEAST_17_JVM && nextLine.contains("@below-java17-jdk-skip-test"))
          || (!IS_AT_MOST_17_JVM && nextLine.contains("@above-java17-skip-test"))) {
        in.close();
        return false;
      }
    }

    in.close();
    return true;
  }

  public static @Nullable String diagnosticToString(
      final Diagnostic<? extends JavaFileObject> diagnostic, boolean usingAnomsgtxt) {

    String result = diagnostic.toString().trim();

    // suppress Xlint warnings
    if (result.contains("uses unchecked or unsafe operations.")
        || result.contains("Recompile with -Xlint:unchecked for details.")
        || result.endsWith(" declares unsafe vararg methods.")
        || result.contains("Recompile with -Xlint:varargs for details.")) {
      return null;
    }

    if (usingAnomsgtxt) {
      // Lines with "unexpected Throwable" are stack traces
      // and should be printed in full.
      if (!result.contains("unexpected Throwable")) {
        String firstLine;
        int lineSepPos = result.indexOf(System.lineSeparator());
        if (lineSepPos != -1) {
          firstLine = result.substring(0, lineSepPos);
        } else {
          firstLine = result;
        }
        int javaPos = firstLine.indexOf(".java:");
        if (javaPos != -1) {
          firstLine = firstLine.substring(javaPos + 5).trim();
        }
        result = firstLine;
      }
    }

    return result;
  }

  public static Set<String> diagnosticsToStrings(
      final Iterable<Diagnostic<? extends JavaFileObject>> actualDiagnostics,
      boolean usingAnomsgtxt) {
    Set<String> actualDiagnosticsStr = new LinkedHashSet<>();
    for (Diagnostic<? extends JavaFileObject> diagnostic : actualDiagnostics) {
      String diagnosticStr = TestUtilities.diagnosticToString(diagnostic, usingAnomsgtxt);
      if (diagnosticStr != null) {
        actualDiagnosticsStr.add(diagnosticStr);
      }
    }

    return actualDiagnosticsStr;
  }

  /**
   * Return the file absolute pathnames, separated by commas.
   *
   * @param javaFiles a list of Java files
   * @return the file absolute pathnames, separated by commas
   */
  public static String summarizeSourceFiles(List<File> javaFiles) {
    StringJoiner sj = new StringJoiner(", ");
    for (File file : javaFiles) {
      sj.add(file.getAbsolutePath());
    }
    return sj.toString();
  }

  public static File getTestFile(String fileRelativeToTestsDir) {
    return new File("tests", fileRelativeToTestsDir);
  }

  public static File findComparisonFile(File testFile) {
    final File comparisonFile =
        new File(testFile.getParent(), testFile.getName().replace(".java", ".out"));
    return comparisonFile;
  }

  public static List<String> optionMapToList(Map<String, @Nullable String> options) {
    List<String> optionList = new ArrayList<>(options.size() * 2);

    for (Map.Entry<String, @Nullable String> opt : options.entrySet()) {
      optionList.add(opt.getKey());

      if (opt.getValue() != null) {
        optionList.add(opt.getValue());
      }
    }

    return optionList;
  }

  public static void writeLines(File file, Iterable<?> lines) {
    try {
      final BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
      Iterator<?> iter = lines.iterator();
      while (iter.hasNext()) {
        Object next = iter.next();
        if (next == null) {
          bw.write("<null>");
        } else {
          bw.write(next.toString());
        }
        bw.newLine();
      }
      bw.flush();
      bw.close();

    } catch (IOException io) {
      throw new RuntimeException(io);
    }
  }

  public static void writeDiagnostics(
      File file,
      File testFile,
      List<String> expected,
      List<String> actual,
      List<String> unexpected,
      List<String> missing,
      boolean usingNoMsgText,
      boolean testFailed) {
    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
      pw.println("File: " + testFile.getAbsolutePath());
      pw.println("TestFailed: " + testFailed);
      pw.println("Using nomsgtxt: " + usingNoMsgText);
      pw.println("#Missing: " + missing.size() + "      #Unexpected: " + unexpected.size());

      pw.println("Expected:");
      pw.println(StringsPlume.joinLines(expected));
      pw.println();

      pw.println("Actual:");
      pw.println(StringsPlume.joinLines(actual));
      pw.println();

      pw.println("Missing:");
      pw.println(StringsPlume.joinLines(missing));
      pw.println();

      pw.println("Unexpected:");
      pw.println(StringsPlume.joinLines(unexpected));
      pw.println();

      pw.println();
      pw.println();
      pw.flush();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeTestConfiguration(File file, TestConfiguration config) {
    try {
      final BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
      bw.write(config.toString());
      bw.newLine();
      bw.newLine();
      bw.flush();
      bw.close();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeJavacArguments(
      File file,
      Iterable<? extends JavaFileObject> files,
      Iterable<String> options,
      Iterable<String> processors) {
    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
      pw.println("Files:");
      for (JavaFileObject f : files) {
        pw.println("    " + f.getName());
      }
      pw.println();

      pw.println("Options:");
      for (String o : options) {
        pw.println("    " + o);
      }
      pw.println();

      pw.println("Processors:");
      for (String p : processors) {
        pw.println("    " + p);
      }
      pw.println();
      pw.println();

      pw.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * If the given TypecheckResult has unexpected or missing diagnostics, fail the running JUnit
   * test.
   *
   * @param testResult the result of type-checking
   */
  public static void assertTestDidNotFail(TypecheckResult testResult) {
    if (testResult.didTestFail()) {
      if (getShouldEmitDebugInfo()) {
        System.out.println("---------------- start of javac ouput ----------------");
        System.out.println(testResult.getCompilationResult().getJavacOutput());
        System.out.println("---------------- end of javac ouput ----------------");
      } else {
        System.out.println("To see the javac command line and output, run with: -Pemit.test.debug");
      }
      Assert.fail(testResult.summarize());
    }
  }

  /**
   * Create the directory (and its parents) if it does not exist.
   *
   * @param dir the directory to create
   */
  public static void ensureDirectoryExists(String dir) {
    try {
      Files.createDirectories(Paths.get(dir));
    } catch (FileAlreadyExistsException e) {
      // directory already exists
    } catch (IOException e) {
      throw new RuntimeException("Could not make directory: " + dir + ": " + e.getMessage());
    }
  }

  /**
   * Return true if the system property is set to "true". Return false if the system property is not
   * set or is set to "false". Otherwise, errs.
   *
   * @param key system property to check
   * @return true if the system property is set to "true". Return false if the system property is
   *     not set or is set to "false". Otherwise, errs.
   * @deprecated Use {@link SystemUtil#getBooleanSystemProperty(String)} instead.
   */
  @Deprecated // 2020-04-30
  public static boolean testBooleanProperty(String key) {
    return testBooleanProperty(key, false);
  }

  /**
   * If the system property is set, return its boolean value; otherwise return {@code defaultValue}.
   * Errs if the system property is set to a non-boolean value.
   *
   * @param key system property to check
   * @param defaultValue value to use if the property is not set
   * @return the boolean value of {@code key} or {@code defaultValue} if {@code key} is not set
   * @deprecated Use {@link SystemUtil#getBooleanSystemProperty(String, boolean)} instead.
   */
  @Deprecated // 2020-04-30
  public static boolean testBooleanProperty(String key, boolean defaultValue) {
    return SystemUtil.getBooleanSystemProperty(key, defaultValue);
  }

  /**
   * Returns the value of system property "emit.test.debug".
   *
   * @return the value of system property "emit.test.debug"
   */
  public static boolean getShouldEmitDebugInfo() {
    return SystemPlume.getBooleanSystemProperty("emit.test.debug");
  }
}

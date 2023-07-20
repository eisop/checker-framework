package org.checkerframework.framework.sarif;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Pair;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.util.Collections;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import de.jcup.sarif_2_1_0.model.*;

/**
 * Provide support for handling with SARIF outputs. If you want to output a SARIF you need to call
 * {@link #initialize(Path)}, which also adds a shutdown hook to the JVM.
 *
 * <p>Use {@link #addResult(Diagnostic.Kind, String, Element)} and others to report an issue.
 *
 * @author Alexander Weigl <weigl@kit.edu>
 * @version 1 (20.07.23)
 */
public class SarifFacade {
    @Nullable private static SarifSchema210 schema;
    @Nullable private static Run run1;

    public static SarifSchema210 getReport() {
        return schema;
    }

    public static void addResult(Result e) {
        if (run1 != null) {
            run1.getResults().add(e);
        }
    }

    /**
     * @param sink
     */
    public static void initialize(Path sink) {
        schema = new SarifSchema210();
        run1 = new Run();
        Tool toolCheckerFramework = new Tool();
        ToolComponent driver = new ToolComponent();
        driver.setName("checker-framework");
        String driverGuid = "1234-guid-test-tool-driver-id";
        driver.setGuid(driverGuid);
        driver.setFullName("Only-Test");
        toolCheckerFramework.setDriver(driver);
        run1.setTool(toolCheckerFramework);
        schema.getRuns().add(run1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> SarifFacade.save(sink)));
    }

    public static void save(Path sink) {}

    public static void addResult(Diagnostic.Kind kind, String messageText, Element preciseSource) {
        JCDiagnostic.DiagnosticPosition pos = null;
        JavacProcessingEnvironment processingEnv = null; // TODO How to receive this
        JavacElements elemUtils = processingEnv.getElementUtils();
        Pair<JCTree, JCTree.JCCompilationUnit> treeTop = elemUtils.getTreeAndTopLevel(e, a, v);
        if (treeTop != null) {
            JavaFileObject newSource = treeTop.snd.sourcefile;
            if (newSource != null) {
                // save the old version and reinstate it later
                pos = treeTop.fst.pos();
            }
        }
    }

    public static void addResult(
            Diagnostic.Kind kind,
            String messageText,
            String uri,
            int lineStart,
            int columnStart,
            int lineEnd,
            int columnEnd) {
        Result e = new Result();
        e.setLevel(toKind(kind));
        Location loc = new Location();
        PhysicalLocation pl = new PhysicalLocation();
        ArtifactLocation al = new ArtifactLocation();
        al.setUri(uri);
        pl.setArtifactLocation(al);
        Region region = new Region();
        region.setStartLine(lineStart);
        region.setStartColumn(columnStart);
        region.setEndColumn(columnEnd);
        region.setEndLine(lineEnd);
        pl.setRegion(region);
        loc.setPhysicalLocation(pl);
        e.setLocations(Collections.singletonList(loc));
        Message message = new Message();
        message.setText(messageText);
        e.setMessage(message);
        addResult(e);
    }

    private static Result.Level toKind(Diagnostic.Kind kind) {
        switch (kind) {
            case ERROR:
                return Result.Level.ERROR;
            case WARNING:
            case MANDATORY_WARNING:
                return Result.Level.WARNING;
            case NOTE:
                return Result.Level.NOTE;
            case OTHER:
                return Result.Level.NONE;
        }
        throw new IllegalArgumentException("unreachable");
    }

    public static void addResult(
            Diagnostic.Kind kind,
            String messageText,
            Tree preciseSource,
            CompilationUnitTree currentRoot) {
        JCDiagnostic.DiagnosticPosition pos = ((JCTree) preciseSource).pos();
        // addResult(kind, messageText, );
    }
}

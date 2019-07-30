/*
 * @test
 * @summary Test case for issue #2173: https://github.com/typetools/checker-framework/issues/2173
 *
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker View.java
 */

import javafx.stage.Stage;
import javafx.stage.Window;

public class View {
    private static void createTable() {
        Window window = new Stage();
        ImporterManager.chooseAndImportFile(window);
    }
}

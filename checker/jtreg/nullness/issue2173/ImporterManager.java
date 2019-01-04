import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import org.checkerframework.checker.i18n.qual.LocalizableKey;
import org.checkerframework.checker.i18n.qual.Localized;

interface Importer {
    public List<Pair<@Localized String, List<String>>> getSupportedFileTypes();
}

class TranslationUtility {
    @SuppressWarnings("i18n")
    public static @Localized String getString(@LocalizableKey String key) {
        return key;
    }
}

interface Pair<A, B> {
    A getFirst();

    B getSecond();
}

public class ImporterManager {
    private static final List<Importer> registeredImporters = new ArrayList<>();

    public static void chooseAndImportFile(Window parent) {
        ArrayList<ExtensionFilter> filters = new ArrayList<>();
        filters.add(
                new ExtensionFilter(
                        TranslationUtility.getString("importer.all.known"),
                        registeredImporters
                                .stream()
                                .flatMap(
                                        imp ->
                                                imp.getSupportedFileTypes()
                                                        .stream()
                                                        .flatMap(
                                                                (Pair<
                                                                                        @Localized String,
                                                                                        List<
                                                                                                String>>
                                                                                p) ->
                                                                        p.getSecond().stream()))
                                .collect(Collectors.toList())));
    }
}

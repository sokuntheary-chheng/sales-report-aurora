package edu.itc.salesreport;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The rule of the brief, made executable: the packages {@code model},
 * {@code ingest}, {@code engine}, {@code ui}, {@code ui/mvc} and
 * {@code ui.mvp} must not know a user interface toolkit.
 *
 * <p>The test only looks at the files that lie directly in those packages:
 * the toolkit adapters ({@code ui.fx}, {@code ui.mvc.fx},
 * {@code ui.console}) are allowed to use JavaFX.
 *
 * <p>Lab-03 exception: the MVVM view layer lives directly in {@code ui}
 * (the lab's file layout), so the known view-layer files are allowlisted
 * here. {@code ReportViewModel} is checked separately and more strictly by
 * {@code ViewModelImportsTest}: it may use {@code javafx.base} only.
 */
class UiIndependenceTest {

    private static final Path SRC =
            Path.of("src/main/java/edu/itc/salesreport");

    /** The packages that must stay free of a toolkit. */
    private static final List<String> PACKAGES =
            List.of("model", "ingest", "engine", "ui", "ui/mvc", "ui/mvp");

    /** The imports that are not allowed in those packages. */
    private static final List<String> FORBIDDEN =
            List.of("import javafx.", "import java.awt.", "import javax.swing.");

    /** Lab-03's MVVM classes that live directly in {@code ui}:
     *  javafx.base ports and the view layer. */
    private static final List<String> UI_VIEW_LAYER = List.of(
            "Command.java", "DelegateCommand.java",
            "ReportView.java", "ReportViewerApp.java",
            "FxBackgroundJobs.java", "FrameGapMonitor.java",
            "LoadBenchmarkApp.java", "ReportViewModel.java");

    @Test
    void theCorePackagesDoNotImportAToolkit() throws IOException {
        var offenders = new ArrayList<String>();
        for (String name : PACKAGES) {
            Path dir = SRC.resolve(name);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java"))
                        .toList()) {
                    if (name.equals("ui")
                            && UI_VIEW_LAYER.contains(file.getFileName().toString())) {
                        continue;
                    }
                    var lines = Files.readAllLines(file);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i).strip();
                        if (FORBIDDEN.stream().anyMatch(line::startsWith)) {
                            offenders.add(SRC.relativize(file) + ":" + (i + 1)
                                    + " " + line);
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), offenders,
                "the core packages must not import a toolkit");
    }
}

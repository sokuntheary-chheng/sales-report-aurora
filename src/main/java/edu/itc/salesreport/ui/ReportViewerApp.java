package edu.itc.salesreport.ui;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ingest.MonthData;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Composition root: the only class that knows every concrete type. */
public class ReportViewerApp extends Application {

    private ExecutorService workers;

    @Override
    public void start(Stage stage) {
        Path dataDir = Path.of(getParameters().getNamed()
                .getOrDefault("data", "data"));
        // adapt YOUR Lab-02 model here; the model code stays unchanged:
        // read the month, keep the rows of one branch, aggregate them
        var reader = new CsvMonthReader(new CsvTransactionParser());
        var aggregator = new DefaultSalesAggregator();
        SummarySource source = (month, branch) -> {
            MonthData data;
            try {
                data = reader.read(dataDir, month);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return aggregator.summarize(branch, data.rows().stream()
                    .filter(row -> row.branch().equals(branch)).toList());
        };
        // Task 4: one virtual thread per job; the UI stays responsive
        workers = Executors.newVirtualThreadPerTaskExecutor();
        var vm = new ReportViewModel(source,
                new FxBackgroundJobs(workers), Clock.systemDefaultZone());
        stage.setScene(new Scene(new ReportView(vm).root(), 960, 600));
        stage.setTitle("Angkor Mart · Monthly report (MVVM)");
        stage.show();
    }

    @Override
    public void stop() {
        if (workers != null) {
            workers.shutdownNow();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

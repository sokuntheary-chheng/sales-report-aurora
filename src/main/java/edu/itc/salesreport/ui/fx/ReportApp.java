package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.SalesModel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;

/**
 * The desktop viewer of the Angkor Mart monthly sales report.
 *
 * <p>The application is the composition root: it builds the model, gives it
 * to the view and shows the window. Task 3 hands the gestures to a
 * controller.
 */
public final class ReportApp extends Application {

    private static final Path DATA_DIR = Path.of("data");

    @Override
    public void start(Stage stage) {
        var model = new SalesModel(new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        var view = new ReportView(model);

        stage.setTitle("Angkor Mart · Monthly sales (MVC)");
        stage.setScene(new Scene(view.root(), 1100, 700));
        stage.show();

        try {
            model.load(DATA_DIR, YearMonth.of(2026, 9));
        } catch (IOException e) {
            view.showError("Could not read the sales files: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

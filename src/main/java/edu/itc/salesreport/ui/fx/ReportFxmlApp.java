package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.ReportController;
import edu.itc.salesreport.ui.SalesModel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.Clock;

/**
 * The viewer of Task 2 with the layout in an FXML file.
 *
 * <p>Run it with {@code mvn javafx:run
 * -Djavafx.mainClass=edu.itc.salesreport.ui.fx.ReportFxmlApp}.
 */
public final class ReportFxmlApp extends Application {

    private static final Path DATA_DIR = Path.of("data");

    @Override
    public void start(Stage stage) {
        var model = new SalesModel(new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        var view = ReportFxmlView.load(model);
        var controller = new ReportController(model, DATA_DIR,
                Clock.systemDefaultZone(), view);
        view.bind(controller);

        stage.setTitle("Angkor Mart · Monthly sales (FXML)");
        stage.setScene(new Scene(view.root(), 1100, 700));
        stage.show();

        var arguments = getParameters().getUnnamed();
        controller.monthEntered(arguments.isEmpty() ? "2026-09" : arguments.getFirst());
        if (arguments.size() > 1) {
            controller.branchChosen(arguments.get(1));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

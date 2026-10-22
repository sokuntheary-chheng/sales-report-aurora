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
 * The desktop viewer of the Angkor Mart monthly sales report.
 *
 * <p>The application is the composition root: it builds the model, the view
 * and the controller, hands the gestures to the controller and shows the
 * window.
 */
public final class ReportApp extends Application {

    private static final Path DATA_DIR = Path.of("data");

    @Override
    public void start(Stage stage) {
        var model = new SalesModel(new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        var view = new ReportView(model);
        var controller = new ReportController(model, DATA_DIR,
                Clock.systemDefaultZone(), view);
        view.bind(controller);

        stage.setTitle("Angkor Mart · Monthly sales (MVC)");
        stage.setScene(new Scene(view.root(), 1100, 700));
        stage.show();

        // the month and the branch may be given on the command line, so that
        // the viewer can open on a given report
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

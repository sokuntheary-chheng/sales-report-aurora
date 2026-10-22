package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.mvp.ReportPresenter;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.Clock;

/**
 * The same screen as {@link ReportApp}, but built as Model View Presenter.
 *
 * <p>Run it with {@code mvn javafx:run
 * -Djavafx.mainClass=edu.itc.salesreport.ui.fx.ReportMvpApp}.
 */
public final class ReportMvpApp extends Application {

    private static final Path DATA_DIR = Path.of("data");

    @Override
    public void start(Stage stage) {
        var model = new SalesModel(new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        var screen = new FxReportScreen();
        var presenter = new ReportPresenter(model, screen, DATA_DIR,
                Clock.systemDefaultZone());
        screen.attach(presenter);

        stage.setTitle("Angkor Mart · Monthly sales (MVP)");
        stage.setScene(new Scene(screen.root(), 1100, 700));
        stage.show();

        var arguments = getParameters().getUnnamed();
        presenter.monthEntered(arguments.isEmpty() ? "2026-09" : arguments.getFirst());
        if (arguments.size() > 1) {
            presenter.branchChosen(arguments.get(1));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

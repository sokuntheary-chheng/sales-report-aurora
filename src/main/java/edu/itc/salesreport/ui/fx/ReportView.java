package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.model.SaleTransaction;
import edu.itc.salesreport.ui.ErrorDisplay;
import edu.itc.salesreport.ui.ReportController;
import edu.itc.salesreport.ui.SalesEvent;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.SalesModelListener;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;

import java.util.List;
import java.util.function.Function;

/**
 * The V of MVC in JavaFX: it observes the model and draws the transactions
 * and the revenue by category.
 *
 * <p>The view adds nothing up and reads no file: every number it shows comes
 * from the model. It only listens and redraws.
 */
public final class ReportView implements SalesModelListener, ErrorDisplay {

    private final SalesModel model;
    private final BorderPane root = new BorderPane();
    private final TextField monthField = new TextField("2026-09");
    private final ComboBox<String> branchBox = new ComboBox<>();
    private final TableView<SaleTransaction> table = new TableView<>();
    private final BarChart<String, Number> chart =
            new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final Label status = new Label("No month loaded");
    private final Label error = new Label();

    public ReportView(SalesModel model) {
        this.model = model;
        branchBox.getItems().setAll("ALL", "PNH", "REP", "BTB");
        branchBox.setValue("ALL");
        table.getColumns().setAll(List.of(
                column("Date", t -> t.date().toString()),
                column("Receipt", SaleTransaction::receiptNo),
                column("Product", SaleTransaction::productName),
                column("Category", SaleTransaction::category),
                column("Qty", t -> String.valueOf(t.quantity())),
                column("Revenue", t -> t.revenue().toPlainString())));
        chart.setTitle("Revenue by category (USD)");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        error.setStyle("-fx-text-fill: firebrick;");
        error.setWrapText(true);

        var filters = new HBox(8,
                new Label("Month"), monthField, new Label("Branch"), branchBox);
        filters.setPadding(new Insets(8));
        filters.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        var centre = new SplitPane(table, chart);
        centre.setDividerPositions(0.6);

        var bottom = new VBox(status, error);
        bottom.setPadding(new Insets(8));
        bottom.setSpacing(4);

        root.setTop(filters);
        root.setCenter(centre);
        root.setBottom(bottom);
        VBox.setVgrow(status, Priority.NEVER);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        model.addListener(this);
    }

    /** The node that holds the whole screen. */
    public Parent root() {
        return root;
    }

    /**
     * Hands the gestures of the screen to the controller.
     *
     * <p>The handlers do nothing but forward the raw text: every rule lives
     * in the controller.
     */
    public void bind(ReportController controller) {
        monthField.setOnAction(event -> controller.monthEntered(monthField.getText()));
        branchBox.setOnAction(event -> controller.branchChosen(branchBox.getValue()));
    }

    @Override
    public void modelChanged(SalesEvent event) {
        branchBox.setValue(model.branch());
        table.getItems().setAll(model.visibleTransactions());
        chart.getData().setAll(series());
        status.setText(statusText(event));
    }

    @Override
    public void showError(String message) {
        error.setText(message);
    }

    @Override
    public void clearError() {
        error.setText("");
    }

    /** One bar series with the revenue of every category. */
    private XYChart.Series<String, Number> series() {
        var series = new XYChart.Series<String, Number>();
        model.summary().revenueByCategory().forEach(
                (category, revenue) -> series.getData()
                        .add(new XYChart.Data<>(category, revenue)));
        return series;
    }

    /** The line below the screen: the figures of the model and the rejects. */
    private String statusText(SalesEvent event) {
        var summary = model.summary();
        var text = switch (event) {
            case SalesEvent.MonthLoaded(var month, var rows, var rejected) ->
                    "%s: %d rows, %d rejected".formatted(month, rows, rejected);
            case SalesEvent.BranchSelected(var branch) ->
                    "%s: %s".formatted(summary.branch(), model.month()
                            .map(month -> month.toString()).orElse("no month"));
        };
        text += " · revenue USD %s · receipts %d"
                .formatted(summary.revenue(), summary.receipts());
        var rejected = model.rejectedRows();
        if (!rejected.isEmpty()) {
            text += System.lineSeparator() + "rejected row: " + rejected.getFirst();
        }
        return text;
    }

    // records have no JavaFX properties: adapt each column with a lambda
    private static TableColumn<SaleTransaction, String> column(String title,
            Function<SaleTransaction, String> value) {
        var col = new TableColumn<SaleTransaction, String>(title);
        col.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        return col;
    }
}

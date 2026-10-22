package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.model.SaleTransaction;
import edu.itc.salesreport.ui.ErrorDisplay;
import edu.itc.salesreport.ui.ReportController;
import edu.itc.salesreport.ui.ReportText;
import edu.itc.salesreport.ui.SalesEvent;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.SalesModelListener;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Function;

/**
 * The same view as {@link ReportView}, but the layout lives in
 * {@code report.fxml}.
 *
 * <p>This class only wires the widgets: it adds nothing up and reads no
 * file, exactly like the view it replaces. The status line comes from
 * {@link ReportText}, so both views say the same thing.
 */
public final class ReportFxmlView implements SalesModelListener, ErrorDisplay {

    @FXML
    private TextField monthField;

    @FXML
    private ComboBox<String> branchBox;

    @FXML
    private TableView<SaleTransaction> table;

    @FXML
    private javafx.scene.chart.BarChart<String, Number> chart;

    @FXML
    private Label status;

    @FXML
    private Label error;

    private SalesModel model;
    private BorderPane root;

    /** Builds the widgets that the file does not fill. */
    @FXML
    private void initialize() {
        branchBox.getItems().setAll("ALL", "PNH", "REP", "BTB");
        branchBox.setValue("ALL");
        table.getColumns().setAll(List.of(
                column("Date", t -> t.date().toString()),
                column("Receipt", SaleTransaction::receiptNo),
                column("Product", SaleTransaction::productName),
                column("Category", SaleTransaction::category),
                column("Qty", t -> String.valueOf(t.quantity())),
                column("Revenue", t -> t.revenue().toPlainString()),
                column("Discount", t -> t.discount().toPlainString())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /**
     * Loads the view and lets it observe the model.
     *
     * @param model the model to observe
     * @return the loaded view
     */
    public static ReportFxmlView load(SalesModel model) {
        var loader = new FXMLLoader(
                ReportFxmlView.class.getResource("report.fxml"));
        try {
            loader.load();
            var view = (ReportFxmlView) loader.getController();
            view.model = model;
            view.root = (BorderPane) loader.getRoot();
            model.addListener(view);
            return view;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read report.fxml", e);
        }
    }

    /** The node that holds the whole screen. */
    public BorderPane root() {
        return root;
    }

    /** The gestures of the screen go to the controller, nothing else. */
    public void bind(ReportController controller) {
        monthField.setOnAction(event -> controller.monthEntered(monthField.getText()));
        branchBox.setOnAction(event -> controller.branchChosen(branchBox.getValue()));
    }

    @Override
    public void modelChanged(SalesEvent event) {
        branchBox.setValue(model.branch());
        table.getItems().setAll(model.visibleTransactions());
        var series = new XYChart.Series<String, Number>();
        model.summary().revenueByCategory().forEach(
                (category, revenue) -> series.getData()
                        .add(new XYChart.Data<>(category, revenue)));
        chart.getData().setAll(series);
        status.setText(ReportText.status(model, event));
    }

    @Override
    public void showError(String message) {
        error.setText(message);
    }

    @Override
    public void clearError() {
        error.setText("");
    }

    private static TableColumn<SaleTransaction, String> column(String title,
            Function<SaleTransaction, String> value) {
        var col = new TableColumn<SaleTransaction, String>(title);
        col.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        return col;
    }
}

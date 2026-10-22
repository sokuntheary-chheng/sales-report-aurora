package edu.itc.salesreport.ui.fx;

import edu.itc.salesreport.ui.mvp.ReportPresenter;
import edu.itc.salesreport.ui.mvp.ReportScreen;
import edu.itc.salesreport.ui.mvp.RowItem;
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
import javafx.scene.layout.VBox;
import javafx.scene.Parent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The JavaFX screen of the MVP variant.
 *
 * <p>The screen is passive: it draws what the presenter pushes and it
 * forwards the gestures of the user. It knows no model and it adds nothing
 * up, so the same screen could show another model.
 */
public final class FxReportScreen implements ReportScreen {

    private final BorderPane root = new BorderPane();
    private final TextField monthField = new TextField("2026-09");
    private final ComboBox<String> branchBox = new ComboBox<>();
    private final TableView<RowItem> table = new TableView<>();
    private final BarChart<String, Number> chart =
            new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final Label status = new Label("No month loaded");
    private final Label error = new Label();

    public FxReportScreen() {
        branchBox.getItems().setAll("ALL", "PNH", "REP", "BTB");
        branchBox.setValue("ALL");
        table.getColumns().setAll(List.of(
                column("Date", RowItem::date),
                column("Receipt", RowItem::receipt),
                column("Product", RowItem::product),
                column("Category", RowItem::category),
                column("Qty", RowItem::quantity),
                column("Revenue", RowItem::revenue),
                column("Discount", RowItem::discount)));
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
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /** The node that holds the whole screen. */
    public Parent root() {
        return root;
    }

    /** The gestures of the user go to the presenter, nothing else. */
    public void attach(ReportPresenter presenter) {
        monthField.setOnAction(event -> presenter.monthEntered(monthField.getText()));
        branchBox.setOnAction(event -> presenter.branchChosen(branchBox.getValue()));
    }

    @Override
    public void showRows(List<RowItem> rows) {
        table.getItems().setAll(rows);
    }

    @Override
    public void showCategoryRevenue(Map<String, BigDecimal> revenue) {
        var series = new XYChart.Series<String, Number>();
        revenue.forEach((category, amount) ->
                series.getData().add(new XYChart.Data<>(category, amount)));
        chart.getData().setAll(series);
    }

    @Override
    public void showStatus(String text) {
        status.setText(text);
    }

    @Override
    public void showError(String message) {
        error.setText(message);
    }

    private static TableColumn<RowItem, String> column(String title,
            Function<RowItem, String> value) {
        var col = new TableColumn<RowItem, String>(title);
        col.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        return col;
    }
}

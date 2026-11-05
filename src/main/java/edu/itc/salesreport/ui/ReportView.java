package edu.itc.salesreport.ui;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Build the controls and bind them. The one piece of logic the view keeps
 * is committing what the user typed into the month box (Enter, focus loss).
 */
public final class ReportView {

    private final BorderPane root = new BorderPane();

    /** The month box; the tests commit into it without opening a window. */
    private final ComboBox<YearMonth> month;

    public ReportView(ReportViewModel vm) {
        month = new ComboBox<>(vm.monthChoices());
        month.setEditable(true);
        month.setConverter(new YearMonthConverter());
        month.valueProperty().bindBidirectional(vm.monthProperty());
        // A typed month does not reach the value by itself: the view has to
        // commit the editor text when Enter is pressed (the key lands on the
        // box or on its editor) ...
        month.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                commitMonth(month);
            }
        });
        // ... and when focus leaves the box or its editor
        month.focusedProperty().addListener(focusLossCommit(month));
        month.getEditor().focusedProperty().addListener(focusLossCommit(month));
        // Task 3: the month box gets a red border while the rule refuses it
        month.styleProperty().bind(Bindings
                .when(vm.monthErrorProperty().isEmpty())
                .then("").otherwise("-fx-border-color: crimson;"));

        HBox filters = new HBox(8, new Label("Month"), month);
        for (String b : ReportViewModel.BRANCHES) {
            CheckBox box = new CheckBox(b);
            box.selectedProperty().bindBidirectional(
                    vm.branchSelectedProperty(b));
            filters.getChildren().add(box);
        }

        Button generate = new Button("Generate");
        generate.setOnAction(e -> vm.generateCommand().execute());
        generate.disableProperty().bind(
                Bindings.not(vm.generateCommand().executableProperty()));
        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> vm.cancelCommand().execute());
        cancel.disableProperty().bind(
                Bindings.not(vm.cancelCommand().executableProperty()));
        filters.getChildren().addAll(generate, cancel);

        ProgressBar bar = new ProgressBar();
        Label validation = new Label();
        Label status = new Label();
        Label error = new Label();
        Label total = new Label();
        bar.progressProperty().bind(vm.progressProperty());
        bar.visibleProperty().bind(vm.loadingProperty());
        validation.textProperty().bind(vm.validationMessageProperty());
        status.textProperty().bind(vm.statusProperty());
        error.textProperty().bind(vm.loadErrorProperty());
        total.textProperty().bind(vm.totalRevenueTextProperty());

        TableView<BranchSummary> table = new TableView<>(vm.summaries());
        table.getColumns().add(column("Branch", BranchSummary::branch));
        table.getColumns().add(column("Revenue",
                s -> s.revenue().toPlainString()));
        table.getColumns().add(column("Discounts",
                s -> s.discounts().toPlainString()));
        table.getColumns().add(column("Receipts",
                s -> String.valueOf(s.receipts())));
        table.getColumns().add(column("Avg basket",
                s -> s.averageBasket().toPlainString()));

        TextField search = new TextField();
        search.setPromptText("Filter products");
        search.textProperty().bindBidirectional(vm.productFilterProperty());
        ListView<ProductTotal> productList =
                new ListView<>(vm.visibleProducts());

        VBox top = new VBox(6, filters, validation,
                new HBox(8, bar, status, error));
        top.setPadding(new Insets(10));
        root.setTop(top);
        root.setCenter(table);
        root.setRight(new VBox(6, new Label("Top products"), search,
                productList));
        root.setBottom(new HBox(8, new Label("Chain revenue:"), total));
    }

    private static TableColumn<BranchSummary, String> column(String title,
            Function<BranchSummary, String> value) {
        TableColumn<BranchSummary, String> c = new TableColumn<>(title);
        c.setCellValueFactory(
                cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return c;
    }

    public Parent root() {
        return root;
    }

    /** The month box; the tests cannot open a window. */
    ComboBox<YearMonth> monthBox() {
        return month;
    }

    /**
     * Commits the typed text of the editor into the value, so the binding
     * passes it on to the ViewModel. Text no rule understands becomes null
     * and the ViewModel answers "Choose a month.".
     */
    static void commitMonth(ComboBox<YearMonth> box) {
        box.setValue(((YearMonthConverter) box.getConverter())
                .fromString(box.getEditor().getText()));
    }

    /** Commits the typed text when focus was on the box and no longer is. */
    static ChangeListener<Boolean> focusLossCommit(ComboBox<YearMonth> box) {
        return (obs, hadFocus, hasFocus) -> {
            if (hadFocus && !hasFocus) {
                commitMonth(box);
            }
        };
    }

    /** Text "2026-09" to YearMonth and back; null for unparsable input. */
    static final class YearMonthConverter extends StringConverter<YearMonth> {
        @Override
        public String toString(YearMonth m) {
            return m == null ? "" : m.toString();
        }

        @Override
        public YearMonth fromString(String s) {
            try {
                return s == null || s.isBlank() ? null
                        : YearMonth.parse(s.strip());
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }
}

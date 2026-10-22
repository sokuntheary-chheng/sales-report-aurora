package edu.itc.salesreport.ui.fx.legacy;

import edu.itc.salesreport.ui.ErrorDisplay;
import edu.itc.salesreport.ui.ReportController;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * The legacy handler of the report screen, after the refactoring.
 *
 * <p>Before the refactoring this class read the files, cut the lines apart,
 * filtered the rows and added up the money inside {@code onLoad()}. All of
 * that now lives in the packages of the brief: {@code ingest} reads and
 * parses, {@code engine} adds up, {@code ui} holds the rules. What is left
 * here is a screen adapter: it forwards what the user did and shows what
 * the controller reports.
 *
 * <p>See {@code lab02/refactoring.md} for the smells and the figures.
 */
public class FatReportController implements ErrorDisplay {

    private final TextField monthField;
    private final ComboBox<String> branchBox;
    private final Label status;
    private final ReportController controller;

    public FatReportController(TextField monthField,
            ComboBox<String> branchBox, Label status,
            ReportController controller) {
        this.monthField = monthField;
        this.branchBox = branchBox;
        this.status = status;
        this.controller = controller;
    }

    /** The month was confirmed: the raw text goes to the controller. */
    public void onLoad() {
        controller.monthEntered(monthField.getText());
    }

    /** Another branch was picked: the raw code goes to the controller. */
    public void onBranchChosen() {
        controller.branchChosen(branchBox.getValue());
    }

    @Override
    public void showError(String message) {
        status.setText(message);
    }

    @Override
    public void clearError() {
        status.setText("");
    }
}

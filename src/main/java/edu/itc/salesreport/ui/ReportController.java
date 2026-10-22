package edu.itc.salesreport.ui;

import edu.itc.salesreport.ui.Validation.Invalid;
import edu.itc.salesreport.ui.Validation.Valid;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Clock;

/**
 * The C of MVC: raw input in, validated commands out.
 *
 * <p>The controller turns the text of the screen into a command, asks the
 * model to carry it out and reports every problem through the
 * {@link ErrorDisplay}. It has no toolkit import and no arithmetic, so it can
 * be tested without a screen.
 */
public final class ReportController {

    private final SalesModel model;
    private final Path dataDir;
    private final Clock clock;
    private final ErrorDisplay errors;

    public ReportController(SalesModel model, Path dataDir, Clock clock,
            ErrorDisplay errors) {
        this.model = model;
        this.dataDir = dataDir;
        this.clock = clock;
        this.errors = errors;
    }

    /** The month field was confirmed. */
    public void monthEntered(String text) {
        switch (InputRules.month(text, clock)) {
            case Valid(var month) -> execute(new ReportCommand.LoadMonth(month));
            case Invalid(var message) -> errors.showError(message);
        }
    }

    /** Another branch was picked. */
    public void branchChosen(String text) {
        switch (InputRules.branch(text)) {
            case Valid(var branch) -> execute(new ReportCommand.SelectBranch(branch));
            case Invalid(var message) -> errors.showError(message);
        }
    }

    /**
     * Carries out one command.
     *
     * <p>Package private on purpose: the controller is the only one that may
     * change the model, and the tests look at it from the same package.
     */
    void execute(ReportCommand command) {
        try {
            switch (command) {
                case ReportCommand.LoadMonth(var month) -> model.load(dataDir, month);
                case ReportCommand.SelectBranch(var branch) -> model.selectBranch(branch);
            }
            errors.clearError();
        } catch (NoSuchFileException e) {
            errors.showError("No sales data found in " + e.getMessage());
        } catch (IOException e) {
            errors.showError("Could not read the sales files: " + e);
        }
    }
}

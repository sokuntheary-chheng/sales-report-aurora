package edu.itc.salesreport.ui;

import java.time.YearMonth;

/**
 * What the controller asks the model to do.
 *
 * <p>A command is a checked intention: the controller only builds one when
 * the input was valid.
 */
public sealed interface ReportCommand {

    /**
     * Load a month.
     *
     * @param month the month to load
     */
    record LoadMonth(YearMonth month) implements ReportCommand {
    }

    /**
     * Show another branch.
     *
     * @param branch the branch code, or {@code ALL} for the whole chain
     */
    record SelectBranch(String branch) implements ReportCommand {
    }
}

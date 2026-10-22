package edu.itc.salesreport.ui;

/**
 * Where the report shows a problem.
 *
 * <p>The controller and the presenter talk to the screen only through this
 * interface, so neither of them needs a toolkit.
 */
public interface ErrorDisplay {

    /** Shows the message that explains what went wrong. */
    void showError(String message);

    /** Removes the message: the last command worked. */
    void clearError();
}

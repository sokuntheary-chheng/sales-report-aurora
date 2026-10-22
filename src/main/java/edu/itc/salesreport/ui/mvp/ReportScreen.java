package edu.itc.salesreport.ui.mvp;

import java.util.List;
import java.util.Map;

/**
 * The passive view: no model access, no formatting, no decisions.
 *
 * <p>The presenter pushes the values that the screen shows; the screen only
 * knows how to draw them.
 */
public interface ReportScreen {

    /** Shows the rows of the report. */
    void showRows(List<RowItem> rows);

    /** Shows the revenue of every category. */
    void showCategoryRevenue(Map<String, java.math.BigDecimal> revenue);

    /** Shows the line with the figures of the report. */
    void showStatus(String text);

    /** Shows the message of a problem, or nothing when there is none. */
    void showError(String message);
}

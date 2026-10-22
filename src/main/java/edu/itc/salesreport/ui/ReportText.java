package edu.itc.salesreport.ui;

/**
 * The text of the report, without a toolkit.
 *
 * <p>The status line is built here so that the JavaFX view and the FXML view
 * say the same thing. The numbers come from the model; this class only puts
 * them into words.
 */
public final class ReportText {

    private ReportText() {
    }

    /**
     * The line below the screen: the figures of the model and the rows that
     * were refused.
     */
    public static String status(SalesModel model, SalesEvent event) {
        var summary = model.summary();
        var text = switch (event) {
            case SalesEvent.MonthLoaded(var month, var rows, var rejected) ->
                    "%s: %d rows, %d rejected".formatted(month, rows, rejected);
            case SalesEvent.BranchSelected(var branch) ->
                    "%s: %s".formatted(summary.branch(),
                            model.month().map(Object::toString).orElse("no month"));
        };
        text += " · revenue USD %s · receipts %d"
                .formatted(summary.revenue(), summary.receipts());
        var rejected = model.rejectedRows();
        if (!rejected.isEmpty()) {
            text += System.lineSeparator() + "rejected row: " + rejected.getFirst();
        }
        return text;
    }
}

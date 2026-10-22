package edu.itc.salesreport.ui.console;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.SalesEvent;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.SalesModelListener;

import java.io.PrintStream;
import java.nio.file.Path;
import java.time.YearMonth;

/**
 * The V of MVC without a toolkit: it prints one line per event and the
 * figures of the selected branch.
 *
 * <p>It proves that the model works before any widget exists, and it stays
 * next to the JavaFX view of Task 2.
 *
 * <p>Run it with {@code java -cp target/classes
 * edu.itc.salesreport.ui.console.ConsoleReportView data}.
 */
public final class ConsoleReportView implements SalesModelListener {

    private final SalesModel model;
    private final PrintStream out;

    /**
     * Builds the view and registers it with the model.
     *
     * @param model the model to observe
     * @param out   where the lines go
     */
    public ConsoleReportView(SalesModel model, PrintStream out) {
        this.model = model;
        this.out = out;
        model.addListener(this);
    }

    @Override
    public void modelChanged(SalesEvent event) {
        out.println(switch (event) {
            case SalesEvent.MonthLoaded(var month, var rows, var rejected) ->
                    "Loaded %s: %d rows, %d rejected".formatted(month, rows, rejected);
            case SalesEvent.BranchSelected(var branch) -> "Branch " + branch;
        });
        var summary = model.summary();
        out.printf("  %-4s revenue %9s  receipts %4d  basket %5s%n",
                summary.branch(), summary.revenue(), summary.receipts(),
                summary.averageBasket());
        summary.revenueByCategory().forEach(
                (category, revenue) -> out.printf("    %-14s %9s%n",
                        category, revenue));
    }

    public static void main(String[] args) throws Exception {
        Path dataDir = Path.of(args.length > 0 ? args[0] : "data");
        var model = new SalesModel(new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        new ConsoleReportView(model, System.out);
        model.load(dataDir, YearMonth.parse("2026-09"));
        model.selectBranch("PNH");
    }
}

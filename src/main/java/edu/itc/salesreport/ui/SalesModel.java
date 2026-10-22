package edu.itc.salesreport.ui;

import edu.itc.salesreport.engine.SalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.MonthData;
import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.SaleTransaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The M of MVC: it loads one month of branch files, keeps the figures of the
 * selected branch and tells its listeners about every change.
 *
 * <p>The model knows nothing about a toolkit: it reads files, adds nothing
 * up itself (the aggregator does that) and publishes events. It is confined
 * to one thread, the thread that drives the user interface.
 */
public final class SalesModel {

    /** The code that stands for the whole chain. */
    public static final String ALL = "ALL";

    private final CsvMonthReader reader;
    private final SalesAggregator aggregator;
    private final List<SalesModelListener> listeners = new CopyOnWriteArrayList<>();

    /** The month that was loaded; null until the first successful load. */
    private MonthData data;

    /** The branch that is shown; the chain until another one is selected. */
    private String branch = ALL;

    public SalesModel(CsvMonthReader reader, SalesAggregator aggregator) {
        this.reader = reader;
        this.aggregator = aggregator;
    }

    public void addListener(SalesModelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SalesModelListener listener) {
        listeners.remove(listener);
    }

    /**
     * Replaces the data with the rows of one month.
     *
     * <p>The month is read before anything changes, so a failed load leaves
     * the old data in place and publishes nothing.
     *
     * @throws IOException if the month cannot be read
     */
    public void load(Path dataDir, YearMonth month) throws IOException {
        MonthData loaded = reader.read(dataDir, month);
        data = loaded;
        fire(new SalesEvent.MonthLoaded(month, loaded.rows().size(),
                loaded.rejected().size()));
    }

    /**
     * Shows another branch. Selecting the branch that is already shown
     * changes nothing and publishes nothing.
     */
    public void selectBranch(String newBranch) {
        if (newBranch.equals(branch)) {
            return;
        }
        branch = newBranch;
        fire(new SalesEvent.BranchSelected(newBranch));
    }

    /** The branch that is shown. */
    public String branch() {
        return branch;
    }

    /** The month that is loaded, if any. */
    public Optional<YearMonth> month() {
        return Optional.ofNullable(data).map(MonthData::month);
    }

    /** The rows of the selected branch; all rows for the whole chain. */
    public List<SaleTransaction> visibleTransactions() {
        if (data == null) {
            return List.of();
        }
        if (ALL.equals(branch)) {
            return data.rows();
        }
        return data.rows().stream()
                .filter(row -> row.branch().equals(branch))
                .toList();
    }

    /** The figures of the selected branch. */
    public BranchSummary summary() {
        return aggregator.summarize(branch, visibleTransactions());
    }

    /** One message per refused row of the loaded month. */
    public List<String> rejectedRows() {
        return data == null ? List.of() : data.rejected();
    }

    private void fire(SalesEvent event) {
        for (SalesModelListener listener : listeners) {
            listener.modelChanged(event);
        }
    }
}

package edu.itc.salesreport.ui.mvp;

import edu.itc.salesreport.model.SaleTransaction;
import edu.itc.salesreport.ui.InputRules;
import edu.itc.salesreport.ui.SalesEvent;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.SalesModelListener;
import edu.itc.salesreport.ui.Validation.Invalid;
import edu.itc.salesreport.ui.Validation.Valid;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.YearMonth;
import java.util.Locale;

/**
 * The MVP presenter: it handles the input and pushes formatted values to the
 * screen.
 *
 * <p>The presenter is the only part of the MVP variant that knows the model
 * and the rules. It holds no toolkit, so every text of the screen can be
 * tested without a window.
 */
public final class ReportPresenter implements SalesModelListener {

    private final SalesModel model;
    private final ReportScreen screen;
    private final Path dataDir;
    private final Clock clock;

    public ReportPresenter(SalesModel model, ReportScreen screen,
            Path dataDir, Clock clock) {
        this.model = model;
        this.screen = screen;
        this.dataDir = dataDir;
        this.clock = clock;
        model.addListener(this);
    }

    /** The month field was confirmed. */
    public void monthEntered(String text) {
        switch (InputRules.month(text, clock)) {
            case Valid(var month) -> load(month);
            case Invalid(var message) -> screen.showError(message);
        }
    }

    /** Another branch was picked. */
    public void branchChosen(String text) {
        switch (InputRules.branch(text)) {
            case Valid(var branch) -> model.selectBranch(branch);
            case Invalid(var message) -> screen.showError(message);
        }
    }

    private void load(YearMonth month) {
        try {
            model.load(dataDir, month);
        } catch (IOException e) {
            screen.showError("No sales data for " + month);
        }
    }

    @Override
    public void modelChanged(SalesEvent event) {
        var summary = model.summary();
        screen.showRows(model.visibleTransactions().stream()
                .map(ReportPresenter::toRow)
                .toList());
        screen.showCategoryRevenue(summary.revenueByCategory());
        screen.showStatus("%s · %d receipts · revenue USD %s · basket USD %s"
                .formatted(summary.branch(), summary.receipts(),
                        money(summary.revenue()),
                        money(summary.averageBasket())));
        screen.showError("");
    }

    /** Money is written the way the report writes it. */
    static String money(BigDecimal amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }

    /** One domain row becomes one row of the screen. */
    private static RowItem toRow(SaleTransaction row) {
        return new RowItem(row.date().toString(), row.receiptNo(),
                row.productName(), row.category(),
                String.valueOf(row.quantity()), row.revenue().toPlainString(),
                row.discount().toPlainString());
    }
}

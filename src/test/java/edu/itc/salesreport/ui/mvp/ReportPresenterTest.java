package edu.itc.salesreport.ui.mvp;

import edu.itc.salesreport.TestData;
import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import edu.itc.salesreport.ui.SalesModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The text of the screen, tested without a window: the screen is replaced by
 * a recording fake.
 */
class ReportPresenterTest {

    /** Records everything the presenter pushes to the screen. */
    static final class FakeScreen implements ReportScreen {
        List<RowItem> rows = List.of();
        Map<String, BigDecimal> categories = Map.of();
        String status = "";
        String error = "";

        @Override
        public void showRows(List<RowItem> rows) {
            this.rows = rows;
        }

        @Override
        public void showCategoryRevenue(Map<String, BigDecimal> revenue) {
            this.categories = revenue;
        }

        @Override
        public void showStatus(String text) {
            this.status = text;
        }

        @Override
        public void showError(String message) {
            this.error = message;
        }
    }

    @TempDir
    Path dataDir;

    private final FakeScreen screen = new FakeScreen();
    private ReportPresenter presenter;

    @BeforeEach
    void setUp() throws IOException {
        TestData.writeSample(dataDir);
        var model = new SalesModel(new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        presenter = new ReportPresenter(model, screen, dataDir,
                Clock.fixed(Instant.parse("2026-09-27T03:00:00Z"),
                        ZoneId.of("Asia/Phnom_Penh")));
    }

    @Test
    void theStatusAndTheRowsAreFormatted() {
        presenter.monthEntered("2026-09");
        presenter.branchChosen("PNH");

        assertEquals("PNH · 1 receipts · revenue USD 31.00 · basket USD 31.00",
                screen.status);
        assertEquals(new RowItem("2026-09-01", "PNH-000001",
                "Power Bank 10000mAh", "Electronics", "1", "18.00", "0.90"),
                screen.rows.get(1));
        assertEquals("", screen.error);
    }

    @Test
    void moneyIsWrittenTheAmericanWay() {
        assertEquals("1,234.50", ReportPresenter.money(new BigDecimal("1234.5")));
    }
}

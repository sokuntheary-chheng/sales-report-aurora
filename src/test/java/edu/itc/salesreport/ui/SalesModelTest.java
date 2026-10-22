package edu.itc.salesreport.ui;

import edu.itc.salesreport.TestData;
import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The model without a screen: four rules of the Lab-02 brief. */
class SalesModelTest {

    @TempDir
    Path dataDir;

    private final List<SalesEvent> events = new ArrayList<>();
    private SalesModel model;

    @BeforeEach
    void setUp() throws IOException {
        TestData.writeSample(dataDir);
        model = new SalesModel(new CsvMonthReader(new CsvTransactionParser()),
                new DefaultSalesAggregator());
        model.addListener(events::add);
    }

    @Test
    void loadFiresMonthLoadedAndAddsUpTheChain() throws IOException {
        model.load(dataDir, YearMonth.of(2026, 9));

        assertEquals(List.of(
                new SalesEvent.MonthLoaded(YearMonth.of(2026, 9), 3, 1)), events);
        assertEquals(new BigDecimal("52.00"), model.summary().revenue());
    }

    @Test
    void selectingTheSameBranchTwiceFiresOnce() throws IOException {
        model.load(dataDir, YearMonth.of(2026, 9));
        events.clear();

        model.selectBranch("PNH");
        model.selectBranch("PNH");

        assertEquals(List.of(new SalesEvent.BranchSelected("PNH")), events);
        var summary = model.summary();
        assertEquals(new BigDecimal("31.00"), summary.revenue());
        assertEquals(new BigDecimal("31.00"), summary.averageBasket());
    }

    @Test
    void aFailedLoadKeepsTheOldRowsAndFiresNothing() throws IOException {
        model.load(dataDir, YearMonth.of(2026, 9));
        events.clear();

        assertThrows(NoSuchFileException.class,
                () -> model.load(dataDir, YearMonth.of(2026, 7)));

        assertTrue(events.isEmpty());
        assertEquals(3, model.visibleTransactions().size());
    }

    @Test
    void aRemovedListenerIsNotCalled() throws IOException {
        var heard = new ArrayList<SalesEvent>();
        SalesModelListener listener = heard::add;
        model.addListener(listener);
        model.removeListener(listener);

        model.load(dataDir, YearMonth.of(2026, 9));

        assertTrue(heard.isEmpty());
    }
}

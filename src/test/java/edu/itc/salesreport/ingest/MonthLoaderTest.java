package edu.itc.salesreport.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonthLoaderTest {

    private static final String GOOD_1 =
            "BTB,2026-09-01,BTB-000001,SKU-1001,Jasmine Rice 5kg,"
            + "Grocery,2,6.50,0.00,KHQR";
    private static final String GOOD_2 =
            "BTB,2026-09-01,BTB-000002,SKU-2001,Mineral Water 1.5L,"
            + "Beverages,1,0.45,0.00,CASH";
    private static final String GOOD_3 =
            "PNH,2026-09-01,PNH-000001,SKU-6001,Notebook A5,"
            + "Stationery,1,0.75,0.00,CARD";
    private static final String BAD =
            "PNH,2026-09-01,PNH-000002,SKU-6002,Ballpoint Pen Blue,"
            + "Stationery,2,0.30,0.00,PAYPAL";

    @TempDir
    Path dataDir;

    private void write(String name, String... rows) throws IOException {
        var dir = Files.createDirectories(dataDir.resolve("2026-09"));
        var lines = new ArrayList<String>();
        lines.add(CsvTransactionParser.HEADER);
        lines.addAll(List.of(rows));
        Files.write(dir.resolve(name), lines);
    }

    @Test
    void publishesThreeEventsInOrderWithCorrectCounts() throws Exception {
        write("BTB-2026-09-01.csv", GOOD_1, GOOD_2);
        write("PNH-2026-09-01.csv", GOOD_3, BAD);

        var loader = new MonthLoader(new CsvTransactionParser());
        var events = new ArrayList<LoadEvent>();
        loader.subscribe(events::add);

        var loaded = loader.load(YearMonth.of(2026, 9), dataDir);

        assertEquals(3, events.size());
        assertInstanceOf(LoadEvent.FileLoaded.class, events.get(0));
        assertInstanceOf(LoadEvent.FileLoaded.class, events.get(1));
        assertInstanceOf(LoadEvent.LoadFinished.class, events.get(2));

        var first = (LoadEvent.FileLoaded) events.get(0);
        assertEquals("BTB-2026-09-01.csv", first.file().getFileName().toString());
        assertEquals(2, first.accepted());
        assertEquals(0, first.rejected());
        assertEquals(1, first.filesDone());
        assertEquals(2, first.filesTotal());

        var second = (LoadEvent.FileLoaded) events.get(1);
        assertEquals("PNH-2026-09-01.csv", second.file().getFileName().toString());
        assertEquals(1, second.accepted());
        assertEquals(1, second.rejected());
        assertEquals(2, second.filesDone());
        assertEquals(2, second.filesTotal());

        var finished = (LoadEvent.LoadFinished) events.get(2);
        assertEquals(3, finished.accepted());
        assertEquals(1, finished.rejected());
        assertNotNull(finished.took());

        assertEquals(3, loaded.size());
        assertEquals(List.of("PNH-2026-09-01.csv:3 bad payment_method 'PAYPAL'"),
                loader.rejectedRows());
    }

    @Test
    void unsubscribedListenerReceivesNothing() throws Exception {
        write("BTB-2026-09-01.csv", GOOD_1);

        var loader = new MonthLoader(new CsvTransactionParser());
        var events = new ArrayList<LoadEvent>();
        Runnable unsubscribe = loader.subscribe(events::add);
        unsubscribe.run();

        loader.load(YearMonth.of(2026, 9), dataDir);

        assertTrue(events.isEmpty());
        assertTrue(loader.rejectedRows().isEmpty());
    }

}

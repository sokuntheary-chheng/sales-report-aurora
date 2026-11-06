package edu.itc.salesreport.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The month reader: the whole month, or only one branch's files. */
class CsvMonthReaderTest {

    private static final String PNH_ROW =
            "PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,"
            + "Grocery,2,6.50,0.00,KHQR";
    private static final String REP_ROW =
            "REP,2026-09-01,REP-000001,SKU-2001,Mineral Water 1.5L,"
            + "Beverages,1,0.45,0.00,CASH";
    private static final String BTB_ROW =
            "BTB,2026-09-01,BTB-000001,SKU-6001,Notebook A5,"
            + "Stationery,1,0.75,0.00,CARD";

    @TempDir
    Path dataDir;

    private void write(String name, String row) throws IOException {
        var dir = Files.createDirectories(dataDir.resolve("2026-09"));
        Files.write(dir.resolve(name),
                List.of(CsvTransactionParser.HEADER, row));
    }

    @Test
    void readBranchKeepsOnlyTheFilesOfThatBranch() throws Exception {
        write("PNH-2026-09-01.csv", PNH_ROW);
        write("REP-2026-09-01.csv", REP_ROW);
        write("BTB-2026-09-01.csv", BTB_ROW);
        var reader = new CsvMonthReader(new CsvTransactionParser());

        var pnh = reader.readBranch(dataDir, YearMonth.of(2026, 9), "PNH");

        var branches = new ArrayList<String>();
        pnh.rows().forEach(row -> branches.add(row.branch()));
        assertEquals(List.of("PNH"), branches);
        assertEquals(1, pnh.rows().size());
    }

    @Test
    void readWithoutBranchKeepsEveryFileOfTheMonth() throws Exception {
        write("PNH-2026-09-01.csv", PNH_ROW);
        write("REP-2026-09-01.csv", REP_ROW);
        write("BTB-2026-09-01.csv", BTB_ROW);
        var reader = new CsvMonthReader(new CsvTransactionParser());

        var all = reader.read(dataDir, YearMonth.of(2026, 9));

        assertEquals(3, all.rows().size());
    }
}

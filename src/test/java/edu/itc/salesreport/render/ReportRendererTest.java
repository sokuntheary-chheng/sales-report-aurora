package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportRendererTest {

    private String render(String ext) {
        return new String(ReportRenderer.forExtension(ext)
                .render(TestReports.september()), StandardCharsets.UTF_8);
    }

    @ParameterizedTest
    @ValueSource(strings = {"txt", "html", "csv"})
    void rendersEveryBranchAndTheChainTotal(String ext) {
        var out = render(ext);
        assertTrue(out.contains("PNH"), ext + " misses PNH");
        assertTrue(out.contains("REP"), ext + " misses REP");
        assertTrue(out.contains("BTB"), ext + " misses BTB");
        assertTrue(out.contains("ALL"), ext + " misses the chain");
        assertTrue(out.contains("2300.50"), ext + " misses the chain total");
    }

    @Test
    void csvLineForRepMatchesExactly() {
        var lines = render("csv").split("\n");
        assertEquals("month,branch,revenue,discounts,receipts,average_basket",
                lines[0]);
        assertEquals("2026-09,PNH,1200.00,0.00,100,12.00", lines[1]);
        assertEquals("2026-09,REP,800.50,0.00,50,16.01", lines[2]);
        assertEquals("2026-09,BTB,300.00,0.00,40,7.50", lines[3]);
        assertEquals("2026-09,ALL,2300.50,0.00,190,12.11", lines[4]);
    }

    @Test
    void htmlEscapesAmpersandAndAngleBrackets() {
        var weird = new BranchSummary("A&B <X>", new BigDecimal("10.00"),
                BigDecimal.ZERO, 1, Map.of(), List.of(), Map.of());
        var report = new MonthlyReport(YearMonth.of(2026, 9), List.of(weird),
                weird, List.of());
        var out = new String(new HtmlReportRenderer().render(report),
                StandardCharsets.UTF_8);
        assertTrue(out.contains("A&amp;B &lt;X&gt;"));
        assertFalse(out.contains("A&B <X>"));
    }

    @Test
    void forExtensionRejectsPdf() {
        assertThrows(IllegalArgumentException.class,
                () -> ReportRenderer.forExtension("pdf"));
    }

    @Test
    void switchOverRendererIsExhaustiveWithoutDefault() {
        var extensions = new ArrayList<String>();
        for (ReportRenderer renderer : List.of(new TextReportRenderer(),
                new HtmlReportRenderer(), new CsvReportRenderer())) {
            switch (renderer) {
                case TextReportRenderer text -> extensions.add(text.extension());
                case HtmlReportRenderer html -> extensions.add(html.extension());
                case CsvReportRenderer csv -> extensions.add(csv.extension());
            }
        }
        assertEquals(List.of("txt", "html", "csv"), extensions);
    }

}

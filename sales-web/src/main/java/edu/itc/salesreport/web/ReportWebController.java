package edu.itc.salesreport.web;

import edu.itc.salesreport.engine.SalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ui.InputRules;
import edu.itc.salesreport.ui.SalesModel;
import edu.itc.salesreport.ui.Validation;
import edu.itc.salesreport.ui.Validation.Invalid;
import edu.itc.salesreport.ui.Validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

/**
 * The one page of the report.
 *
 * <p>The controller does what the desktop controller does: it validates the
 * parameters with the same {@link InputRules}, it builds a
 * {@link SalesModel} for the request and it hands the figures to the
 * template. It adds no arithmetic of its own.
 */
@Controller
public class ReportWebController {

    /** The two parameters of the page, already checked. */
    private record Inputs(Validation<YearMonth> month, Validation<String> branch) {
    }

    private final CsvMonthReader reader;
    private final SalesAggregator aggregator;
    private final Clock clock;
    private final Path dataDir;

    public ReportWebController(CsvMonthReader reader, SalesAggregator aggregator,
            Clock clock, @Value("${salesreport.data-dir:data}") String dataDir) {
        this.reader = reader;
        this.aggregator = aggregator;
        this.clock = clock;
        // a String, not a Path: Spring would resolve a relative Path
        // against the servlet context instead of the working directory
        this.dataDir = Path.of(dataDir);
    }

    @GetMapping("/report")
    public String report(@RequestParam(defaultValue = "2026-09") String month,
            @RequestParam(defaultValue = "ALL") String branch, Model page) {
        page.addAttribute("month", month);
        page.addAttribute("branch", branch);
        page.addAttribute("branches", List.of("ALL", "PNH", "REP", "BTB"));
        var inputs = new Inputs(InputRules.month(month, clock),
                InputRules.branch(branch));
        switch (inputs) {
            case Inputs(Valid<YearMonth>(var m), Valid<String>(var b)) ->
                    show(m, b, page);
            case Inputs(Invalid<YearMonth>(var message), Valid<String>(var b)) ->
                    page.addAttribute("error", message);
            case Inputs(Valid<YearMonth>(var m), Invalid<String>(var message)) ->
                    page.addAttribute("error", message);
            case Inputs(Invalid<YearMonth>(var monthMessage),
                    Invalid<String>(var branchMessage)) ->
                    page.addAttribute("error", monthMessage + " " + branchMessage);
        }
        return "report";
    }

    private void show(YearMonth month, String branch, Model page) {
        var model = new SalesModel(reader, aggregator);
        try {
            model.load(dataDir, month);
            model.selectBranch(branch);
        } catch (IOException e) {
            page.addAttribute("error",
                    "No sales data found in " + e.getMessage());
            return;
        }
        page.addAttribute("summary", model.summary());
        page.addAttribute("rejected", model.rejectedRows());
    }
}

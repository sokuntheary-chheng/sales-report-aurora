package edu.itc.salesreport.model;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/** The finished report for one month: every branch plus the chain total. */
public record MonthlyReport(YearMonth month, List<BranchSummary> branches,
        BranchSummary chain, List<String> anomalies) {

    public MonthlyReport {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(branches, "branches");
        Objects.requireNonNull(chain, "chain");
        Objects.requireNonNull(anomalies, "anomalies");
        branches = List.copyOf(branches);
        anomalies = List.copyOf(anomalies);
    }

}

package edu.itc.salesreport.engine;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.SaleTransaction;

import java.util.List;

/** Adds up the figures of a branch, or of the whole chain. */
public interface SalesAggregator {

    /**
     * Summarizes the rows of one branch.
     *
     * @param branch the branch code, or {@code ALL} for the whole chain
     * @param rows   the rows to add up
     */
    BranchSummary summarize(String branch, List<SaleTransaction> rows);
}

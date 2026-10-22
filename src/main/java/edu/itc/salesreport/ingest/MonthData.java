package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;

import java.time.YearMonth;
import java.util.List;

/**
 * One month of sales: the rows that were accepted and the rows that were
 * refused.
 *
 * @param month    the month the rows belong to
 * @param rows     the accepted rows, in the order they were read
 * @param rejected one message per refused row, "file:line reason"
 */
public record MonthData(YearMonth month, List<SaleTransaction> rows,
                         List<String> rejected) {

    public MonthData {
        rows = List.copyOf(rows);
        rejected = List.copyOf(rejected);
    }
}

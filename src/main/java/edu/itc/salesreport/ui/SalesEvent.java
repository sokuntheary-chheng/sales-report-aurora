package edu.itc.salesreport.ui;

import java.time.YearMonth;

/**
 * Everything the model tells its listeners.
 *
 * <p>The model never returns a value to the view: it publishes an event and
 * the view reads the new state from the model.
 */
public sealed interface SalesEvent {

    /**
     * A month was loaded.
     *
     * @param month    the month that was read
     * @param rows     the number of rows that were accepted
     * @param rejected the number of rows that were refused
     */
    record MonthLoaded(YearMonth month, int rows, int rejected)
            implements SalesEvent {
    }

    /**
     * Another branch was selected.
     *
     * @param branch the branch code, or {@code ALL} for the whole chain
     */
    record BranchSelected(String branch) implements SalesEvent {
    }
}

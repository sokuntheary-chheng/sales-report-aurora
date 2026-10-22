package edu.itc.salesreport.ui.mvp;

/**
 * One row of the report, already formatted by the presenter.
 *
 * <p>The record holds strings, not domain types: the screen must not know
 * what a {@code BigDecimal} or a {@code LocalDate} is.
 *
 * @param date     the day of the transaction
 * @param receipt  the number of the receipt
 * @param product  the name of the product
 * @param category the category of the product
 * @param quantity the number of units
 * @param revenue  the revenue of the line
 * @param discount the discount of the line
 */
public record RowItem(String date, String receipt, String product,
                      String category, String quantity, String revenue,
                      String discount) {
}

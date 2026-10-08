package edu.itc.salesreport.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SaleTransactionTest {

    private static SaleTransaction sale(String quantity, String unitPrice,
            String discount) {
        return new SaleTransaction("PNH", LocalDate.of(2026, 9, 1),
                "PNH-000123", "SKU-6001", "Notebook A5", "Stationery",
                Integer.parseInt(quantity), new BigDecimal(unitPrice),
                new BigDecimal(discount), PaymentMethod.CASH);
    }

    @Test
    void revenueRoundsHalfUpAtScaleTwo() {
        assertEquals(new BigDecimal("1.01"), sale("3", "0.335", "0.00").revenue());
        assertEquals(new BigDecimal("13.00"), sale("2", "6.50", "0.00").revenue());
        assertEquals(new BigDecimal("2.60"), sale("3", "0.90", "0.10").revenue());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(NullPointerException.class,
                () -> new SaleTransaction(null, LocalDate.of(2026, 9, 1),
                        "PNH-000123", "SKU-6001", "Notebook A5", "Stationery",
                        1, BigDecimal.ONE, BigDecimal.ZERO, PaymentMethod.CASH));
        assertThrows(IllegalArgumentException.class, () -> sale("0", "6.50", "0.00"));
        assertThrows(IllegalArgumentException.class, () -> sale("-2", "6.50", "0.00"));
        assertThrows(IllegalArgumentException.class, () -> sale("2", "-6.50", "0.00"));
        assertThrows(IllegalArgumentException.class, () -> sale("2", "6.50", "-0.10"));
        assertThrows(IllegalArgumentException.class, () -> sale("2", "6.50", "20.00"));
        var exactDiscount = sale("2", "6.50", "13.00");
        assertEquals(new BigDecimal("0.00"), exactDiscount.revenue());
    }

    @Test
    void unmodifiableCollectionsAndAverageBasket() {
        var products = List.of(new ProductTotal("SKU-6001", "Notebook A5",
                new BigDecimal("300.00"), 400L));
        var summary = new BranchSummary("PNH", new BigDecimal("800.50"),
                new BigDecimal("10.00"), 50, Map.of("Grocery", new BigDecimal("400.25")),
                products, Map.of(PaymentMethod.KHQR, new BigDecimal("800.50")));

        assertThrows(UnsupportedOperationException.class,
                () -> summary.topProducts().add(products.getFirst()));
        assertThrows(UnsupportedOperationException.class,
                () -> summary.revenueByCategory().put("Toys", BigDecimal.ZERO));
        assertThrows(UnsupportedOperationException.class,
                () -> summary.revenueByPayment().put(PaymentMethod.CASH, BigDecimal.ZERO));

        assertEquals(new BigDecimal("16.01"), summary.averageBasket());
        assertEquals(new BigDecimal("0.00"),
                new BranchSummary("REP", BigDecimal.ZERO, BigDecimal.ZERO, 0,
                        Map.of(), List.of(), Map.of()).averageBasket());

        var report = new MonthlyReport(YearMonth.of(2026, 9), List.of(summary),
                summary, List.of("REP: 1 rejected row"));
        assertThrows(UnsupportedOperationException.class,
                () -> report.branches().add(summary));
        assertThrows(UnsupportedOperationException.class,
                () -> report.anomalies().add("another"));
    }

}

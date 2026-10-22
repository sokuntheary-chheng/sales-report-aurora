package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Everything one branch (or the chain) contributes to a monthly report. */
public record BranchSummary(String branch, BigDecimal revenue,
        BigDecimal discounts, long receipts,
        Map<String, BigDecimal> revenueByCategory,
        List<ProductTotal> topProducts,
        Map<PaymentMethod, BigDecimal> revenueByPayment) {

    public BranchSummary {
        Objects.requireNonNull(branch, "branch");
        Objects.requireNonNull(revenue, "revenue");
        Objects.requireNonNull(discounts, "discounts");
        Objects.requireNonNull(revenueByCategory, "revenueByCategory");
        Objects.requireNonNull(topProducts, "topProducts");
        Objects.requireNonNull(revenueByPayment, "revenueByPayment");
        // the report shows the categories by name and the payments in the
        // order of the enum, so the order of the maps is kept
        revenueByCategory = Collections.unmodifiableMap(
                new TreeMap<>(revenueByCategory));
        topProducts = List.copyOf(topProducts);
        var payments = new EnumMap<PaymentMethod, BigDecimal>(
                PaymentMethod.class);
        payments.putAll(revenueByPayment);
        revenueByPayment = Collections.unmodifiableMap(payments);
    }

    /** revenue / receipts, scale 2, 0.00 when there are no receipts. */
    public BigDecimal averageBasket() {
        if (receipts == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return revenue.divide(BigDecimal.valueOf(receipts), 2, RoundingMode.HALF_UP);
    }

}

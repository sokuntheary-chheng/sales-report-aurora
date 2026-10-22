package edu.itc.salesreport.engine;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.ProductTotal;
import edu.itc.salesreport.model.SaleTransaction;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The figures of a branch: revenue, discounts, receipts, revenue by category,
 * revenue by payment method and the ten products with the most revenue.
 *
 * <p>The aggregator owns the arithmetic, so no view and no controller has to
 * add anything up. The category map stays sorted by name and the payment map
 * stays in the order of the enum, because the report shows them in that order.
 */
public final class DefaultSalesAggregator implements SalesAggregator {

    /** How many products the summary shows. */
    private static final int TOP_PRODUCTS = 10;

    @Override
    public BranchSummary summarize(String branch, List<SaleTransaction> rows) {
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        BigDecimal discounts = BigDecimal.ZERO.setScale(2);
        Set<String> receipts = new HashSet<>();
        var byCategory = new TreeMap<String, BigDecimal>();
        var byPayment = new EnumMap<PaymentMethod, BigDecimal>(PaymentMethod.class);
        var productRevenue = new HashMap<String, BigDecimal>();
        var productQuantity = new HashMap<String, Long>();
        var productName = new HashMap<String, String>();

        for (SaleTransaction row : rows) {
            revenue = revenue.add(row.revenue());
            discounts = discounts.add(row.discount());
            receipts.add(row.receiptNo());
            byCategory.merge(row.category(), row.revenue(), BigDecimal::add);
            byPayment.merge(row.paymentMethod(), row.revenue(), BigDecimal::add);
            productRevenue.merge(row.sku(), row.revenue(), BigDecimal::add);
            productQuantity.merge(row.sku(), (long) row.quantity(), Long::sum);
            productName.put(row.sku(), row.productName());
        }

        var topProducts = productRevenue.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<String, BigDecimal> entry)
                                -> entry.getValue())
                        .reversed()
                        .thenComparing(entry -> entry.getKey()))
                .limit(TOP_PRODUCTS)
                .map(entry -> new ProductTotal(entry.getKey(),
                        productName.get(entry.getKey()),
                        entry.getValue(),
                        productQuantity.get(entry.getKey())))
                .toList();

        return new BranchSummary(branch, revenue, discounts, receipts.size(),
                byCategory, topProducts, byPayment);
    }
}

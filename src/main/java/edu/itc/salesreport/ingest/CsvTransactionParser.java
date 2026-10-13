package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.PaymentMethod;
import edu.itc.salesreport.model.SaleTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Function;

/** Parses the canonical Angkor Mart POS export (10 fields, no quoting). */
public final class CsvTransactionParser implements TransactionParser {

    public static final String HEADER = "branch,date,receipt_no,sku,"
            + "product_name,category,quantity,unit_price,discount,"
            + "payment_method";

    private static final Set<String> BRANCHES = Set.of("PNH", "REP", "BTB");
    private static final Set<String> CATEGORIES = Set.of("Grocery", "Beverages",
            "Household", "Personal Care", "Electronics", "Stationery");

    @Override
    public SaleTransaction parse(String line) throws InvalidRowException {
        if (line == null || line.isBlank()) {
            throw new InvalidRowException("empty line");
        }
        String[] f = line.split(",", -1);
        if (f.length != 10) {
            throw new InvalidRowException(
                    "expected 10 fields but found " + f.length);
        }
        String branch = f[0].strip();
        if (!BRANCHES.contains(branch)) {
            throw new InvalidRowException("unknown branch '" + branch + "'");
        }
        String category = f[5].strip();
        if (!CATEGORIES.contains(category)) {
            throw new InvalidRowException("unknown category '" + category + "'");
        }
        LocalDate date = field("date", f[1], LocalDate::parse);
        int quantity = field("quantity", f[6], Integer::parseInt);
        BigDecimal unitPrice = field("unit_price", f[7], BigDecimal::new);
        BigDecimal discount = field("discount", f[8], BigDecimal::new);
        PaymentMethod paymentMethod =
                field("payment_method", f[9], PaymentMethod::valueOf);

        if (quantity <= 0) {
            throw new InvalidRowException(
                    "bad quantity '" + f[6].strip() + "': must be greater than 0");
        }
        if (unitPrice.signum() < 0) {
            throw new InvalidRowException(
                    "bad unit_price '" + f[7].strip() + "': must not be negative");
        }
        if (discount.signum() < 0) {
            throw new InvalidRowException(
                    "bad discount '" + f[8].strip() + "': must not be negative");
        }
        if (discount.compareTo(BigDecimal.valueOf(quantity).multiply(unitPrice)) > 0) {
            throw new InvalidRowException(
                    "bad discount '" + f[8].strip() + "': exceeds the line total");
        }
        try {
            return new SaleTransaction(branch, date, f[2].strip(), f[3].strip(),
                    f[4].strip(), category, quantity, unitPrice, discount,
                    paymentMethod);
        } catch (IllegalArgumentException e) {
            throw new InvalidRowException(e.getMessage(), e);
        }
    }

    /** Converts one field; any RuntimeException becomes a checked error. */
    private static <T> T field(String name, String raw,
            Function<String, T> convert) throws InvalidRowException {
        try {
            return convert.apply(raw.strip());
        } catch (RuntimeException e) {
            throw new InvalidRowException(
                    "bad " + name + " '" + raw + "'", e);
        }
    }

}

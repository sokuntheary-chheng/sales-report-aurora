package edu.itc.salesreport.ingest;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Writes the deterministic sample months of the Angkor Mart chain.
 *
 * <p>The generator is seeded, so every run produces the same files, the same
 * 3,600 lines for 2026-09 and the same totals. The 2026-09 figures are the
 * ones published in the Lab-02 brief (chain revenue USD 26,721.90 in 1,493
 * receipts; PNH USD 8,455.90 in 489 receipts), so the console view, the
 * JavaFX viewer and the Spring MVC page all show the same numbers.
 *
 * <p>How a month is built:
 * <ol>
 *   <li>every category of a branch gets a target revenue (the published
 *       figures) and a share of the 40 lines per branch file;</li>
 *   <li>the lines are drawn from the product catalogue with a quantity of
 *       1..4 units and the occasional small discount;</li>
 *   <li>the drawn lines are <em>calibrated</em>: quantities are adjusted until
 *       the category revenue is the target revenue to the cent, and the last
 *       cent is carried by one discount;</li>
 *   <li>the lines of a day are grouped into receipts (40 lines in 16 or 17
 *       receipts) and written to {@code data/<yyyy-MM>/<BRANCH>-<date>.csv}.</li>
 * </ol>
 *
 * <p>Run it with {@code java -cp target/classes
 * edu.itc.salesreport.ingest.SalesDataGenerator data}.
 */
public final class SalesDataGenerator {

    /** Same seed everywhere: the files and the totals never change. */
    private static final long SEED = 20260902L;

    /** One branch file holds 40 transaction lines. */
    private static final int LINES_PER_FILE = 40;

    /** Quantities of one line and their probability. */
    private static final int[] QUANTITIES = {1, 2, 3, 4};
    private static final double[] QUANTITY_WEIGHTS = {0.20, 0.30, 0.30, 0.20};

    /** The small discounts a POS line may carry. */
    private static final List<BigDecimal> DISCOUNTS = List.of(
            new BigDecimal("0.10"), new BigDecimal("0.20"), new BigDecimal("0.50"));

    /** Payment methods and their probability. */
    private static final List<String> PAYMENTS = List.of("CASH", "CARD", "KHQR");

    /** The catalogue: sku, product name, category, unit price, row weight. */
    private record Product(String sku, String name, String category,
                           BigDecimal price, double weight) {}

    /** One generated line, before it is written to a CSV file. */
    private static final class Line {
        private final Product product;
        private int quantity;
        private BigDecimal discount;
        /** A line the brief shows verbatim: the calibration leaves it alone. */
        private boolean fixed;

        private Line(Product product, int quantity, BigDecimal discount) {
            this.product = product;
            this.quantity = quantity;
            this.discount = discount;
        }

        private BigDecimal revenue() {
            return product.price()
                    .multiply(BigDecimal.valueOf(quantity))
                    .subtract(discount)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    /** The published 2026-09 revenue per branch and category, in USD. */
    private static final Map<String, Map<String, BigDecimal>> SEPTEMBER = Map.of(
            "PNH", Map.of(
                    "Beverages", new BigDecimal("402.30"),
                    "Electronics", new BigDecimal("3404.50"),
                    "Grocery", new BigDecimal("1281.80"),
                    "Household", new BigDecimal("1094.40"),
                    "Personal Care", new BigDecimal("1801.20"),
                    "Stationery", new BigDecimal("471.70")),
            "REP", Map.of(
                    "Beverages", new BigDecimal("365.20"),
                    "Electronics", new BigDecimal("3890.00"),
                    "Grocery", new BigDecimal("1420.00"),
                    "Household", new BigDecimal("1155.50"),
                    "Personal Care", new BigDecimal("1860.00"),
                    "Stationery", new BigDecimal("453.00")),
            "BTB", Map.of(
                    "Beverages", new BigDecimal("363.90"),
                    "Electronics", new BigDecimal("3878.90"),
                    "Grocery", new BigDecimal("1412.50"),
                    "Household", new BigDecimal("1155.00"),
                    "Personal Care", new BigDecimal("1859.10"),
                    "Stationery", new BigDecimal("452.90")));

    /** The published number of receipts per branch in 2026-09. */
    private static final Map<String, Integer> SEPTEMBER_RECEIPTS =
            Map.of("PNH", 489, "REP", 502, "BTB", 502);

    private static final List<String> CATEGORIES = List.of(
            "Beverages", "Electronics", "Grocery", "Household",
            "Personal Care", "Stationery");

    private static final List<String> BRANCHES = List.of("PNH", "REP", "BTB");

    private static final List<Product> CATALOGUE = List.of(
            new Product("SKU-1001", "Jasmine Rice 5kg", "Grocery",
                    new BigDecimal("6.50"), 0.60),
            new Product("SKU-1002", "Fish Sauce 700ml", "Grocery",
                    new BigDecimal("1.80"), 0.40),
            new Product("SKU-2001", "Drinking Water 1.5L", "Beverages",
                    new BigDecimal("0.60"), 0.60),
            new Product("SKU-2002", "Iced Coffee Can", "Beverages",
                    new BigDecimal("0.90"), 0.40),
            new Product("SKU-3001", "Dish Soap 500ml", "Household",
                    new BigDecimal("1.25"), 0.55),
            new Product("SKU-3002", "Laundry Powder 1kg", "Household",
                    new BigDecimal("2.95"), 0.45),
            new Product("SKU-4001", "Toothpaste 150g", "Personal Care",
                    new BigDecimal("1.60"), 0.15),
            new Product("SKU-4002", "Shampoo 400ml", "Personal Care",
                    new BigDecimal("3.80"), 0.30),
            new Product("SKU-4003", "Sunscreen SPF50", "Personal Care",
                    new BigDecimal("7.50"), 0.55),
            new Product("SKU-5001", "USB-C Cable 1m", "Electronics",
                    new BigDecimal("4.50"), 0.35),
            new Product("SKU-5002", "Power Bank 10000mAh", "Electronics",
                    new BigDecimal("18.90"), 0.65),
            new Product("SKU-6001", "Notebook A5", "Stationery",
                    new BigDecimal("0.75"), 0.60),
            new Product("SKU-6002", "Ballpoint Pen Blue", "Stationery",
                    new BigDecimal("0.30"), 0.40));

    private SalesDataGenerator() {
    }

    /**
     * Writes the sample months below {@code dataDir}.
     *
     * @param args the first argument is the data directory, default {@code data}
     */
    public static void main(String[] args) throws IOException {
        Path dataDir = Path.of(args.length > 0 ? args[0] : "data");
        writeMonth(dataDir, YearMonth.of(2026, 9), SEPTEMBER, SEPTEMBER_RECEIPTS);
        writeMonth(dataDir, YearMonth.of(2026, 8),
                scaleTargets(SEPTEMBER, 31.0 / 30.0),
                scaleReceipts(SEPTEMBER_RECEIPTS, 31.0 / 30.0));
    }

    /** Scales a table of targets to another month length. */
    private static Map<String, Map<String, BigDecimal>> scaleTargets(
            Map<String, Map<String, BigDecimal>> table, double factor) {
        var scaled = new LinkedHashMap<String, Map<String, BigDecimal>>();
        table.forEach((branch, categories) -> {
            var copy = new LinkedHashMap<String, BigDecimal>();
            categories.forEach((category, amount) ->
                    copy.put(category, amount.multiply(BigDecimal.valueOf(factor))
                            .setScale(2, RoundingMode.HALF_UP)));
            scaled.put(branch, copy);
        });
        return scaled;
    }

    /** Scales a table of receipt counts to another month length. */
    private static Map<String, Integer> scaleReceipts(Map<String, Integer> table,
            double factor) {
        var scaled = new LinkedHashMap<String, Integer>();
        table.forEach((branch, count) ->
                scaled.put(branch, (int) Math.round(count * factor)));
        return scaled;
    }

    /** Writes every branch file of one month. */
    private static void writeMonth(Path dataDir, YearMonth month,
            Map<String, Map<String, BigDecimal>> targets,
            Map<String, Integer> receipts) throws IOException {
        Path dir = Files.createDirectories(dataDir.resolve(month.toString()));
        System.out.printf("%s: %d files%n", month, month.lengthOfMonth() * 3);
        for (int b = 0; b < BRANCHES.size(); b++) {
            String branch = BRANCHES.get(b);
            var summary = writeBranch(dir, month, branch,
                    targets.get(branch), receipts.get(branch),
                    new Random(SEED + 1000L * b + month.getMonthValue()));
            System.out.printf("  %s: %d lines, %d receipts, revenue USD %s%n",
                    branch, summary.lines(), summary.receipts(),
                    summary.revenue().setScale(2, RoundingMode.HALF_UP));
        }
    }

    /** What one branch file set adds up to. */
    private record Summary(int lines, int receipts, BigDecimal revenue) {}

    /**
     * Writes the 40-lines-per-day files of one branch and calibrates them so
     * that every category shows the target revenue.
     */
    private static Summary writeBranch(Path dir, YearMonth month, String branch,
            Map<String, BigDecimal> targets, int receiptCount, Random random)
            throws IOException {
        int days = month.lengthOfMonth();
        int[] linesPerCategory = planLines(targets, days * LINES_PER_FILE);
        int[][] perDay = apportion(linesPerCategory, days);
        int[] receiptsPerDay = split(receiptCount, days);

        var daysLines = new ArrayList<List<Line>>();
        var lines = new ArrayList<Line>();
        for (int day = 0; day < month.lengthOfMonth(); day++) {
            var dayLines = new ArrayList<Line>();
            for (int c = 0; c < CATEGORIES.size(); c++) {
                for (int i = 0; i < perDay[day][c]; i++) {
                    dayLines.add(draw(CATEGORIES.get(c), random));
                }
            }
            Collections.shuffle(dayLines, random);
            // The brief shows the first two lines of PNH-2026-09-01.csv; the
            // generator writes exactly that receipt.
            boolean sample = month.equals(YearMonth.of(2026, 9))
                    && branch.equals("PNH") && day == 0;
            if (sample) {
                dayLines.remove(dayLines.size() - 1);
                dayLines.remove(dayLines.size() - 1);
                var shampoo = new Line(product("SKU-4002"), 3, BigDecimal.ZERO);
                var water = new Line(product("SKU-2001"), 2, BigDecimal.ZERO);
                shampoo.fixed = true;
                water.fixed = true;
                dayLines.add(0, shampoo);
                dayLines.add(1, water);
            }
            daysLines.add(dayLines);
            lines.addAll(dayLines);
        }

        calibrate(lines, targets);

        int receiptNumber = 0;
        for (int day = 0; day < daysLines.size(); day++) {
            boolean sample = month.equals(YearMonth.of(2026, 9))
                    && branch.equals("PNH") && day == 0;
            receiptNumber = writeFile(dir, month, branch, day, daysLines.get(day),
                    receiptsPerDay[day], sample ? 2 : 0, receiptNumber, random);
        }

        BigDecimal revenue = BigDecimal.ZERO;
        for (Line line : lines) {
            revenue = revenue.add(line.revenue());
        }
        return new Summary(lines.size(), receiptCount, revenue);
    }

    /**
     * Decides how many lines every category gets. A category keeps its share
     * of the month, and the month keeps its number of lines.
     */
    private static int[] planLines(Map<String, BigDecimal> targets, int totalLines) {
        int[] plan = new int[CATEGORIES.size()];
        int sum = 0;
        for (int c = 0; c < CATEGORIES.size(); c++) {
            String category = CATEGORIES.get(c);
            plan[c] = Math.max(1, (int) Math.round(
                    targets.get(category).doubleValue()
                            / expectedLineValue(category)));
            sum += plan[c];
        }
        // the month has exactly totalLines lines: hand the difference to the
        // categories that hold the most lines, one line at a time
        int index = 0;
        while (sum < totalLines) {
            plan[index % plan.length]++;
            sum++;
            index++;
        }
        while (sum > totalLines) {
            int widest = 0;
            for (int c = 1; c < plan.length; c++) {
                if (plan[c] > plan[widest]) {
                    widest = c;
                }
            }
            if (plan[widest] <= 1) {
                break;
            }
            plan[widest]--;
            sum--;
        }
        return plan;
    }

    /** The revenue one drawn line of a category is expected to carry. */
    private static double expectedLineValue(String category) {
        double value = 0;
        for (Product product : products(category)) {
            value += product.weight()
                    * (2.5 * product.price().doubleValue() - 0.05);
        }
        return value;
    }

    /**
     * Splits the lines of a branch over the days of the month: every day gets
     * exactly {@value #LINES_PER_FILE} lines and every category keeps its
     * share of the month.
     *
     * <p>First every category is spread evenly over the days, then the days
     * that are one line too heavy or too light give or take a line.
     */
    private static int[][] apportion(int[] linesPerCategory, int days) {
        int[][] plan = new int[days][linesPerCategory.length];
        for (int c = 0; c < linesPerCategory.length; c++) {
            int base = linesPerCategory[c] / days;
            int rest = linesPerCategory[c] % days;
            for (int day = 0; day < days; day++) {
                // the days that get one line more rotate over the month
                int turn = ((day - c) % days + days) % days;
                plan[day][c] = base + (turn < rest ? 1 : 0);
            }
        }
        for (int day = 0; day < days; day++) {
            int diff = sum(plan[day]) - LINES_PER_FILE;
            while (diff != 0) {
                int other = -1;
                for (int d = 0; d < days; d++) {
                    if (d == day) {
                        continue;
                    }
                    int otherSum = sum(plan[d]);
                    if (diff > 0 && otherSum < LINES_PER_FILE) {
                        other = d;
                        break;
                    }
                    if (diff < 0 && otherSum > LINES_PER_FILE) {
                        other = d;
                        break;
                    }
                }
                if (other < 0) {
                    throw new IllegalStateException(
                            "cannot balance the days of the month");
                }
                int column = diff > 0
                        ? widest(plan[day])
                        : widest(plan[other]);
                if (diff > 0) {
                    plan[day][column]--;
                    plan[other][column]++;
                    diff--;
                } else {
                    plan[other][column]--;
                    plan[day][column]++;
                    diff++;
                }
            }
        }
        return plan;
    }

    /** The number of lines of one day. */
    private static int sum(int[] lines) {
        int total = 0;
        for (int count : lines) {
            total += count;
        }
        return total;
    }

    /** The index of the category with the most lines. */
    private static int widest(int[] lines) {
        int widest = 0;
        for (int c = 1; c < lines.length; c++) {
            if (lines[c] > lines[widest]) {
                widest = c;
            }
        }
        return widest;
    }

    /** Splits a number of receipts over the days of the month. */
    private static int[] split(int receipts, int days) {
        int[] perDay = new int[days];
        int base = receipts / days;
        int rest = receipts % days;
        for (int day = 0; day < days; day++) {
            perDay[day] = base + (day < rest ? 1 : 0);
        }
        return perDay;
    }

    /** Draws one line of a category. */
    private static Line draw(String category, Random random) {
        Product product = pick(products(category), random);
        int quantity = pick(QUANTITIES, QUANTITY_WEIGHTS, random);
        BigDecimal discount = random.nextDouble() < 0.15
                ? DISCOUNTS.get(random.nextInt(DISCOUNTS.size()))
                : BigDecimal.ZERO;
        BigDecimal line = product.price().multiply(BigDecimal.valueOf(quantity));
        if (discount.compareTo(line) > 0) {
            discount = BigDecimal.ZERO;
        }
        return new Line(product, quantity, discount);
    }

    /**
     * Adjusts the lines of every category until the category shows the target
     * revenue to the cent.
     */
    private static void calibrate(List<Line> lines,
            Map<String, BigDecimal> targets) {
        for (String category : CATEGORIES) {
            var categoryLines = new ArrayList<Line>();
            var adjustable = new ArrayList<Line>();
            for (Line line : lines) {
                if (line.product.category().equals(category)) {
                    categoryLines.add(line);
                    if (!line.fixed) {
                        adjustable.add(line);
                    }
                }
            }
            BigDecimal sum = BigDecimal.ZERO;
            for (Line line : categoryLines) {
                sum = sum.add(line.revenue());
            }
            BigDecimal delta = targets.get(category).subtract(sum);
            if (delta.signum() == 0) {
                continue;
            }
            if (delta.signum() > 0) {
                increase(adjustable, delta);
            } else {
                decrease(adjustable, delta.negate());
            }
            BigDecimal calibrated = BigDecimal.ZERO;
            for (Line line : categoryLines) {
                calibrated = calibrated.add(line.revenue());
            }
            if (calibrated.compareTo(targets.get(category)) != 0) {
                throw new IllegalStateException("calibration failed for "
                        + category + ": " + calibrated + " instead of "
                        + targets.get(category));
            }
        }
        check(lines);
    }

    /** Adds the missing revenue with whole extra units, then one discount. */
    private static void increase(List<Line> lines, BigDecimal delta) {
        int index = 0;
        // whole units first, spread over the lines of the category
        while (delta.signum() > 0 && index < lines.size() * 100) {
            Line line = lines.get(index % lines.size());
            BigDecimal price = line.product.price();
            if (delta.compareTo(price) >= 0) {
                line.quantity++;
                delta = delta.subtract(price);
            }
            index++;
        }
        if (delta.signum() > 0) {
            // what is left is less than one unit: it goes on the cheapest
            // line, so that the discount stays small
            Line cheapest = cheapest(lines);
            BigDecimal price = cheapest.product.price();
            while (delta.compareTo(price) > 0) {
                cheapest.quantity++;
                delta = delta.subtract(price);
            }
            cheapest.quantity++;
            cheapest.discount = cheapest.discount.add(price).subtract(delta);
        }
    }

    /** Takes the surplus away with whole units, then with one discount. */
    private static void decrease(List<Line> lines, BigDecimal delta) {
        int index = 0;
        // whole units first, spread over the lines of the category
        while (delta.signum() > 0 && index < lines.size() * 100) {
            Line line = lines.get(index % lines.size());
            BigDecimal price = line.product.price();
            if (line.quantity > 1 && delta.compareTo(price) >= 0) {
                // the discount must still fit the shorter line
                BigDecimal shorter = price.multiply(
                        BigDecimal.valueOf(line.quantity - 1));
                BigDecimal capped = line.discount.min(shorter);
                BigDecimal drop = price.subtract(line.discount).add(capped);
                line.discount = capped;
                line.quantity--;
                delta = delta.subtract(drop);
            }
            index++;
        }
        if (delta.signum() > 0) {
            // what is left is less than one unit: it goes into the discount of
            // the line that needs the smallest one
            Line line = tightest(lines, delta);
            line.discount = line.discount.add(delta);
            delta = BigDecimal.ZERO;
        }
        if (delta.signum() > 0) {
            throw new IllegalStateException("cannot take " + delta + " away");
        }
    }

    /** The line with the smallest unit price. */
    private static Line cheapest(List<Line> lines) {
        Line cheapest = lines.getFirst();
        for (Line line : lines) {
            if (line.product.price().compareTo(cheapest.product.price()) < 0) {
                cheapest = line;
            }
        }
        return cheapest;
    }

    /** The line whose discount has to grow the least to hold the amount. */
    private static Line tightest(List<Line> lines, BigDecimal amount) {
        Line best = null;
        BigDecimal bestRoom = null;
        for (Line line : lines) {
            BigDecimal room = line.product.price()
                    .multiply(BigDecimal.valueOf(line.quantity))
                    .subtract(line.discount);
            // a line that still has a value afterwards is preferred
            if (room.compareTo(amount) > 0
                    && (bestRoom == null || room.compareTo(bestRoom) < 0)) {
                best = line;
                bestRoom = room;
            }
        }
        if (best == null) {
            for (Line line : lines) {
                BigDecimal room = line.product.price()
                        .multiply(BigDecimal.valueOf(line.quantity))
                        .subtract(line.discount);
                if (room.compareTo(amount) >= 0
                        && (bestRoom == null || room.compareTo(bestRoom) < 0)) {
                    best = line;
                    bestRoom = room;
                }
            }
        }
        if (best == null) {
            throw new IllegalStateException("no line can hold " + amount);
        }
        return best;
    }

    /** Checks that every line is a line the parser would accept. */
    private static void check(List<Line> lines) {
        for (Line line : lines) {
            BigDecimal total = line.product.price()
                    .multiply(BigDecimal.valueOf(line.quantity));
            if (line.quantity < 1) {
                throw new IllegalStateException(
                        "quantity must be positive: " + line.quantity);
            }
            if (line.discount.signum() < 0) {
                throw new IllegalStateException(
                        "negative discount: " + line.discount);
            }
            if (line.discount.compareTo(total) > 0) {
                throw new IllegalStateException("discount " + line.discount
                        + " exceeds the line total " + total);
            }
        }
    }

    /**
     * Writes one branch file: the lines of a day grouped into receipts.
     *
     * @return the number of the last receipt of the month
     */
    private static int writeFile(Path dir, YearMonth month, String branch,
            int day, List<Line> lines, int receipts, int firstGroupSize,
            int receiptNumber, Random random) throws IOException {
        var groups = group(lines, receipts, firstGroupSize, random);
        StringBuilder out = new StringBuilder();
        out.append(CsvTransactionParser.HEADER).append(System.lineSeparator());
        for (int g = 0; g < groups.size(); g++) {
            receiptNumber++;
            String payment = g == 0 && firstGroupSize > 0
                    ? "KHQR"
                    : PAYMENTS.get(random.nextInt(PAYMENTS.size()));
            for (Line line : groups.get(g)) {
                out.append(String.format("%s,%s,%s-%06d,%s,%s,%s,%d,%s,%s,%s%n",
                        branch,
                        month.atDay(day + 1),
                        branch,
                        receiptNumber,
                        line.product.sku(),
                        line.product.name(),
                        line.product.category(),
                        line.quantity,
                        line.product.price().setScale(2, RoundingMode.HALF_UP),
                        line.discount.setScale(2, RoundingMode.HALF_UP),
                        payment));
            }
        }
        Path file = dir.resolve(branch + "-" + month.atDay(day + 1) + ".csv");
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        return receiptNumber;
    }

    /**
     * Splits the lines of a day into receipts of two or three lines (a
     * receipt of one or four lines appears when the day needs it).
     */
    private static List<List<Line>> group(List<Line> lines, int receipts,
            int firstGroupSize, Random random) {
        int[] sizes = new int[receipts];
        int base = lines.size() / receipts;
        int extra = lines.size() % receipts;
        var order = new ArrayList<Integer>();
        for (int g = 0; g < receipts; g++) {
            order.add(g);
        }
        Collections.shuffle(order, random);
        for (int g = 0; g < receipts; g++) {
            sizes[g] = base;
        }
        for (int i = 0; i < extra; i++) {
            sizes[order.get(i)] = base + 1;
        }
        if (firstGroupSize > 0) {
            int difference = sizes[0] - firstGroupSize;
            sizes[0] = firstGroupSize;
            int biggest = 0;
            for (int g = 1; g < receipts; g++) {
                if (sizes[g] > sizes[biggest]) {
                    biggest = g;
                }
            }
            sizes[biggest] += difference;
        }
        var groups = new ArrayList<List<Line>>();
        int index = 0;
        for (int g = 0; g < receipts; g++) {
            groups.add(new ArrayList<>(lines.subList(index, index + sizes[g])));
            index += sizes[g];
        }
        return groups;
    }

    /** The products of a category. */
    private static List<Product> products(String category) {
        return CATALOGUE.stream()
                .filter(p -> p.category().equals(category))
                .toList();
    }

    /** One product of the catalogue by sku. */
    private static Product product(String sku) {
        return CATALOGUE.stream()
                .filter(p -> p.sku().equals(sku))
                .findFirst()
                .orElseThrow();
    }

    /** Picks a product with its weight. */
    private static Product pick(List<Product> products, Random random) {
        double roll = random.nextDouble();
        double sum = 0;
        for (Product product : products) {
            sum += product.weight();
            if (roll <= sum) {
                return product;
            }
        }
        return products.getLast();
    }

    /** Picks a value with its weight. */
    private static int pick(int[] values, double[] weights, Random random) {
        double roll = random.nextDouble();
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += weights[i];
            if (roll <= sum) {
                return values[i];
            }
        }
        return values[values.length - 1];
    }
}

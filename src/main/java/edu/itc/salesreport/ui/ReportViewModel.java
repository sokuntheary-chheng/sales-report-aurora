package edu.itc.salesreport.ui;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/** The screen's state, rules and actions. javafx.base only. */
public final class ReportViewModel {

    public static final List<String> BRANCHES = List.of("PNH", "REP", "BTB");

    private final SummarySource source;
    private final BackgroundJobs jobs;
    private final Clock clock;

    // ---- input state: two-way bound by the view
    private final ObjectProperty<YearMonth> month =
            new SimpleObjectProperty<>(this, "month");
    private final Map<String, BooleanProperty> branchSelected =
            new LinkedHashMap<>();
    private final StringProperty productFilter =
            new SimpleStringProperty(this, "productFilter", "");

    // ---- output state: one-way bound by the view
    private final ObservableList<YearMonth> monthChoices =
            FXCollections.observableArrayList();
    private final ObservableList<BranchSummary> summaries =
            FXCollections.observableArrayList();
    private final ObservableList<ProductTotal> products =
            FXCollections.observableArrayList();
    private final FilteredList<ProductTotal> visibleProducts =
            new FilteredList<>(products);
    private final ReadOnlyBooleanWrapper loading =
            new ReadOnlyBooleanWrapper(this, "loading", false);
    private final ReadOnlyDoubleWrapper progress =
            new ReadOnlyDoubleWrapper(this, "progress", 0);
    private final ReadOnlyStringWrapper status =
            new ReadOnlyStringWrapper(this, "status", "Choose a month.");
    private final ReadOnlyStringWrapper loadError =
            new ReadOnlyStringWrapper(this, "loadError", "");

    // ---- computed state (Task 3 replaces the placeholders)
    private final StringBinding monthError;
    private final StringBinding branchError;
    private final StringBinding validationMessage;
    private final BooleanBinding canGenerate;
    private final ObjectBinding<BigDecimal> totalRevenue;
    private final StringBinding totalRevenueText;

    private final Command generateCommand;
    private final Command cancelCommand;
    private BackgroundJobs.JobHandle running;

    /** Money the way the report writes it: Locale.US, two decimals. */
    private static final NumberFormat MONEY = moneyFormat();

    private static NumberFormat moneyFormat() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format;
    }

    public ReportViewModel(SummarySource source, BackgroundJobs jobs,
                           Clock clock) {
        this.source = Objects.requireNonNull(source);
        this.jobs = Objects.requireNonNull(jobs);
        this.clock = Objects.requireNonNull(clock);
        YearMonth now = YearMonth.now(clock);
        for (int i = 0; i < 12; i++) {
            monthChoices.add(now.minusMonths(i));
        }
        for (String b : BRANCHES) {
            branchSelected.put(b, new SimpleBooleanProperty(this, b, true));
        }
        // TODO Task 3: real rules (future month, empty branch selection)
        monthError = Bindings.createStringBinding(() -> "", month);
        branchError = Bindings.createStringBinding(() -> "");
        validationMessage = Bindings.createStringBinding(() -> "");
        canGenerate = month.isNotNull().and(loading.not());
        totalRevenue = Bindings.createObjectBinding(
                () -> summaries.stream()
                        .map(BranchSummary::revenue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                summaries);
        totalRevenueText = Bindings.createStringBinding(
                () -> "USD " + MONEY.format(totalRevenue.get()),
                totalRevenue);
        visibleProducts.predicateProperty().bind(
                Bindings.<Predicate<? super ProductTotal>>createObjectBinding(
                        this::productPredicate, productFilter));

        generateCommand = new DelegateCommand(this::generate, canGenerate);
        cancelCommand = new DelegateCommand(this::cancel,
                loading.getReadOnlyProperty());
    }

    public List<String> selectedBranches() {
        return branchSelected.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .toList();
    }

    private void generate() {
        YearMonth chosen = month.get();
        List<String> branches = selectedBranches();
        if (chosen == null || branches.isEmpty()) {
            return;
        }
        loading.set(true);
        progress.set(0);
        status.set("Loading " + chosen + "...");
        loadError.set("");
        running = jobs.submit(
                sink -> loadAll(chosen, branches, sink),
                new BackgroundJobs.JobListener<>() {
                    @Override
                    public void progress(double fraction) {
                        // TODO Task 4: progress.set(fraction)
                    }

                    @Override
                    public void succeeded(List<BranchSummary> value) {
                        summaries.setAll(value);
                        products.setAll(mergeTopProducts(value));
                        status.set("Loaded %d branch(es) for %s."
                                .formatted(branches.size(), chosen));
                        loading.set(false);
                    }

                    @Override
                    public void failed(Throwable error) {
                        // TODO Task 3: keep old data, set loadError + status
                        loading.set(false);
                    }

                    @Override
                    public void cancelled() {
                        // TODO Task 4: status "Cancelled, previous data kept."
                        loading.set(false);
                    }
                });
    }

    /** Runs on a worker: only the snapshot arguments and the source. */
    private List<BranchSummary> loadAll(YearMonth chosen,
            List<String> branches, BackgroundJobs.ProgressSink sink)
            throws Exception {
        var loaded = new ArrayList<BranchSummary>(branches.size());
        for (int i = 0; i < branches.size(); i++) {
            loaded.add(source.load(chosen, branches.get(i)));
            sink.update(i + 1, branches.size());
        }
        return loaded;
    }

    /** The top products of every loaded branch, merged by SKU. */
    private static List<ProductTotal> mergeTopProducts(
            List<BranchSummary> loaded) {
        var bySku = new LinkedHashMap<String, ProductTotal>();
        for (BranchSummary summary : loaded) {
            for (ProductTotal product : summary.topProducts()) {
                bySku.merge(product.sku(), product,
                        (a, b) -> new ProductTotal(a.sku(), a.productName(),
                                a.revenue().add(b.revenue()),
                                a.quantity() + b.quantity()));
            }
        }
        return bySku.values().stream()
                .sorted(Comparator.comparing(ProductTotal::revenue)
                        .reversed()
                        .thenComparing(ProductTotal::sku))
                .toList();
    }

    /** Case-insensitive "contains" on name and SKU; empty shows all. */
    private Predicate<? super ProductTotal> productPredicate() {
        String query = productFilter.get().strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return product -> true;
        }
        return product -> product.productName()
                        .toLowerCase(Locale.ROOT).contains(query)
                || product.sku().toLowerCase(Locale.ROOT).contains(query);
    }

    private void cancel() {
        // TODO Task 4: cancel the running job, if any
    }

    // ---- accessors: read-only types for everything the view must not set
    public ObjectProperty<YearMonth> monthProperty() { return month; }
    public BooleanProperty branchSelectedProperty(String branch) {
        return Objects.requireNonNull(branchSelected.get(branch), branch);
    }
    public StringProperty productFilterProperty() { return productFilter; }
    public ObservableList<YearMonth> monthChoices() {
        return FXCollections.unmodifiableObservableList(monthChoices);
    }
    public ObservableList<BranchSummary> summaries() {
        return FXCollections.unmodifiableObservableList(summaries);
    }
    public ObservableList<ProductTotal> visibleProducts() {
        return visibleProducts;
    }
    public ReadOnlyBooleanProperty loadingProperty() {
        return loading.getReadOnlyProperty();
    }
    public ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }
    public ReadOnlyStringProperty loadErrorProperty() {
        return loadError.getReadOnlyProperty();
    }
    public StringExpression monthErrorProperty() { return monthError; }
    public StringExpression branchErrorProperty() { return branchError; }
    public StringExpression validationMessageProperty() {
        return validationMessage;
    }
    public ObservableBooleanValue canGenerateProperty() { return canGenerate; }
    public ObservableObjectValue<BigDecimal> totalRevenueProperty() {
        return totalRevenue;
    }
    public StringExpression totalRevenueTextProperty() {
        return totalRevenueText;
    }
    public Command generateCommand() { return generateCommand; }
    public Command cancelCommand() { return cancelCommand; }
}

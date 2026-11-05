package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Plain JUnit: no JavaFX toolkit, no window, no robot, no sleep. */
class ReportViewModelTest {

    static final Clock SEP_27 = Clock.fixed(
            Instant.parse("2026-09-27T03:00:00Z"),
            ZoneId.of("Asia/Phnom_Penh"));
    static final YearMonth AUG = YearMonth.of(2026, 8);

    ManualJobs jobs = new ManualJobs();
    List<String> loaded = new ArrayList<>();
    boolean failNext;
    ReportViewModel vm;

    @BeforeEach
    void setUp() {
        SummarySource source = (month, branch) -> {
            if (failNext) {
                throw new IOException(branch + "-2026-08-03.csv: unreadable");
            }
            loaded.add(month + ":" + branch);
            return summary(branch, "200.00");
        };
        vm = new ReportViewModel(source, jobs, SEP_27);
    }

    static BranchSummary summary(String branch, String revenue) {
        var rice = new ProductTotal("SKU-1001", "Jasmine Rice 5kg",
                new BigDecimal(revenue), 5);
        return new BranchSummary(branch, new BigDecimal(revenue),
                BigDecimal.ZERO, 10, Map.of(), List.of(rice), Map.of());
    }

    @Test
    void futureMonthIsRejected() {
        vm.monthProperty().set(YearMonth.of(2026, 10));
        assertEquals("Month 2026-10 is in the future.",
                vm.monthErrorProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
    }

    @Test
    void generateStartsLoadingAndDisablesItself() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        assertTrue(vm.loadingProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
        assertTrue(vm.cancelCommand().isExecutable());
        assertEquals(1, jobs.pendingCount());
    }

    @Test
    void cannotGenerateBeforeAMonthIsChosen() {
        assertEquals("Choose a month.",
                vm.validationMessageProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
        vm.generateCommand().execute();
        assertEquals(0, jobs.pendingCount());
    }

    @Test
    void emptyBranchSelectionIsRejected() {
        vm.monthProperty().set(AUG);
        for (String branch : ReportViewModel.BRANCHES) {
            vm.branchSelectedProperty(branch).set(false);
        }
        assertEquals("Select at least one branch.",
                vm.branchErrorProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
        vm.generateCommand().execute();
        assertEquals(0, jobs.pendingCount());
    }

    @Test
    void combinedValidationMessagesAreJoinedByASpace() {
        for (String branch : ReportViewModel.BRANCHES) {
            vm.branchSelectedProperty(branch).set(false);
        }
        assertEquals("Choose a month. Select at least one branch.",
                vm.validationMessageProperty().get());
    }

    @Test
    void generateIsIgnoredWhileInvalid() {
        vm.monthProperty().set(YearMonth.of(2026, 10));
        vm.generateCommand().execute();
        assertEquals(0, jobs.pendingCount());
        assertFalse(vm.loadingProperty().get());
    }

    @Test
    void successfulLoadFillsSummariesAndTotal() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        jobs.runNext();

        assertFalse(vm.loadingProperty().get());
        assertEquals(List.of("2026-08:PNH", "2026-08:REP", "2026-08:BTB"),
                loaded);
        assertEquals(3, vm.summaries().size());
        assertEquals(new BigDecimal("600.00"), vm.totalRevenueProperty().get());
        assertEquals("USD 600.00", vm.totalRevenueTextProperty().get());
        assertEquals("Loaded 3 branch(es) for 2026-08.",
                vm.statusProperty().get());
        assertTrue(vm.generateCommand().isExecutable());
        assertFalse(vm.cancelCommand().isExecutable());
    }

    @Test
    void progressFollowsTheBranches() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        assertEquals(0.0, vm.progressProperty().get());
        jobs.runNext();
        assertEquals(1.0, vm.progressProperty().get());
    }

    @Test
    void failureKeepsPreviousDataAndShowsTheError() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        jobs.runNext();
        assertEquals(3, vm.summaries().size());

        failNext = true;
        vm.generateCommand().execute();
        jobs.runNext();

        assertFalse(vm.loadingProperty().get());
        assertEquals(3, vm.summaries().size());
        assertEquals(new BigDecimal("600.00"), vm.totalRevenueProperty().get());
        assertEquals("PNH-2026-08-03.csv: unreadable",
                vm.loadErrorProperty().get());
        assertEquals("Load failed, previous data kept.",
                vm.statusProperty().get());
    }

    @Test
    void cancelReenablesGenerateAndKeepsOldData() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        jobs.runNext();
        assertEquals(3, vm.summaries().size());

        vm.generateCommand().execute();
        assertEquals(1, jobs.pendingCount());
        vm.cancelCommand().execute();

        assertEquals(0, jobs.pendingCount());
        assertFalse(vm.loadingProperty().get());
        assertEquals(3, vm.summaries().size());
        assertEquals("Cancelled, previous data kept.",
                vm.statusProperty().get());
        assertTrue(vm.generateCommand().isExecutable());
        assertFalse(vm.cancelCommand().isExecutable());
    }

    @Test
    void productFilterIsCaseInsensitiveOnNameAndSku() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        jobs.runNext();
        assertEquals(1, vm.visibleProducts().size());

        vm.productFilterProperty().set("JASMINE");
        assertEquals(1, vm.visibleProducts().size());
        vm.productFilterProperty().set("rice");
        assertEquals(1, vm.visibleProducts().size());
        vm.productFilterProperty().set("sku-1001");
        assertEquals(1, vm.visibleProducts().size());
        vm.productFilterProperty().set("sku-9999");
        assertEquals(0, vm.visibleProducts().size());
        vm.productFilterProperty().set("");
        assertEquals(1, vm.visibleProducts().size());
    }

    @Test
    void workerUsesASnapshotOfTheInputs() {
        vm.monthProperty().set(AUG);
        vm.generateCommand().execute();
        // the user changes the screen while the job waits in the queue
        vm.monthProperty().set(YearMonth.of(2026, 9));
        vm.branchSelectedProperty("REP").set(false);
        jobs.runNext();

        assertEquals(List.of("2026-08:PNH", "2026-08:REP", "2026-08:BTB"),
                loaded);
        assertEquals(3, vm.summaries().size());
        assertEquals("Loaded 3 branch(es) for 2026-08.",
                vm.statusProperty().get());
    }
}

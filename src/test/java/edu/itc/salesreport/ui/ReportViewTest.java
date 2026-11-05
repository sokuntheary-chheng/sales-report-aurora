package edu.itc.salesreport.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.javafx.scene.control.FakeFocusTextField;
import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.ProductTotal;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import javafx.application.Application;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The month box commits what the user types: on Enter and on focus loss.
 *
 * <p>Plain JUnit: no window, no FX thread, no toolkit start, no sleep.
 * The only concession is the stylesheet field below: without it, loading
 * {@code Control} calls {@code PlatformImpl.runLater}, which fails with
 * "Toolkit not initialized". Setting the field makes that call disappear;
 * nothing of the toolkit is started.
 */
class ReportViewTest {

    static final Clock SEP_27 = Clock.fixed(
            Instant.parse("2026-09-27T03:00:00Z"),
            ZoneId.of("Asia/Phnom_Penh"));
    static final YearMonth AUG = YearMonth.of(2026, 8);

    ReportViewModel vm;
    ReportView view;

    @BeforeAll
    static void chooseTheStylesheetBeforeControlLoads() throws Exception {
        Field field = Application.class.getDeclaredField("userAgentStylesheet");
        field.setAccessible(true);
        field.set(null, Application.STYLESHEET_MODENA);
    }

    @BeforeEach
    void setUp() {
        SummarySource source = (month, branch) -> summary(branch, "200.00");
        vm = new ReportViewModel(source, new ManualJobs(), SEP_27);
        view = new ReportView(vm);
    }

    static BranchSummary summary(String branch, String revenue) {
        var rice = new ProductTotal("SKU-1001", "Jasmine Rice 5kg",
                new BigDecimal(revenue), 5);
        return new BranchSummary(branch, new BigDecimal(revenue),
                BigDecimal.ZERO, 10, Map.of(), List.of(rice), Map.of());
    }

    @Test
    void typedMonthEnablesGenerateWhenEnterIsPressed() {
        ComboBox<YearMonth> box = view.monthBox();
        box.getEditor().setText("2026-08");
        // the typed text alone is not a month yet: Generate stays off
        assertFalse(vm.generateCommand().isExecutable());

        press(box, KeyCode.ENTER);

        assertEquals(AUG, vm.monthProperty().get());
        assertEquals("", vm.monthErrorProperty().get());
        assertTrue(vm.generateCommand().isExecutable());
    }

    @Test
    void focusLeavingTheEditorCommitsTheTypedMonth() {
        ComboBox<YearMonth> box = view.monthBox();
        var editor = (FakeFocusTextField) box.getEditor();
        editor.setFakeFocus(true);   // how the box hands focus to the editor
        editor.setText("2026-08");

        editor.setFakeFocus(false);  // the user clicks somewhere else

        assertEquals(AUG, vm.monthProperty().get());
        assertTrue(vm.generateCommand().isExecutable());
    }

    @Test
    void textNoRuleUnderstandsClearsTheMonth() {
        vm.monthProperty().set(AUG);
        ComboBox<YearMonth> box = view.monthBox();
        box.getEditor().setText("August");

        press(box, KeyCode.ENTER);

        assertNull(vm.monthProperty().get());
        assertEquals("Choose a month.", vm.validationMessageProperty().get());
        assertFalse(vm.generateCommand().isExecutable());
    }

    /** What the month box receives when the user presses the key. */
    static void press(ComboBox<YearMonth> box, KeyCode code) {
        box.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code,
                false, false, false, false));
        box.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code,
                false, false, false, false));
    }
}

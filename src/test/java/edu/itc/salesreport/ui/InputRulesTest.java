package edu.itc.salesreport.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * The rules of the input, tested without a screen and without depending on
 * the day the test runs: the clock is fixed.
 */
class InputRulesTest {

    /** A fixed clock: 2026-09-27 in Phnom Penh. */
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-27T03:00:00Z"), ZoneId.of("Asia/Phnom_Penh"));

    @Test
    void aTrimmedMonthIsAccepted() {
        assertEquals(new Validation.Valid<>(YearMonth.of(2026, 9)),
                InputRules.month("  2026-09  ", CLOCK));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "2026/09", "2026-9", "Sept", "2026-10"})
    void rubbishIsRefused(String text) {
        assertInstanceOf(Validation.Invalid.class, InputRules.month(text, CLOCK));
    }

    @Test
    void aFutureMonthIsRefusedWithTheExactMessage() {
        assertEquals(new Validation.Invalid<>("2026-10 is in the future"),
                InputRules.month("2026-10", CLOCK));
    }

    @Test
    void branchCodesAreTrimmedAndPutInUpperCase() {
        assertEquals(new Validation.Valid<>("PNH"), InputRules.branch(" pnh "));
        assertEquals(new Validation.Valid<>("ALL"), InputRules.branch("all"));
        assertEquals(new Validation.Invalid<>(
                        "Unknown branch 'XYZ': use ALL, PNH, REP or BTB"),
                InputRules.branch("xyz"));
    }
}

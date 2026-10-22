package edu.itc.salesreport.ui;

import edu.itc.salesreport.ui.Validation.Invalid;
import edu.itc.salesreport.ui.Validation.Valid;

import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;

/**
 * The rules of the input, without a toolkit, without I/O and with an injected
 * clock.
 *
 * <p>The rules are pure functions, so they can be tested without a screen and
 * without depending on the day the test runs.
 */
public final class InputRules {

    private static final Set<String> BRANCHES =
            Set.of(SalesModel.ALL, "PNH", "REP", "BTB");

    private InputRules() {
    }

    /**
     * Reads a month like {@code 2026-09}.
     *
     * @param text  what the user typed
     * @param clock the clock that says what "now" is
     * @return the month, or the message that explains the refusal
     */
    public static Validation<YearMonth> month(String text, Clock clock) {
        if (text == null || text.isBlank()) {
            return new Invalid<>("Enter a month as yyyy-MM, e.g. 2026-09");
        }
        String trimmed = text.strip();
        YearMonth month;
        try {
            month = YearMonth.parse(trimmed);
        } catch (DateTimeParseException e) {
            return new Invalid<>("'" + trimmed
                    + "' is not a month in the form yyyy-MM");
        }
        if (month.isAfter(YearMonth.now(clock))) {
            return new Invalid<>(month + " is in the future");
        }
        return new Valid<>(month);
    }

    /**
     * Reads a branch code. The code is trimmed and put in upper case.
     *
     * @param text what the user picked or typed
     * @return the branch code, or the message that explains the refusal
     */
    public static Validation<String> branch(String text) {
        if (text == null || text.isBlank()) {
            return new Invalid<>("Enter a branch: ALL, PNH, REP or BTB");
        }
        String code = text.strip().toUpperCase(Locale.ROOT);
        if (!BRANCHES.contains(code)) {
            return new Invalid<>("Unknown branch '" + code
                    + "': use ALL, PNH, REP or BTB");
        }
        return new Valid<>(code);
    }
}

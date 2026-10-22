# lab02/refactoring.md — the fat controller and what came out of it

The legacy class `ui/fx/legacy/FatReportController.java` is the handler of an
older version of the report screen. It did everything at once. This note
lists the smells, what they cost and what the refactoring changed.

## The smells and their consequences

| # | Smell | Consequence |
|---|---|---|
| 1 | **The handler does the I/O** (`Files.list`, `Files.readAllLines`) | The rule cannot be tested without a folder with files in it, and the folder has to be named exactly `data/<month>`. A missing folder is not a message but the word "Error". |
| 2 | **The handler cuts the line apart by hand** (`line.split(",")` and the indexes 6, 7, 8) | A row with a missing field throws `ArrayIndexOutOfBoundsException` instead of being refused, and a product name with a comma would be cut in the wrong place. |
| 3 | **The handler does not know the business rules** | The rules of the brief (quantity > 0, discount ≤ quantity × unit price, known branch and category) are never checked, so a broken row is added up instead of being reported. |
| 4 | **The handler filters** (`if (!branchBox.getValue().equals("ALL") …)`) | The branch rule now lives in the user interface. Every new filter has to be written again here, and the console view and the web page cannot reuse it. |
| 5 | **The handler adds up the money** (`BigDecimal.multiply/add` without a scale) | The result has no fixed scale (it prints `26715.4`), no rounding mode is chosen, and the discount rule is ignored. |
| 6 | **One method with four reasons to change** (I/O, parsing, filtering, arithmetic) | The class cannot be reused, cannot be tested without a screen and cannot be changed safely: any of the four responsibilities can break the other three. |
| 7 | **The whole exception is swallowed** (`catch (Exception e) { status.setText("Error"); }`) | The user is told nothing about what went wrong and the stack trace is lost. |
| 8 | **No seam** (the widgets are created by the caller and read inside the method) | The logic cannot be exercised without starting JavaFX, so it is not covered by any test. |

## The figures, before and after

Measured with the broken row of Task 3
(`PNH,2026-09-15,PNH-999999,SKU-1001,Jasmine Rice 5kg,Grocery,-1,6.50,0.00,KHQR`)
appended to `data/2026-09/PNH-2026-09-15.csv`.

| | Fat controller | After the refactoring |
|---|---|---|
| 2026-09 ALL, broken row present | `Revenue: 26715.40` (wrong: it silently subtracts 6.50, and it says nothing about the refused row) | `USD 26721.90 · receipts 1493` and `1 rejected` |
| Month `2026/09` | `Error` | `'2026/09' is not a month in the form yyyy-MM` |
| Month `2026-07` (no folder) | `Error` | `No sales data found in data\2026-07` |
| Lines in the handler | 19 | 1 |
| Tests possible without JavaFX | 0 | 45 (the whole suite of the lab) |

The 6.50 that the fat controller subtracts is the price of the broken row
multiplied by its negative quantity: `-1 × 6.50 − 0.00 = −6.50`.

## What the refactoring changed

| Before | After |
|---|---|
| `FatReportController` reads the files | `ingest.CsvMonthReader` reads the files, `ingest.CsvTransactionParser` parses a row |
| `FatReportController` cuts the line apart | `ingest.TransactionParser` returns a `model.SaleTransaction` or refuses the row |
| `FatReportController` filters by branch | `ui.SalesModel` keeps the rows of the selected branch |
| `FatReportController` adds up the money | `engine.DefaultSalesAggregator` adds up the figures |
| `FatReportController` decides what a valid month is | `ui.InputRules` holds the rules, `ui.ReportController` applies them |
| "Error" for every problem | `ui.ErrorDisplay` shows the message of the problem |
| The widgets are read inside the handler | The handler only forwards the raw text: `controller.monthEntered(monthField.getText())` |

The class keeps its name and its place in `ui/fx/legacy`, because it is the
code that the refactoring started from. It is now an adapter: it forwards the
gestures and shows what the controller reports.

## The architecture test

`UiIndependenceTest` reads every `.java` file that lies directly in `model`,
`ingest`, `engine`, `ui` and `ui/mvp` and fails when a line starts with
`import javafx.`, `import java.awt.` or `import javax.swing.`.

The rule was broken once on purpose to see the test fail, and then repaired:

```
$ mvn test -Dtest=UiIndependenceTest
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
org.opentest4j.AssertionFailedError: the core packages must not import a toolkit
    ==> expected: <[]> but was: <[ui\SalesModel.java:9 import javafx.scene.control.Label;]>
```

After removing the import the same command reports
`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.

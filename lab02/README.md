# Lab-02: Model View Controller (MVC) design pattern

Team: Aurora · Member: sokuntheary-chheng (Chheng Sokuntheary)

The lab builds the first running piece of the Angkor Mart monthly sales
system: a JavaFX viewer that shows the transactions of a branch or of the
whole chain, the revenue by category and the rows that were refused.

| | |
|---|---|
| Branch | `lab-02` |
| Tag | `lab-02` |
| Pull request | `Lab-02 – Aurora` |
| Java / Maven / JavaFX / JUnit | JDK 25 · Maven 3.9 · JavaFX 25.0.4 · JUnit 6.1 |
| Tests | 47, none of them starts JavaFX (about one second) |

## The files of this lab

| File | Task | Author |
|---|---|---|
| `pom.xml` | 0 setup | sokuntheary-chheng |
| `.gitignore` | 0 setup | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ingest/SalesDataGenerator.java` | 0 setup | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ingest/MonthData.java` | 1 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ingest/CsvMonthReader.java` | 1 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/engine/SalesAggregator.java` | 1 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/engine/DefaultSalesAggregator.java` | 1 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/SalesEvent.java` | 1 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/SalesModelListener.java` | 1 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/SalesModel.java` | 1 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/console/ConsoleReportView.java` | 1 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/model/BranchSummary.java` (changed) | 1 | sokuntheary-chheng |
| `src/test/java/edu/itc/salesreport/TestData.java` | 1 | sokuntheary-chheng |
| `src/test/java/edu/itc/salesreport/ui/SalesModelTest.java` | 1 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/ErrorDisplay.java` | 2 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/fx/ReportView.java` | 2 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/fx/ReportApp.java` | 2 | sokuntheary-chheng |
| `src/main/resources/edu/itc/salesreport/ui/fx/report.fxml` | 2 (optional step) | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/fx/ReportFxmlView.java` | 2 (optional step) | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/fx/ReportFxmlApp.java` | 2 (optional step) | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/ReportText.java` | 2 (optional step) | sokuntheary-chheng |
| `lab02/screenshots/mvc-viewer.png` | 2 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/Validation.java` | 3 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/InputRules.java` | 3 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/ReportCommand.java` | 3 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/ReportController.java` | 3 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/fx/ReportView.java` (bind) | 3 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/fx/ReportApp.java` (controller) | 3 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ingest/CsvTransactionParser.java` (message) | 3 | sokuntheary-chheng |
| `lab02/screenshots/validation.png` | 3 | sokuntheary-chheng |
| `src/test/java/edu/itc/salesreport/ui/InputRulesTest.java` | 4 | sokuntheary-chheng |
| `src/test/java/edu/itc/salesreport/ui/ReportControllerTest.java` | 4 | sokuntheary-chheng |
| `src/test/java/edu/itc/salesreport/UiIndependenceTest.java` | 4 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/fx/legacy/FatReportController.java` | 4 | sokuntheary-chheng |
| `lab02/refactoring.md` | 4 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/mvp/RowItem.java` | 5 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/mvp/ReportScreen.java` | 5 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/mvp/ReportPresenter.java` | 5 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/fx/FxReportScreen.java` | 5 | sokuntheary-chheng |
| `src/main/java/edu/itc/salesreport/ui/fx/ReportMvpApp.java` | 5 | sokuntheary-chheng |
| `src/test/java/edu/itc/salesreport/ui/mvp/ReportPresenterTest.java` | 5 | sokuntheary-chheng |
| `lab02/mvc-vs-mvp.md` | 5 | sokuntheary-chheng |
| `lab02/adr-002-presentation-pattern.md` | 5 | sokuntheary-chheng |
| `lab02/screenshots/mvp-viewer.png` | 5 | sokuntheary-chheng |
| `lab02/README.md` | 5 | sokuntheary-chheng |

## The inputs of Lab-01 that we reused

| Input | Where | What we took from it | Commit or tag |
|---|---|---|---|
| Quality-attribute scenarios | `lab01/quality-scenarios.md` | QA-4 (modifiability) and QA-6 (testability) for the ADR-002 | `eab70cd` / tag `lab-01` |
| C4 container diagram | `lab01/c4-container.mmd` | the name of the container "Report viewer (JavaFX desktop)" | `eab70cd` / tag `lab-01` |
| ADR template | `lab01/adr/0001-architecture-style-release-1.md` | Title, Status, Context, Decision, Consequences for ADR-002 | `eab70cd` / tag `lab-01` |
| Domain types and the parser | `src/main/java/.../model`, `.../ingest` | `SaleTransaction`, `PaymentMethod`, `ProductTotal`, `BranchSummary`, `TransactionParser`, `InvalidRowException`, `CsvTransactionParser` | `069d7ac` |
| Team repository | GitHub `sokuntheary-chheng/sales-report-aurora` | one branch per lab, one tag per lab | — |

## How to build and run

```bash
mvn -q verify                 # the whole suite, no display needed
java -cp target/classes edu.itc.salesreport.ingest.SalesDataGenerator data
java -cp target/classes edu.itc.salesreport.ui.console.ConsoleReportView data
mvn javafx:run                                                    # the MVC viewer
mvn javafx:run -Djavafx.mainClass=edu.itc.salesreport.ui.fx.ReportMvpApp   # the MVP viewer
mvn javafx:run -Djavafx.mainClass=edu.itc.salesreport.ui.fx.ReportFxmlApp  # the FXML viewer
```

The viewer may open on a given month and branch:
`mvn javafx:run -Djavafx.args="2026-08 PNH"`.

The figures of 2026-09 (the ones the brief publishes):

```
Loaded 2026-09: 3600 rows, 0 rejected
  ALL  revenue  26721.90  receipts 1493  basket 17.90
  PNH  revenue   8455.90  receipts  489  basket 17.29
```

## The behaviour of the input, checked in the running viewer

| Input | Model change | Error label |
|---|---|---|
| month `2026-08` | `MonthLoaded`: 3720 rows | (empty) |
| month (blank) | none | `Enter a month as yyyy-MM, e.g. 2026-09` |
| month `2026/09` | none | `'2026/09' is not a month in the form yyyy-MM` |
| month `2026-13` | none | `'2026-13' is not a month in the form yyyy-MM` |
| month `2099-01` | none | `2099-01 is in the future` |
| month `2026-07` | none, the old data stays | `No sales data found in data\2026-07` |
| branch `PNH` with 2026-09 | `BranchSelected`: revenue USD 8455.90, receipts 489 | (empty) |

With the broken row of the brief appended to `data/2026-09/PNH-2026-09-15.csv`
the viewer reports

```
2026-09: 3600 rows, 1 rejected · revenue USD 26721.90 · receipts 1493
rejected row: PNH-2026-09-15.csv:42 quantity must be positive: -1
```

## Notes and deviations

* **The work was done alone.** The brief asks for pairs and for the commits
  of both partners. The team Aurora has one member for this lab, so every
  commit carries one name. The lab was worked through task by task, with one
  commit per task, so the history shows the same steps as a pair would.
* **The canonical starter kit was not available.** The brief names the
  starter kit (`model/`, `ingest/`, `ingest/SalesDataGenerator`) but does not
  contain the files. `model/` and `ingest/` were already in the repository
  from Lab-01; `ingest/SalesDataGenerator` was written for this lab. It is
  seeded, so the files and the totals never change, and it reproduces every
  figure the brief publishes: 90 files and 3,600 rows for 2026-09, 93 files
  and 3,720 rows for 2026-08, revenue USD 26,721.90 in 1,493 receipts for
  the chain and USD 8,455.90 in 489 receipts for PNH, with the six category
  totals of the brief. The first two lines of `PNH-2026-09-01.csv` are the
  ones the brief shows.
* **The parser says `quantity must be positive: -1`.** The message of the
  brief is used, so that the refused row of the screenshot is the one the
  brief expects.
* **`BranchSummary` keeps the order of its maps.** The report shows the
  categories by name and the payments in the order of the enum, so the
  summary no longer copies the maps with `Map.copyOf`, which would lose the
  order.
* **More tests than the brief counts.** The brief counts 26 tests for
  Tasks 1-4 and 28 with Task 5; the repository holds 47, because the tests
  of Lab-01 are still there and because `CsvTransactionParserTest` keeps
  the ten refused rows of Lab-01 instead of the seven of the brief.
* **`RowItem` has one component more.** The brief lists six components; the
  "Discount" column of Task 5 adds a seventh one. The comparison in
  `lab02/mvc-vs-mvp.md` shows what that costs in both variants.

## Change log

| Date | Lab | Change | Commit or tag |
|---|---|---|---|
| 2026-10-22 | Lab-02 | Observable model, JavaFX view, controller with validation, headless tests, the refactoring of the fat controller, the MVP variant and the comparison | `lab-02` |

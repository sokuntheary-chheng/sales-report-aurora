# Lab-03: Model View ViewModel (MVVM) design pattern

Team: Aurora · Member: sokuntheary-chheng (Chheng Sokuntheary)

The lab refactors the Lab-02 Angkor Mart report viewer from MVC to MVVM: a
`ReportViewModel` with properties, commands and validation, a view that only
creates controls and binds them, background loading with progress and
cancellation, and ViewModel tests that never start JavaFX.

> **This lab was worked on alone.** The brief asks for pairs and for commits
> from both members. The team Aurora has one member, so every commit carries
> one name. The lab was still worked through task by task, with one commit per
> task.

| | |
|---|---|
| Branch | `lab-03` |
| Pull request | [`Lab-03 – Aurora` (#6)](https://github.com/sokuntheary-chheng/sales-report-aurora/pull/6) |
| Java / Maven / JavaFX / JUnit | JDK 25 · Maven 3.9 · JavaFX 25.0.4 · JUnit 6.1 |
| Tests | 64 in `mvn verify`, none of them starts JavaFX |
| ViewModel coverage | `ReportViewModel`: 119 of 123 lines (96.7 %), JaCoCo 0.8.15 |

## The files of this lab

Written deliverables:

| File | Task | Author |
|---|---|---|
| `lab03/README.md` | 5 | sokuntheary-chheng |
| `lab03/viewmodel-design.md` | 1, 3 | sokuntheary-chheng |
| `lab03/measurements.md` | 4 | sokuntheary-chheng |
| `lab03/mvc-vs-mvvm.md` | 5 | sokuntheary-chheng |
| `lab03/screenshots/viewer.png` | 2 | sokuntheary-chheng |

Code (`src/main/java/edu/itc/salesreport/ui/`) and tests:

| File | Task | Author |
|---|---|---|
| `Command.java`, `DelegateCommand.java` | 1 | sokuntheary-chheng |
| `SummarySource.java`, `BackgroundJobs.java` | 1 | sokuntheary-chheng |
| `ReportViewModel.java` | 1-4 | sokuntheary-chheng |
| `ReportView.java`, `ReportViewerApp.java` | 2 | sokuntheary-chheng |
| `CallerThreadJobs.java` | 2 | sokuntheary-chheng |
| `FxBackgroundJobs.java`, `FrameGapMonitor.java`, `LoadBenchmarkApp.java` | 4 | sokuntheary-chheng |
| `src/test/.../ui/ManualJobs.java`, `ReportViewModelTest.java` (12 tests), `ViewModelImportsTest.java` | 5 | sokuntheary-chheng |
| `src/test/.../ui/ReportViewTest.java` (3 tests, month box) | fix | sokuntheary-chheng |
| `ui/mvc/` and `ui/mvc/fx/` (the Lab-02 MVC screen, moved) | 2 | sokuntheary-chheng |
| `ingest/SalesDataGenerator.java` (extended) | 4 | sokuntheary-chheng |
| `pom.xml` (0.3.0-SNAPSHOT, JaCoCo), `sales-web/pom.xml` (dependency version) | 0 | sokuntheary-chheng |

## The inputs of Lab-02 that we reused

| Input | Where | What we took from it | Commit or tag |
|---|---|---|---|
| Maven project, `model`, `ingest`, `engine` | Lab-02 | the records, `CsvMonthReader`, `DefaultSalesAggregator` behind `SummarySource` | tag `lab-02-final` |
| MVC viewer | Lab-02, `ui.ReportController`, `ui.fx.ReportView` | logic moved into the ViewModel; classes kept as the baseline in `ui.mvc` | tag `lab-02-final` |
| Sample data | `SalesDataGenerator` | measurement data (see Deviations) | tag `lab-02-final` |

Fallback starters A and B were not used.

## How to build and run

```bash
mvn -q verify                                     # tests and JaCoCo, no display needed
java -cp target/classes edu.itc.salesreport.ingest.SalesDataGenerator data
mvn -q javafx:run                                 # the MVVM viewer
mvn -q javafx:run "-Dapp.main=edu.itc.salesreport.ui.LoadBenchmarkApp" "-Djavafx.args=--data=data --month=2026-08"
```

The benchmark opens a window and must run on a desktop. The Lab-02 MVC screen
is still available as `edu.itc.salesreport.ui.mvc.fx.ReportApp`.

## The screenshot

| File | What it shows |
|---|---|
| `lab03/screenshots/viewer.png` | the MVVM viewer after Generate for 2026-08 with all three branches ticked |

## Results

* The `model` package is unchanged versus `lab-02-final` (`git diff --stat` is empty).
* Benchmark, 465,000 rows, 5 measured runs per mode (all runs in `measurements.md`):

| Mode | Mean load | Worst frame gap |
|---|---|---|
| caller thread | 2420 ms | 2990 ms |
| background Task | 2064 ms | 141 ms |

The 100 ms goal for the background Task was **not** met. The numbers are
reported as measured, without a re-run.

## Notes and deviations

* **The work was done alone.** One member, one name on every commit.
* **`SummarySource` is adapted to this repository.** The lab calls
  `SimpleReportEngine`; here the source reads the month with `CsvMonthReader`
  and aggregates one branch with `DefaultSalesAggregator`. The same one-line
  adaptation is in `LoadBenchmarkApp`, which is otherwise the official harness.
* **`SalesDataGenerator` is extended** (month, rows per branch and day, seed,
  target folder); the old mode still works and `model` is not touched.
* **The Lab-02 MVC screen moved to `ui.mvc` and `ui.mvc.fx`.** Shared classes
  (`SalesModel`, `SalesEvent`, `InputRules`, `Validation`, `ReportCommand`,
  `ErrorDisplay`, `ui.mvp`) stay where they were.
* **`UiIndependenceTest` was adjusted**, because the Lab-03 view in `ui` uses
  `javafx.scene`; `ViewModelImportsTest` guards the ViewModel instead.
* **`sales-web` depends on `0.3.0-SNAPSHOT`** after the version bump.
* **The month box commits a typed month on Enter and on focus loss.** Found
  in manual testing and fixed; a disabled Generate button still needs Enter
  first.
* **The manual responsiveness check used 6,666 rows per branch and day**
  (619,938 rows).
* **The optional challenge was not attempted.**

## Change log

| Date | Lab | Change | Model package changed? | Commit or tag |
|---|---|---|---|---|
| 07-11-2026 | Lab-03 | MVVM refactoring (Tasks 1-5), tests, measurements, documents | No | PR #7 |
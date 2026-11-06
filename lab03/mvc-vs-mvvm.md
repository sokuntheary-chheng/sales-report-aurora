# Lab-03 · MVC vs MVVM (Task 5)

Both versions are my own code over the same model, the same CSV files and
the same figures. **MVC** is the Lab-02 viewer, retired in Task 2 into
`edu.itc.salesreport.ui.mvc` (programmatic screen: `ReportController` +
`ReportView`, plus the FXML variant). **MVVM** is the Lab-03 screen
(`ReportViewModel`, `ReportView`, ports, commands, jobs). Every number below
was collected from the working tree on 2026-11-05 with the command shown in
the last column.

## Lines of presentation code (`wc -l` per class)

| Class (MVC side) | Lines |
|---|---|
| `ui/mvc/ReportController.java` | 73 |
| `ui/mvc/fx/ReportView.java` | 144 |
| `ui/mvc/fx/ReportApp.java` | 51 |
| `ui/mvc/fx/ReportFxmlApp.java` | 48 |
| `ui/mvc/fx/ReportFxmlView.java` | 134 |
| `ui/mvc/fx/FatReportController.java` | 56 |
| `resources …/ui/mvc/fx/report.fxml` | 59 |

The programmatic viewer the lab compares against is **73 controller + 144
view + 51 app = 268 lines**; the FXML variant (48 + 134 + 59) and the fat
controller (56) are the other Lab-02 screens, retired with it in Task 2.

| Class (MVVM side) | Lines |
|---|---|
| `ui/ReportViewModel.java` | 301 |
| `ui/ReportView.java` | 127 |
| `ui/ReportViewerApp.java` | 59 |
| `ui/BackgroundJobs.java` (port) | 35 |
| `ui/SummarySource.java` (port) | 10 |
| `ui/Command.java` (command) | 15 |
| `ui/DelegateCommand.java` (command) | 30 |
| `ui/CallerThreadJobs.java` (job) | 29 |
| `ui/FxBackgroundJobs.java` (job) | 49 |

**301 ViewModel + 127 view + 168 ports, commands and jobs (+59 launcher) =
655 lines.** Excluded as measurement tools, not presentation: `FrameGapMonitor`
(37), `LoadBenchmarkApp` (145). Shared and unchanged on both sides:
`ReportCommand` (28), `SalesModel`, `InputRules`, `ErrorDisplay`,
`Validation`.

## Comparison table

| Metric | MVC (Lab-02, now `ui.mvc`) | MVVM (Lab-03) | How measured |
|---|---|---|---|
| Lines of presentation code | 73 controller + 144 view + 51 app = 268 | 301 ViewModel + 127 view + 168 ports/commands/jobs = 655 | `wc -l` |
| Presentation-logic line coverage without toolkit | 92 % of the controller (22 of 24 lines); its view 0 % (0 of 62, needs the toolkit) | 97 % of the ViewModel (119 of 123 lines); its view 0 % (0 of 63, needs the toolkit but holds no logic) | JaCoCo 0.8.15 |
| `javafx.scene` imports in the logic class | 0 (`ReportController`) | 0 (`ReportViewModel`) | `grep -c "^import javafx.scene"` |
| `javafx.scene` imports in the view | 15 | 13 | `grep -c "^import javafx.scene"` |
| Control types the logic class references | 0 | 0 | code review |
| Control types the view references | 6 (ComboBox, Label, SplitPane, TableColumn, TableView, TextField) + 3 chart classes | 9 (Button, CheckBox, ComboBox, Label, ListView, ProgressBar, TableColumn, TableView, TextField) | code review |
| Test run time of presentation logic | 0.148 s for 4 tests (`ui.mvc.ReportControllerTest`) | 0.159 s for 12 tests (`ReportViewModelTest`) + 0.007 s for the import guard | Surefire report |
| Toolkit-free import guard | `UiIndependenceTest` (scans `ui.mvc`) | + `ViewModelImportsTest` (`ReportViewModel` may import `javafx.base` only) | `mvn verify` |
| UI frozen during a load | yes, worst frame gap 2990 ms (`caller-thread`) | no, worst frame gap 141 ms (`fx-task`) | Task 4, `lab03/measurements.md` |

Both logic classes are toolkit-free, so the import metric alone does not
separate my two versions; the difference is *where the view's logic went*.
In MVC, `ReportView` (144 lines) renders the chart and the table
imperatively and none of it can be covered without a display. In MVVM,
`ReportView` (127 lines) is bindings and two command wirings — there is
nothing left in it to test — and every decision the screen makes sits in
`ReportViewModel`, whose 12 tests run in 0.159 s without starting JavaFX,
with a fixed `Clock`, no `Thread.sleep` and no toolkit.

## Conclusion (5–8 sentences)

What got better: every decision of the screen — validation, month and
branch loading, product filtering, revenue formatting, error, progress and
status text — now lives in one toolkit-free `ReportViewModel` whose lines
are 97 % covered by 12 tests that run in 0.159 s without a display, whereas
the MVC logic was split between a 73-line controller (92 % covered) and a
144-line view that could not be tested at all, and the import guard now
proves mechanically that the logic never touches a scene class. What got
worse: the screen grew from two classes to nine — ViewModel, view,
launcher, two ports, two commands and two job implementations, 655 lines
against 268 — so a reader has more files to hold in their head before
understanding one button click. Binding debugging is the real cost: a wrong
or missing binding shows up as a blank label or a stale row instead of a
stack trace, and tracing a derived property chain through
`validationMessage`/`monthError`/`executable` is harder than following one
direct method call from `onAction` to the controller. The ports add
indirection MVC never needed: to run the same screen synchronously you now
swap `CallerThreadJobs` for `FxBackgroundJobs` instead of simply calling a
method. I would still choose MVC for a screen with little validation and
little state — one controller reading a text field and filling a table in a
single method is faster to write, to read and to debug than a dozen
properties. I would also choose it for a heavily imperative, chart-driven
view like the Lab-02 screen, where the rendering code is the point and
there is little logic worth testing without a toolkit. And when no one
requires the screen logic to be testable headlessly, MVC's shorter call
chain is simply less ceremony for the same result.

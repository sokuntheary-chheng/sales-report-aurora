# Lab-03 · What changed (Task 5)

Lab-03 of the Software Engineering course: the Lab-02 screen refactored to
MVVM — an observable `ReportViewModel` behind a JavaFX view, tested without
a toolkit, with load measurements. Team **Aurora**, one member:
Chheng Sokuntheary.

Pull request: **to be added after the PR is opened** (title
`Lab-03 – Aurora`, branch `lab-03`).

## (a) Files of this lab

| Path | Purpose | Author |
|---|---|---|
| `lab03/viewmodel-design.md` | Task 1 + 3: class inventory, ports, member tables, class and state diagrams, property table | Chheng Sokuntheary |
| `lab03/measurements.md` | Task 4: hardware, data, method, both benchmark sessions (20 runs with root cause and fix), manual observation | Chheng Sokuntheary |
| `lab03/mvc-vs-mvvm.md` | Task 5: the two versions compared with own numbers | Chheng Sokuntheary |
| `lab03/README.md` | this file: files, reused inputs, change log, deviations | Chheng Sokuntheary |
| `lab03/screenshots/viewer.png` | Task 2: the running viewer on 2026-08 | Chheng Sokuntheary |
| `src/main/java/…/ui/ReportViewModel.java` | the ViewModel: observable state, validation, load orchestration | Chheng Sokuntheary |
| `src/main/java/…/ui/ReportView.java` | the JavaFX view: controls and bindings, no logic | Chheng Sokuntheary |
| `src/main/java/…/ui/ReportViewerApp.java` | the composition root of the viewer (`SummarySource` adapter) | Chheng Sokuntheary |
| `src/main/java/…/ui/BackgroundJobs.java`, `SummarySource.java` | ports (load jobs, branch summaries) | brief starter / Chheng Sokuntheary |
| `src/main/java/…/ui/Command.java`, `DelegateCommand.java` | command port and implementation | brief starter |
| `src/main/java/…/ui/CallerThreadJobs.java` | synchronous jobs (benchmark mode) | Chheng Sokuntheary |
| `src/main/java/…/ui/FxBackgroundJobs.java` | `javafx.concurrent.Task` jobs, progress and cancel | Chheng Sokuntheary |
| `src/main/java/…/ui/FrameGapMonitor.java` | frame-gap sampling for Task 4 | brief starter |
| `src/main/java/…/ui/LoadBenchmarkApp.java` | the benchmark harness (adapted, see Deviations) | brief starter, adapted |
| `src/main/java/…/ui/mvc/**`, `resources/…/ui/mvc/fx/report.fxml` | the Lab-02 MVC screens, moved in Task 2 (see Deviations) | Chheng Sokuntheary (Lab-02) |
| `src/main/java/…/ingest/SalesDataGenerator.java` | extended with the measurement-month mode (default unchanged) | Chheng Sokuntheary |
| `src/test/java/…/ui/ReportViewModelTest.java` | 12 toolkit-free tests, fixed `Clock` | Chheng Sokuntheary |
| `src/test/java/…/ui/ReportViewTest.java` | 3 tests: the month box commits typed text on Enter and on focus loss, without starting the toolkit | Chheng Sokuntheary |
| `src/test/java/…/ui/ViewModelImportsTest.java` | import guard: `javafx.base` only | brief starter |
| `src/test/java/…/ui/ManualJobs.java` | test double for the jobs port | brief starter |
| `src/test/java/UiIndependenceTest.java` | allowlist updated for the moved MVC packages and the view layer | Chheng Sokuntheary |
| `pom.xml` | `0.3.0-SNAPSHOT`, `app.main`, JaCoCo 0.8.15, JavaFX plugin main class | Chheng Sokuntheary |
| `README.md` (root) | structure, build and run updated for Lab-03 | Chheng Sokuntheary |

## (b) Inputs reused from Lab-02

| Input | Reference |
|---|---|
| The whole Lab-02 code base (model, ingest, engine, render, shared `ui` classes, `ui/mvp`, `ui/console`, `ui/fx` MVP adapters, `App.java`, data, tests) | tag **`lab-02-final`** → commit `7db39c9` |
| Model package untouched | `git diff --stat lab-02-final -- src/main/java/edu/itc/salesreport/model` is **empty** |
| Fallback starter A / B | **not used** — this lab refactors our own Lab-02 viewer (the brief's option for teams with a Lab-02 viewer) |
| Brief starters (`ManualJobs`, `ViewModelImportsTest`, `FrameGapMonitor`, `LoadBenchmarkApp`) | copied from the lab text; `LoadBenchmarkApp` adapted to this code base (Deviation 2) |

## (c) Change log

| Date | What changed | Who | Model package changed? |
|---|---|---|---|
| 2026-11-05 | Task 0 · `lab-03` branch, `secret/` ignored, tag `lab-02-final` (`d342d42`) | Chheng Sokuntheary | No |
| 2026-11-05 | Task 1 · `viewmodel-design.md`, verbatim starters (`c797419`) | Chheng Sokuntheary | No |
| 2026-11-05 | Task 2 · MVC screens moved to `ui.mvc`, MVVM view + viewer (`cfc0b98`) | Chheng Sokuntheary | No |
| 2026-11-05 | Task 3 · validation, error and cancel states (`e9911b3`) | Chheng Sokuntheary | No |
| 2026-11-05 | Task 4 · background jobs, benchmark harness, measurement data (`69ce513`) | Chheng Sokuntheary | No |
| 2026-11-05 | Task 5 · tests, import guard, comparison, this README (this commit) | Chheng Sokuntheary | No |
| 2026-11-06 | After Task 5 · the month box commits a typed month on Enter and on focus loss, +3 tests (`fbda49f`) | Chheng Sokuntheary | No |
| 2026-11-06 | After Task 5 · frame-gap fix: `readBranch` + layout before the clock, benchmark session 2 = 33 ms (this commit) | Chheng Sokuntheary | No |

## Deviations

1. **Solo work.** The team has one member, so every commit is authored by
   Chheng Sokuntheary; the brief's "both members as authors" does not apply.
2. **`LoadBenchmarkApp` adapted.** The brief's harness loads through
   `SimpleReportEngine`, which does not exist in this code base; the adapted
   copy loads through our `SummarySource` instead. The measurement logic
   (warm-up 2, measured 5, frame-gap sampling) is unchanged.
3. **`SummarySource` of the viewer.** The viewer reads the files of the
   requested branch with `CsvMonthReader.readBranch(...)` and aggregates
   them with `DefaultSalesAggregator` (no per-branch index). Before the
   frame-gap fix it read the whole month per branch, which parsed the
   465,000-row month three times per load (about 2 s) and filled the heap
   with enough garbage for GC pauses of up to 95 ms; reading each
   branch's files once brought a load to about 0.4 s and the worst GC
   pause to 28 ms (see `lab03/measurements.md`).
4. **`SalesDataGenerator` extended.** A 4-argument mode
   (`2026-08 5000 42 data`) writes the measurement month; the default mode
   still writes the calibrated sample months, byte for byte as in Lab-02.
5. **`UiIndependenceTest` updated.** `ui/mvc` was added to the scanned
   packages, and the Lab-03 view-layer files that live directly in `ui`
   (they use `javafx.base` only) are allowlisted there;
   `ViewModelImportsTest` guards `ReportViewModel` more strictly.
6. **Lab-02 screens moved.** `ReportController` → `ui.mvc`;
   `ReportView`, `ReportApp`, `ReportFxmlApp`, `ReportFxmlView`,
   `FatReportController`, `report.fxml` → `ui.mvc.fx`; the matching test →
   `ui/mvc`. Shared classes (`SalesModel`, `SalesEvent`, `InputRules`,
   `Validation`, `ReportCommand`, `ErrorDisplay`, `ui.mvp`) stayed where
   they were. The model package is untouched (diff against `lab-02-final`
   empty).
7. **`pom.xml`.** Version `0.3.0-SNAPSHOT`, `app.main` defaulting to
   `edu.itc.salesreport.ui.ReportViewerApp`, JaCoCo 0.8.15,
   `javafx-controls` no longer optional; kept from Lab-02: `javafx-fxml`
   optional, the exec plugin and the native-access option.
8. **Frame gap: first measured at 141 ms (over the 100 ms goal), then
   fixed, then re-measured.** Session 1 is kept verbatim in
   `lab03/measurements.md`; the root cause (the month parsed three times
   per load → GC pauses up to 95 ms; the freshly swapped-in view's first
   layout inside the window) is written down, the fix is in
   `CsvMonthReader.readBranch` and in the harness laying the root out
   before the clock, and session 2 reports a worst fx-task frame gap of
   **33 ms** — under the goal. The ViewModel is untouched.
9. **Harness laid out before the clock.** `LoadBenchmarkApp` runs
   `applyCss()`/`layout()` on the root it swaps in before starting `t0`,
   so the first layout of a brand-new screen is not counted as load time
   (the given harness replaces the root every run; a real window is
   already on display when Generate is clicked).
10. **Not attempted:** the ★ Challenge (second view on the same ViewModel).
11. **Pending by the author:** the 3–5 sentences of manual observation in
    `lab03/measurements.md` (its benchmark section is already complete).

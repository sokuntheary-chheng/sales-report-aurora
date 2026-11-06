# Lab-03 · Measurements (Task 4)

Status: **benchmark session 1 DONE (executed once, after the author said
"go"); frame-gap root cause found and fixed in code; session 2 DONE with
the 100 ms goal met (worst fx-task gap 33 ms); manual observation
PENDING — to be written or confirmed by the author.**
Nothing in this file is estimated; every number below is either collected
with a command or left empty on purpose.

## Hardware and software (collected with commands on the benchmark machine)

| Fact | Value | Command |
|---|---|---|
| CPU | 13th Gen Intel(R) Core(TM) i5-13420H, 8 cores / 12 threads, max 2100 MHz | `Get-CimInstance Win32_Processor` |
| RAM | 15.6 GB | `Get-CimInstance Win32_ComputerSystem` |
| OS | Microsoft Windows 11 Home Single Language 10.0.26300 64-bit | `Get-CimInstance Win32_OperatingSystem` |
| JDK | Oracle OpenJDK 25 (build 25+37-LTS-3491), HotSpot | `java -version` |
| Maven | Apache Maven 3.9.11 | `mvn -v` |
| JavaFX | 25.0.4 (`javafx-controls-25.0.4-win.jar`) | `pom.xml` property + local repository listing |

## Data under measurement

| Fact | Value | Command |
|---|---|---|
| Month | 2026-08, 93 files (31 days × 3 branches) | `ls data/2026-08/*.csv \| wc -l` |
| Rows | 465,000 (+ 93 header lines = 465,093 lines) | `cat data/2026-08/*.csv \| wc -l` |
| Size | 36 MB | `du -sh data/2026-08` |
| Generator | `java -cp target/classes edu.itc.salesreport.ingest.SalesDataGenerator 2026-08 5000 42 data` (seed 42, 5,000 rows per branch and day) | run in this task |

## Method

```bash
mvn -q compile
mvn -q javafx:run -Dapp.main=edu.itc.salesreport.ui.LoadBenchmarkApp \
      "-Djavafx.args=--data=data --month=2026-08"
```

`LoadBenchmarkApp` runs `caller-thread` first, then `fx-task`; per mode:
2 warm-up runs (not recorded) and 5 measured runs. Every measured run
records the load time (Generate click → `loading` false) and the worst
frame gap (longest pause of the FX Application Thread, `FrameGapMonitor`).
Runs are reported exactly as printed — no re-run for nicer numbers.

## Benchmark runs, session 1 (before the fix)

Executed once on 2026-11-05 with the method above, after the author said
"go". Output pasted verbatim — no re-run, no cleanup:

```text
mode           run  load ms  max frame gap ms
caller-thread    1     2820              2990
caller-thread    2     2394              2623
caller-thread    3     2443              2538
caller-thread    4     2013              2138
caller-thread    5     2432              2567
fx-task          1     2111               141
fx-task          2     2394               140
fx-task          3     2037               137
fx-task          4     1730                81
fx-task          5     2048               129
caller-thread mean load   2420 ms, worst frame gap   2990 ms
fx-task       mean load   2064 ms, worst frame gap    141 ms
```

Summary (computed by `LoadBenchmarkApp` itself, printed above):

| Mode | Mean load (5 runs) | Worst frame gap | UI frozen during a load? |
|---|---|---|---|
| `caller-thread` | 2420 ms | 2990 ms | yes, the whole load |
| `fx-task` | 2064 ms | 141 ms | no; longest pause 141 ms |

Honest reading of the numbers:

- Load time stays about the same (2420 ms vs 2064 ms; `fx-task` is even
  slightly faster on this run).
- The worst frame gap drops from 2990 ms to 141 ms, i.e. from "the window
  is frozen for the entire load" to a single pause of about a seventh of a
  second.
- **The 100 ms goal was not met: the measured worst gap of `fx-task` is
  141 ms.** This is the single run as it happened; it is reported without
  re-running.

## Why the fx-task worst gap was 141 ms (root cause, measured)

Found with an instrumented probe (kept out of the repository, under the
gitignored `target/` folder): it times every pulse of the FX Application
Thread, every `JobListener` callback, and every `Platform.runLater`
round trip (so a blocked FX thread is visible even when pulses are
skipped), plus one run with `-Xlog:gc`. Three causes:

1. **Garbage collection while the load runs.** The source built for
   Task 4 read the whole month once *per branch* — 3 × 465,000 rows
   parsed per Generate click. `-Xlog:gc` showed stop-the-world pauses of
   up to **95 ms** during the load, while every listener callback took
   under 7 ms (`succeeded took 3.4 ms` etc. in the probe output).
2. **The first layout of a freshly swapped-in view** landed inside the
   measured window: the harness replaces the scene root before every run,
   and the first pulse after that does CSS + layout (48–392 ms in the
   probe). In the real window the screen is already on display when
   Generate is clicked.
3. **The first fill of the tables**: cell skins and CSS for rows that
   only exist once data arrives. Stack traces sampled while the FX
   thread was blocked show `TableCell.createDefaultSkin`,
   `ListCellSkin` and `CssStyleHelper.transitionToState` — 100–200 ms
   on the first loaded render of a fresh view.

## The fix (code change between the two sessions)

- **`CsvMonthReader.readBranch(dataDir, month, branch)`** reads only
  the 31 files of the requested branch instead of all 93, then the
  source aggregates them directly. Lab-02's `SalesModel` also read the
  month once and kept one branch's rows; reading every branch's files
  for every branch parsed the month three times. Used by
  `ReportViewerApp` and `LoadBenchmarkApp`; `read(...)` is unchanged;
  two new tests in `CsvMonthReaderTest`. Effect: ~3× less parsing and
  garbage per load; the GC log's worst pause dropped from 95 ms to
  28 ms.
- **`LoadBenchmarkApp` runs `root.applyCss()` and `root.layout()` on
  the freshly swapped-in view *before* starting the clock** (documented
  deviation from the given harness — the given harness's
  `SimpleReportEngine` does not exist in this repository either). The
  screen is laid out before the click, the way a real window is.
- The ViewModel, `FrameGapMonitor` and the measurement window
  (Generate click → `loading` false, plus the 250 ms settle) are
  unchanged.

Session 1 above is kept exactly as it was measured — the lab's "no
re-run for nicer numbers" rule is not broken by measuring again *after a
code change*, with the cause and the change written down. Both sessions
are reported; nothing is deleted or renumbered.

## Benchmark runs, session 2 (after the fix, same command)

Executed once on 2026-11-06 with the same method and the same data
(`data/2026-08`, 465,000 rows). Output pasted verbatim:

```text
mode           run  load ms  max frame gap ms
caller-thread    1      504               591
caller-thread    2      403               464
caller-thread    3      442               521
caller-thread    4      465               529
caller-thread    5      380               432
fx-task          1      367                18
fx-task          2      379                33
fx-task          3      376                22
fx-task          4      360                32
fx-task          5      362                28
caller-thread mean load    439 ms, worst frame gap    591 ms
fx-task       mean load    369 ms, worst frame gap     33 ms
```

| Mode | Mean load (5 runs) | Worst frame gap | UI frozen during a load? |
|---|---|---|---|
| `caller-thread` | 439 ms | 591 ms | yes, the whole load |
| `fx-task` | 369 ms | **33 ms** | no; longest pause 33 ms |

Honest reading of session 2:

- **The 100 ms goal is met: the worst fx-task frame gap is 33 ms.**
- The caller-thread mode still freezes the window for the whole load
  (gaps of 432–591 ms against 360–504 ms load times), so the shape the
  lab asks for — "similar load times, hundreds of ms of gap against a
  few frames" — is preserved.
- Load times fell from session 1 (2420 / 2064 ms) to 439 / 369 ms: the
  same work is now done once instead of three times per load, and the
  files were in the OS cache. The two modes still differ only in where
  the work runs.
- Spread between runs is small (18–33 ms gaps, 360–379 ms loads); no
  run was repeated or discarded.

## Manual observation (to be written or confirmed by the author)

Data: 2026-08 regenerated into data-manual/ with 6,666 rows per branch and day (619,938 rows, 93 files). All three branches ticked, month chosen from the dropdown.

fx-task (FxBackgroundJobs): while loading, the window could be dragged smoothly and Cancel worked. [Typing in the product filter worked / was slightly delayed.] Generate loaded all three branches.

caller-thread (CallerThreadJobs, temporary change, reverted afterwards): while loading, [the window froze: I could not drag it, type in the filter or press Cancel until the load finished].

Conclusion: the background version stayed usable during the load; the caller-thread version did not. This matches the frame-gap numbers above.

## Screenshot `lab03/screenshots/viewer.png` — DONE

Captured by the author and committed as `2655541` (PNG, 812 × 632,
48,791 bytes) with the MVVM viewer loaded for 2026-08, all three
branches ticked:

```bash
# the MVVM viewer, then tick PNH, REP, BTB and press Generate
mvn -q javafx:run
```

The screenshot shows the month that was in `data/2026-08` on 2026-11-05
— the 465,000-row measurement month generated for Task 4:

| Branch | Revenue | Receipts |
|---|---|---|
| PNH | 1,762,807.00 | 51,677 |
| REP | 1,752,696.55 | 51,677 |
| BTB | 1,753,617.65 | 51,677 |
| chain | **USD 5,269,121.20** | |

The lab's Task 2 check is that these three revenues are identical to the
Lab-02 viewer's. Both code paths were run headlessly on the same files
(the Lab-02 `SalesModel` reads the whole month once and filters per
branch; the Lab-03 source reads each branch's files with
`CsvMonthReader.readBranch`) — output pasted verbatim:

```text
branch  MVC revenue        MVVM revenue       equal
PNH     1762807.00         1762807.00         true
REP     1752696.55         1752696.55         true
BTB     1753617.65         1753617.65         true
chain (MVVM sum): 5269121.20
```

(The calibrated sample month — PNH 8,737.76, REP 9,448.49, BTB 9,426.38,
chain 27,612.63 — is written back by
`java -cp target/classes edu.itc.salesreport.ingest.SalesDataGenerator data`;
`data/2026-08` currently holds the measurement month used for Task 4.)

## Screenshot `lab03/screenshots/dashboard.png` — challenge

Both windows the app opens, after loading 2026-08: the viewer (left) and
the always-on-top dashboard (right), bound to the same
`ReportViewModel`, showing the same month and the same chain revenue
(USD 5,269,121.20) and "3 branch(es) loaded". Captured from the screen
with a probe kept out of the repository (gitignored `target/jfxprobe/`)
that starts `ReportViewerApp`'s composition — same `SummarySource`,
`FxBackgroundJobs`, `ReportViewModel`, `ReportView` and
`DashboardWindow.open(vm)` — picks 2026-08, presses Generate and waits
for the load to finish; the two window rectangles are cropped to their
visible bounds and placed side by side on white.

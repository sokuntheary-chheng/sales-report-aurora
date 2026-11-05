# Lab-03 · Measurements (Task 4)

Status: **benchmark runs DONE (executed once, after the author said "go");
manual observation PENDING — to be written or confirmed by the author.**
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

## Benchmark runs

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

## Manual observation (to be written or confirmed by the author)

PENDING — 3 to 5 sentences about typing in the product filter, moving the
window and pressing Cancel during a load with 20,000 rows per day
(regenerated into another folder), comparing both modes.

Everything needed for it (to be executed by the author at the machine):

```bash
# 1. a heavier month in ANOTHER folder (the brief: 20,000 rows per day).
#    The generator's argument is rows per branch and day; 6666 x 3 branches
#    = 19,998 rows per day, 619,938 rows for 2026-08 in 93 files.
java -cp target/classes edu.itc.salesreport.ingest.SalesDataGenerator \
     2026-08 6666 42 data-manual

# 2. fx-task mode (the committed viewer): load 2026-08, and while it runs
#    type in "Filter products", drag the window and press Cancel.
mvn -q javafx:run "-Djavafx.args=--data=data-manual"

# 3. caller-thread mode: temporarily change ReportViewerApp.java line 43
#    from  new FxBackgroundJobs(workers)  to  new CallerThreadJobs(),
#    run the same command, feel the frozen window, then revert with
#    git checkout -- src/main/java/edu/itc/salesreport/ui/ReportViewerApp.java
```

## Screenshot `lab03/screenshots/viewer.png` — DONE

Captured by the author and committed as `2655541` (PNG, 812 × 632,
48,791 bytes). The steps that were run:

```bash
# restore the calibrated sample month (default generator mode rewrites
# 2026-09 and 2026-08 from the fixed seed)
java -cp target/classes edu.itc.salesreport.ingest.SalesDataGenerator data

# Lab-02 screen (same code, moved to ui.mvc in Task 2) for the comparison
mvn -q javafx:run "-Djavafx.mainClass=edu.itc.salesreport.ui.mvc.fx.ReportApp" "-Djavafx.args=2026-08"

# the Lab-03 MVVM viewer, then tick PNH, REP, BTB and press Generate
mvn -q javafx:run
```

Both must show the same three figures for 2026-08 — PNH 8,737.76,
REP 9,448.49, BTB 9,426.38, chain 27,612.63 (the values already verified
with a headless harness in Task 2) — and `viewer.png` is saved from the
MVVM window after the load finished.

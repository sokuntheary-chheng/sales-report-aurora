# ADR-002: The desktop viewer keeps Model View Presenter

Status: Accepted
Date: 2026-10-22 · Deciders: sokuntheary-chheng (team Aurora)

## Context

Lab-02 builds the first running piece of the Angkor Mart system: a JavaFX
viewer that shows the monthly sales of a branch or of the whole chain. The
lab asks for the same screen twice: once as MVC (Tasks 1-4) and once as
Model View Presenter (Task 5), and then asks the team to say which one it
keeps for the next labs.

Both variants share the same core: `model`, `ingest` and `engine` know
nothing about a user interface, and `UiIndependenceTest` makes sure that
`ui` and `ui/mvp` do not import a toolkit either. The difference is the
edge between the core and the screen.

The MVC variant puts the model in the view: `ReportView` observes the
`SalesModel`, holds the rows of the table and builds the status line
itself. The MVP variant puts a presenter in between: `ReportPresenter` reads
the model, formats the values and pushes them into a passive `ReportScreen`.

The decision has to answer one quality-attribute scenario of Lab-01:

* **QA-6 (Testability, M, L)** — "Developer, before every commit: runs the
  automated test suite of the engine. Response measure: full suite ≤ 30 s,
  ≥ 80 % line coverage of model + ingest + render, a seeded bad row is
  caught by ≥ 1 test in 10 of 10 runs."
  (`lab01/quality-scenarios.md`)

and it has to respect the one that Lab-01 already fixed:

* **QA-4 (Modifiability, H, L)** — "Head-office analyst asks for Excel
  (XLSX) output: ≤ 2 person-days, ≤ 3 files changed outside the new
  renderer class, `mvn -q verify` exits 0."

## Decision

We keep **Model View Presenter** for the desktop viewer of the following
labs, and we keep the MVC variant in the repository as the reference
implementation of the pattern.

The reason is QA-6. In the MVP variant every text that the user reads is
produced by a class without a toolkit, so it can be checked without a
window: `ReportPresenterTest` asserts the exact status line
(`PNH · 1 receipts · revenue USD 31.00 · basket USD 31.00`) and the exact
row (`RowItem("2026-09-01", "PNH-000001", "Power Bank 10000mAh",
"Electronics", "1", "18.00", "0.90")`). In the MVC variant the status line
lives inside the JavaFX class, so only a screenshot can check it — which is
exactly what `lab02/screenshots/mvc-viewer.png` is.

The second reason is the web page that the same lab asks for (the Spring MVC
challenge). A passive screen is the right shape for a second audience: the
presenter and its tests can be reused, and only a new `ReportScreen` (a
Thymeleaf template) is needed. This is the same idea as QA-4: one new
presentation, the rules and the model stay where they are.

## Alternatives considered

- **Keep MVC.** It is one class less and one indirection less (145 + 69
  lines against 122 + 92 + 45), and a new table column is a change in one
  file instead of three. We did not choose it because the text of the screen
  then lives in the toolkit class, where QA-6 cannot reach it, and because
  the second screen (the web page) would have to repeat the formatting.
- **Keep both.** Both variants are in the repository, but only one of them
  may be the pattern that the next labs extend. Two live patterns cost
  twice: every new lab would have to decide again, and half of the changes
  would land in the variant that is not used.
- **A third pattern (a shared formatting helper only).** We did part of it:
  `ui/ReportText` builds the status line for both views without a toolkit.
  That keeps the two variants comparable, but it does not replace the
  presenter: the MVP presenter is still the only one that can be tested
  end to end.

## Consequences

- Positive: every text of the screen is covered by a test that needs no
  display (19 tests for the variant, 47 for the lab, the whole suite runs in
  about a second) — QA-6.
- Positive: the screen holds no model reference, so a second screen (the
  Thymeleaf page of the challenge, a Swing screen later) is a new
  implementation of `ReportScreen` and nothing else — QA-4.
- Positive: the domain (`model`, `ingest`, `engine`) and the rules
  (`InputRules`, `Validation`) are the same for both variants, so the
  choice does not touch the packages that the later labs build on.
- Negative: a change in the table costs three classes instead of one
  (measured in `lab02/mvc-vs-mvp.md`): the row record, the mapping and the
  column.
- Negative: the presenter is a second place that knows the shape of the
  report. A field that is added to the model has to be added to `RowItem`
  and to the mapping, and the compiler only helps for the first.
- Negative: one indirection more between the model and the screen, so a
  newcomer has to read `SalesModel` → `ReportPresenter` → `ReportScreen` →
  `FxReportScreen` before understanding the screen.
- Negative: the MVC variant stays in the code base. It is kept as the
  reference of the pattern and as the subject of the comparison, but it is
  not maintained any more; a change is only made in the MVP variant.

Revisit when: a second screen appears that cannot be driven by a presenter
(a batch export for example), or when the extra indirection of the
presenter costs more than the tests it buys.

# lab02/mvc-vs-mvp.md — the same screen in MVC and in MVP

Both variants show the same screen and the same figures for 2026-09:
3,600 rows, six bars, revenue USD 26,721.90, 1,493 receipts
(`lab02/screenshots/mvc-viewer.png` and `lab02/screenshots/mvp-viewer.png`).
The numbers of this table were measured on our own code.

| Criterion | MVC (ReportView + ReportController) | MVP (FxReportScreen + ReportPresenter) |
|---|---|---|
| Lines of code (view / controller-presenter) | 145 / 69 | 122 / 92 (plus 45 for the screen interface and the row record) |
| Tests that run without JavaFX | 17 that cover the variant (4 model, 9 rules, 4 controller) | 19 that cover the variant (the same 17 and 2 that check the text of the presenter) |
| Where money and status text are formatted | in the view: `ReportView` builds the status line, the model hands over `BigDecimal` | in the presenter: `ReportPresenter.money()` and the status text; the screen only draws strings |
| Files changed to add a "Discount" column | 1: `ui/fx/ReportView.java` (the model already has the discount) | 3: `ui/mvp/RowItem.java` (a new component), `ui/mvp/ReportPresenter.java` (the mapping), `ui/fx/FxReportScreen.java` (the column) |
| Does the view hold a model reference? | yes: `ReportView` holds the `SalesModel` and registers itself as a listener | no: `FxReportScreen` knows the presenter and the setters of the screen, never the model |
| Effort for a second screen (web, Swing) | the view has to be written again and it needs the model and the observer | the presenter and its tests can be reused; only a new `ReportScreen` is needed |

## The rest of the numbers

| | MVC | MVP |
|---|---|---|
| Classes of the variant | `SalesModel`, `SalesEvent`, `SalesModelListener`, `ReportController`, `InputRules`, `Validation`, `ReportCommand`, `ErrorDisplay`, `ReportView` | the same, plus `ReportScreen`, `RowItem`, `ReportPresenter` |
| Text of the screen covered by a test | only the rules and the model, not the status line of the view | the exact status line and the exact row of the presenter |
| Toolkit in the core | none (checked by `UiIndependenceTest`) | none (checked by `UiIndependenceTest`, `ui/mvp` included) |
| Screen knows the domain | yes: the table shows `SaleTransaction` objects | no: the table shows `RowItem` records of strings |

## What the "Discount" column cost

The column was added to both variants, as the brief asks.

* **MVC** — the model already had the discount (`SaleTransaction.discount()`),
  so only the view changed: one more column in `ReportView`.
* **MVP** — the screen may only show what the presenter pushes, so the
  discount had to travel through the presenter: a new component in the
  `RowItem` record, the mapping in `ReportPresenter.toRow()` and the column
  in `FxReportScreen`. The presenter test had to be extended with the
  discount of the row it checks.

This is the sharpest difference between the two variants: in MVC a column is
a change in one class, in MVP it is a change in three classes and in the
tests. The price of MVP is that the text of the screen is testable and that
the screen can be replaced without touching the model.

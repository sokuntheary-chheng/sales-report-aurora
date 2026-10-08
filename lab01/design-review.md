# Design review: cohesion and coupling of release 1

Scope: `src/main/java` (production code). Counted as *distinct project packages* on `import edu.itc.salesreport.*` lines. Test code sits in the same packages as the code it tests and adds no new edge, so the numbers do not change if tests are counted. `I = Ce / (Ca + Ce)`.

| Package | Responsibility (cohesion, one sentence)                          | Ce | Ca | I = Ce/(Ca+Ce)        |
| ------ | --------------------------------------------------------------- | -- | -- | --------------------- |
| model  | Holds the canonical sales facts (`SaleTransaction`, totals, summaries) and their invariants, and knows nothing about files or formats. | 0  | 2  | 0/(2+0) = **0.00**    |
| ingest | Turns the daily CSV files into `SaleTransaction`s and publishes load progress. | 1  | 1  | 1/(1+1) = **0.50**    |
| render | Turns one `MonthlyReport` into the bytes of one output format.   | 1  | 0  | 1/(0+1) = **1.00**    |
| (root) `App` | Composition root: wires parser, loader and observers, then runs. | 1  | 0  | 1/(0+1) = **1.00**    |

- Ce/Ca evidence: `model` has zero project imports; `ingest` imports `model` (3 files); `render` imports `model` (4 files); `App` imports `ingest` (3 lines). Nobody imports `render` yet, so `Ca(render) = 0` and its `I` is 1.00 — it is the package most likely to change, which is exactly what we want from a Strategy: new formats arrive there and nowhere else.
- Cohesion check: each package has one reason to change — the model when the brief's invariants change, ingest when the POS export format changes, render when a file format is added. `DependencyRulesTest` enforces the two hard edges: `model` imports no other project package, and `ingest` never imports `render`.
- Coupling accepted: **`ingest → model`**. Producing the canonical records *is* the parser's job, and it points at a package with `I = 0.00` that can never depend back on it. The alternative — a second, ingest-local "row" type plus a mapping layer — would add a file, a conversion and a place for the two shapes to drift, for no gain.

**Sealed hierarchy or open interface for QA-4?** The sealed `ReportRenderer` serves QA-4 (new export format in ≤ 2 person-days, ≤ 3 files changed) better than an open interface. Adding XLSX touches exactly three places — the new class, `permits`, and `forExtension` — and the compiler then flags every non-exhaustive `switch`, so a forgotten format is a build failure instead of a missing file in the head office's inbox. An open interface would make step 1 a one-line `implements` but would silently leave `forExtension` and the exhaustive switches broken, and it buys extensibility for third-party plugins that no stakeholder asked for. Cost to accept: every new format edits the same two lines of `ReportRenderer`, so we must merge that file deliberately.

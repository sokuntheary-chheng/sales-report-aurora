# sales-report-aurora

The monthly sales report of the Angkor Mart chain, built as a Maven project
for the Software Engineering course (Labs 02 to 08 extend it).

Team: Aurora · Member: sokuntheary-chheng (Chheng Sokuntheary)

| | |
|---|---|
| Maven | `edu.itc:sales-report`, root package `edu.itc.salesreport` |
| Versions | JDK 25 · Maven 3.9 · JavaFX 25.0.4 · JUnit 6.1 |
| Data | one CSV per branch and day, `data/<yyyy-MM>/<BRANCH>-<yyyy-MM-dd>.csv` |

## The labs

| Folder | What it holds |
|---|---|
| `lab01/` | the architecture foundations: C4 diagrams, quality-attribute scenarios, two ADRs |
| `lab02/` | the MVC viewer: the report of the month as a desktop application, the same screen as MVP, the notes of the lab |
| `lab03/` | the MVVM refactor: the observable ViewModel behind the JavaFX view, tests without a toolkit, load measurements, the MVC vs MVVM comparison |
| `sales-web/` | the Lab-02 challenge: the same model on a Spring MVC page |

## The structure

```
sales-report-aurora/
├── pom.xml                     the Maven project of the chain (JavaFX optional)
├── .gitignore
├── src/main/java/edu/itc/salesreport/
│   ├── model/                  the domain: SaleTransaction, BranchSummary, ProductTotal, PaymentMethod
│   ├── ingest/                 the reading of the files: the parser, the month reader, the sample generator
│   ├── engine/                 the figures: the aggregator
│   ├── render/                 the Lab-01 batch output (txt, html, csv)
│   ├── ui/                     the Lab-03 MVVM screen: ReportViewModel, ReportView, ReportViewerApp,
│   │   │                       the ports (BackgroundJobs, SummarySource), the commands and the jobs
│   │   ├── console/            the console view (no toolkit)
│   │   ├── fx/                 the JavaFX adapters of the MVP: FxReportScreen, ReportMvpApp
│   │   ├── mvc/                the Lab-02 MVC screen, moved here in Lab-03: ReportController
│   │   │   └── fx/             its JavaFX screens: ReportView, ReportApp, ReportFxml*, FatReportController
│   │   └── mvp/                the MVP: ReportPresenter, ReportScreen, RowItem
│   └── App.java                the composition root of the Lab-01 batch
├── src/main/resources/edu/itc/salesreport/ui/mvc/fx/report.fxml   the layout of the FXML variant
├── src/test/java/edu/itc/salesreport/   67 tests, none of them starts JavaFX
├── lab01/  lab02/  lab03/
├── sales-web/                  the Spring MVC page of the challenge
└── tools/SampleData.java       the sample generator of Lab-01
```

The packages `model`, `ingest`, `engine`, `ui`, `ui/mvc` and `ui/mvp` must
never import a user-interface toolkit (`javafx.scene`, `java.awt`,
`javax.swing`); the test `edu.itc.salesreport.UiIndependenceTest` makes
that rule executable. The Lab-03 view layer lives directly in `ui` and may
use `javafx.base` only — `ViewModelImportsTest` enforces that on
`ReportViewModel`.

## How to build and run

```bash
mvn -q verify                 # the whole suite; no display needed

# the sample data of the lab (two months, reproducible from the seed)
java -cp target/classes edu.itc.salesreport.ingest.SalesDataGenerator data

# the console view of the report
java -cp target/classes edu.itc.salesreport.ui.console.ConsoleReportView data

# the desktop viewer (a desktop session is needed for the window)
mvn javafx:run          # Lab-03: the MVVM viewer (pom property app.main)

# the Lab-02 screens, kept for the comparison
mvn javafx:run -Djavafx.mainClass=edu.itc.salesreport.ui.mvc.fx.ReportApp
mvn javafx:run -Djavafx.mainClass=edu.itc.salesreport.ui.mvc.fx.ReportFxmlApp
mvn javafx:run -Djavafx.mainClass=edu.itc.salesreport.ui.fx.ReportMvpApp
mvn javafx:run -Djavafx.mainClass=edu.itc.salesreport.ui.mvc.fx.ReportApp -Djavafx.args="2026-08 PNH"
```

The web page of the challenge:

```bash
mvn -q install                 # publish edu.itc:sales-report:0.3.0-SNAPSHOT
cd sales-web && mvn -q verify
java -jar target/sales-web-0.2.0-SNAPSHOT.jar --salesreport.data-dir=../data
```

Lab-03 is documented in `lab03/README.md` (what changed, reused inputs,
deviations), with `lab03/viewmodel-design.md`, `lab03/measurements.md` and
`lab03/mvc-vs-mvvm.md` beside it.

## The figures of 2026-09

```
Loaded 2026-09: 3600 rows, 0 rejected
  ALL  revenue  26721.90  receipts 1493  basket 17.90
  PNH  revenue   8455.90  receipts  489  basket 17.29
```

The desktop viewer and the web page show the same figures, because both ask
the same model.

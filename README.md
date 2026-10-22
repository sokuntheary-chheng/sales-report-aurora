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
│   ├── ui/                     the MVC: SalesModel, SalesEvent, ReportController, InputRules, ErrorDisplay
│   │   ├── console/            the console view (no toolkit)
│   │   ├── fx/                 the JavaFX adapters: ReportView, ReportApp, FxReportScreen, ReportMvpApp
│   │   │   └── legacy/         the fat controller of Lab-02, refactored
│   │   └── mvp/                the MVP: ReportPresenter, ReportScreen, RowItem
│   └── App.java                the composition root of the Lab-01 batch
├── src/main/resources/edu/itc/salesreport/ui/fx/report.fxml   the layout of the FXML variant
├── src/test/java/edu/itc/salesreport/   47 tests, none of them starts JavaFX
├── lab01/  lab02/
├── sales-web/                  the Spring MVC page of the challenge
└── tools/SampleData.java       the sample generator of Lab-01
```

The packages `model`, `ingest`, `engine`, `ui` and `ui/mvp` must never import
`javafx`, `java.awt` or `javax.swing`; the test
`edu.itc.salesreport.UiIndependenceTest` makes that rule executable.

## How to build and run

```bash
mvn -q verify                 # the whole suite; no display needed

# the sample data of the lab (two months, reproducible from the seed)
java -cp target/classes edu.itc.salesreport.ingest.SalesDataGenerator data

# the console view of the report
java -cp target/classes edu.itc.salesreport.ui.console.ConsoleReportView data

# the desktop viewer (a desktop session is needed for the window)
mvn javafx:run
mvn javafx:run -Djavafx.mainClass=edu.itc.salesreport.ui.fx.ReportMvpApp
mvn javafx:run -Djavafx.mainClass=edu.itc.salesreport.ui.fx.ReportFxmlApp
mvn javafx:run -Djavafx.args="2026-08 PNH"    # open on a month and a branch
```

The web page of the challenge:

```bash
mvn -q install                 # publish edu.itc:sales-report:0.2.0-SNAPSHOT
cd sales-web && mvn -q verify
java -jar target/sales-web-0.2.0-SNAPSHOT.jar --salesreport.data-dir=../data
```

## The figures of 2026-09

```
Loaded 2026-09: 3600 rows, 0 rejected
  ALL  revenue  26721.90  receipts 1493  basket 17.90
  PNH  revenue   8455.90  receipts  489  basket 17.29
```

The desktop viewer and the web page show the same figures, because both ask
the same model.

# Lab-01: architecture foundations of the sales report system

Team: Aurora · Members: sokuntheary-chheng (sokuntheary-chheng)

## Files produced in this lab

| File                                              | Purpose                           | Author(s) |
| ------------------------------------------------- | --------------------------------- | --------- |
| lab01/quality-scenarios.md                        | QA-1..QA-6, utility tree, drivers | sokuntheary-chheng |
| lab01/c4-context.mmd, .png                        | C4 level 1                        | sokuntheary-chheng |
| lab01/c4-container.mmd, .png                      | C4 level 2, release 1             | sokuntheary-chheng |
| lab01/README.md                                   | this file: C4 notes, record what changed | sokuntheary-chheng |
| lab01/design-review.md                            | cohesion and coupling review      | sokuntheary-chheng |
| lab01/adr/0001-architecture-style-release-1.md    | ADR-0001: modular monolith batch  | sokuntheary-chheng |
| lab01/adr/0002-branch-to-head-office-data-flow.md | ADR-0002: branch push over SFTP, partial-report policy | sokuntheary-chheng |
| pom.xml, src/\*\*, tools/SampleData.java          | sales-report 0.1.0-SNAPSHOT       | sokuntheary-chheng |

## C4 notes

### `c4-context.mmd` — level 1, system in context

- The context diagram deliberately does not show the six containers inside the system, the CSV column layout, or any class or package — level 1 answers only "who talks to the system and with which external system", and the finance director is drawn as a person who receives e-mail rather than as a user who logs in.
- The branch POS boxes serve **QA-3 (H, M)**: they are the thing whose link can be two hours late, and the system boundary is where a missing upload must turn into a partial report instead of a failure; the system box itself serves **QA-1 (H, H)**, because the single "produces monthly reports" responsibility has to hold 3 M rows inside 60 s.
- Open question for the client: does the finance director ever need to log in and open a report, or is a report-ready e-mail the whole interaction?

### `c4-container.mmd` — level 2, release 1 containers

- The container diagram deliberately does not show the model records, the parser or renderer classes, the internal method calls of the report batch, or any deployment/host detail — it shows only separately runnable or deployable units and the data stores between them, each with its technology and each connector with its protocol.
- The landing zone serves **QA-3 (H, M)**: keeping `BRANCH-yyyy-MM-dd.csv` as a file on disk makes a re-run idempotent (a re-upload replaces, never duplicates), and the report store serves **QA-5 (H, M)**: it is the one choke point from which the web report page must filter by branch so a manager can never read another branch's figures.
- Open question for the client: is a directory of rendered files an acceptable report store for release 1, or does head office need the reports in a queryable database from day one?

## Inputs reused from earlier labs

None (first lab).

## Change log

| Date       | Lab    | Change                                                    | Commit or tag |
| ---------- | ------ | --------------------------------------------------------- | ------------- |
| 2026-10-08 | Lab-01 | Project created: model, parser, renderers, loader; 2 ADRs | lab-01        |

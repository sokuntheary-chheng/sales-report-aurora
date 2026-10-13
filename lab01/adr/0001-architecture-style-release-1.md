# ADR-0001: Build release 1 as a modular monolith batch job in one JVM

Status: Accepted
Date: 2026-10-08 · Deciders: sokuntheary-chheng

## Context

Head office wants the report for month M on the morning of day 2 of M+1, built from about 3 M rows a month (QA-1: ≤ 60 s on an 8-core server). One branch file can be missing at start time and must not stop the run (QA-3: REP file 2 h late, others out by 07:00). Branch managers must never see another branch's figures (QA-5). Constraints from the brief: a small team, an eight-week semester, JDK 25 and Maven only, no operations team to run a broker, and one deployment target (the report server). `lab01/c4-container.mmd` already fixes release 1 at six containers in one system boundary.

## Decision

We will build release 1 as a **modular monolith batch job**: one JVM, launched once a day at 06:00, with the packages `model`, `ingest`, `engine`, `render` inside one Maven artifact. Ingest reads the landing-zone files, the engine aggregates in the same process, renderers turn the `MonthlyReport` into files through the sealed `ReportRenderer` Strategy, and progress leaves the process as `LoadEvent`s (Observer). The deployable unit is the jar; the data store is the file system.

## Alternatives considered

- **B: process pipeline** — a coordinator plus worker processes over pipes or local sockets. Attractive because parsing could be spread over 4–6 workers without threading, which helps QA-1. We did not choose it: every hop needs serialization, a protocol and crash supervision, which turns a worker failure into the lost or partial file that QA-3 is trying to avoid, and N binaries multiply what we must build and test in eight weeks.
- **C: event-driven microservices** — ingest, aggregation, report and notification services behind a message broker. Attractive because the broker buffers late files for QA-2 and QA-3 and lets each branch scale independently. We did not choose it: per-receipt events do not fit QA-1 (the ATAM estimate puts a broker at 50 000 msg/s, so 3 M receipts is 60 s of broker time alone), a broker is an operated component that enlarges QA-5's attack surface and we have never run one.

## Consequences

- Positive: no IPC and no serialization on the hot path, so the whole 3 M rows stay in one heap and QA-1 depends only on our parse and aggregate code (QA-1).
- Positive: a re-run is the same program over the same directory, so the failure policy for QA-3 is one command and no external state to reconcile (QA-3).
- Positive: one process means one place to enforce "own branch only" before anything is rendered, and `mvn test` covers everything with no services to start (QA-5, QA-6).
- Negative: scaling stops at the cores of that one box — a month at 10 M rows or a slower disk would miss the 60 s of QA-1 (QA-1, QA-2).
- Negative: no isolation — an OutOfMemoryError or an infinite loop in a renderer takes ingest down with it, and head office gets no report at all until the re-run (QA-3).
- Negative: a new output format redeploys the whole batch, so a fix in `render` restarts the daily job for everyone (QA-4).

Revisit when: a single month exceeds 10 M rows, one run on the 8-core server passes 45 s, or a second consumer needs the data while the batch is running.

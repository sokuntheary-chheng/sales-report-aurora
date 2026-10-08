# ADR-0002: Have each branch push one CSV file per day to head office over SFTP

Status: Accepted
Date: 2026-10-08 · Deciders: sokuntheary-chheng

## Context

Each branch's POS exports one file per day by 23:00 over its own internet link, and those links fail now and then (brief). Head office needs month M on the morning of day 2 of M+1, and QA-3 says: REP's file is 2 h late, the other branches' reports are still out by 07:00. Branch managers may only see their own branch (QA-5), so head office must be the single place that holds every branch's data. The landing zone in `lab01/c4-container.mmd` is a directory, and `MonthLoader` already reads `data/<yyyy-MM>/BRANCH-<yyyy-MM-dd>.csv` in file-name order.

## Decision

We will let **each branch push one file per day** into head office's landing zone over SFTP, named `BRANCH-yyyy-MM-dd.csv`. Head office does not pull and the POS does not stream events; the batch simply reads whatever has arrived by the cut-off. Head office holds the only read credentials, each branch gets a write-only drop folder for its own `BRANCH` prefix.

**Failure policy for QA-3.** The batch starts at 06:00 and re-checks for a missing branch file every 5 minutes until **06:45**. If a file is still absent, the report is **published as partial at 07:00** for the branches that are present, with the missing branch named in the `anomalies` list — head office never waits past the cut-off. When the late REP file lands (say 08:00), an operator re-runs the batch; it recomputes the whole month from the current file set and replaces the report files, so the late data is merged by **recomputation, not by patching**. **Idempotency** is keyed on the file name `BRANCH-yyyy-MM-dd.csv` plus a SHA-256 checksum stored in the report store's manifest: the same name with the same checksum is ignored, the same name with a different checksum **replaces** the old file (it never appends), and rendered reports are overwritten. A re-run therefore cannot duplicate a day or double-count a receipt.

## Alternatives considered

- **B: head office pulls from each branch** — head office polls every branch over SFTP/HTTPS and owns retries and scheduling. Attractive because no agent is needed on the branches. We did not choose it: head office would have to hold credentials *inside* three branch networks (QA-5), and polling does not remove the failure — a link that is down at 06:00 is still down when head office retries, so QA-3's guarantee would depend on our poller rather than on the branch's own retry.
- **C: the POS publishes one event per receipt to a message broker** — the broker absorbs late and out-of-order data natively and gives intraday figures. We did not choose it: 3 M receipts a day through a broker is 60 s of broker time at 50 000 msg/s before a single row is aggregated (QA-1), every POS vendor would have to change (outside our control), and a broker plus its credentials is an operated component and a new attack surface (QA-5, cost).

## Consequences

- Positive: a branch outage costs exactly one file, so PNH and BTB still reach head office by 07:00 and QA-3's partial-report rule has a precise unit (QA-3).
- Positive: plain files in a directory are trivial to fake in `@TempDir`, to checksum and to time, which keeps QA-6's 30 s test suite and QA-1's measurement honest (QA-6, QA-1).
- Positive: branches only need a write-only drop folder, so a compromised branch account cannot read another branch's figures (QA-5).
- Negative: a partial report can be mistaken for the final one, so the missing branch must be labelled loudly — a real risk of the finance director signing off incomplete numbers (QA-3, QA-5).
- Negative: the branch has exactly one window (23:00 upload, 06:00–06:45 cut-off); a link that stays down all night cannot be recovered inside the deadline (QA-3).
- Negative: each site needs an agent or cron job to push, so a fourth branch means a fourth thing to install and keep alive (QA-2, QA-4).

Revisit when: a branch file misses the cut-off twice in one quarter, head office asks for intraday figures, or a branch cannot run the push agent.

# NL→SQL engine — production readiness

## What this is

An internal analytics tool. Users type a question in plain English, we generate SQL,
run it against the analytics database, and show them the answer.

    question  ->  LLM  ->  execute  ->  result

That is the entire pipeline today. See `QueryPipeline.java`.

## Where we are

- **SLA: p95 under 1.5 seconds**, end to end. Product is firm on this — the tool sits
  inside a dashboard and users type follow-up questions.
- **~15% of generations are unusable.** Some fail at the database, some don't.
- Leadership wants **99%+ correct answers** before this goes to the whole company.

## Measured stage costs

| Stage | p50 | p95 | worst observed |
|---|---|---|---|
| LLM generation | 700 ms | 1300 ms | 1450 ms |
| LLM repair call (same model, "fix this SQL") | 810 ms | 900 ms | — |
| SQL parse / AST walk | ~5 ms | ~8 ms | — |
| Schema validation against catalog | ~2 ms | ~4 ms | — |
| SQL execution | 10 ms | 70 ms | 2800 ms |
| Serialization + network | ~30 ms | ~40 ms | — |

## Your task

Design the production execution engine. You have the running system to poke at.

## Running it

    mvn spring-boot:run

First start seeds `data/analytics.db` (a few seconds). Then:

    ./ask.sh "How many orders were placed in Q3 2025?"

    GET  /v1/schema          the table definitions
    GET  /v1/metrics         per-query timings, SLA breaches, error rate
    POST /v1/metrics/reset

`StubLlmClient` stands in for the hosted model so latency and outputs are reproducible.
Treat it as a black box that costs what the table above says.

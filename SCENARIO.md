# NL→SQL engine — production readiness

## What this is

An internal analytics tool. Users type a question in plain English, we generate SQL,
run it against the analytics database, and show them the answer.

    question  ->  LLM  ->  execute  ->  result

That is the entire pipeline today. See `QueryPipeline.java`.

## Where we are

- **~15% of generations are unusable.** Some fail at the database, some don't.
- Leadership wants **99%+ correct answers** before this goes to the whole company.

## Your task

Design the production execution engine. You have the running system to poke at.

## Running it

    ./run.sh

First start seeds `data/analytics.db` (a few seconds). Then:

    ./ask.sh "How many orders were placed in Q3 2025?"

    GET  /v1/schema          the table definitions
    GET  /v1/metrics         per-query timings, SLA breaches, error rate
    POST /v1/metrics/reset

`StubLlmClient` stands in for the hosted model so latency and outputs are reproducible.
Treat it as a black box.

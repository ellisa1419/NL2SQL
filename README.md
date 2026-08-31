# nl2sql-engine

Natural-language-to-SQL service. Java 17+, Spring Boot 3.3, SQLite.

    mvn spring-boot:run

Port 8080. The analytics database is a local SQLite file at `data/analytics.db`,
seeded automatically on first start (35,000 orders / ~70,000 order items).
Delete the file to reseed.

    ./ask.sh "How many orders were placed in Q3 2025?"

## Layout

    web/QueryController      HTTP surface
    pipeline/QueryPipeline   the current flow: generate -> execute
    llm/LlmClient            model interface
    llm/StubLlmClient        reproducible stand-in for the hosted model
    exec/SqlExecutor         runs SQL against SQLite
    metrics/PipelineMetrics  timings, SLA breaches, error rate
    data/DataSeeder          builds the database on first run

Read `SCENARIO.md` first.

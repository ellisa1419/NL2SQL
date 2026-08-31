# nl2sql-engine

Natural-language-to-SQL service. Plain Java 17, no build tool, no framework. SQLite.

    ./run.sh

Port 8080. The analytics database is a local SQLite file at `data/analytics.db`,
seeded automatically on first start (35,000 orders / ~70,000 order items).
Delete the file to reseed.

    ./ask.sh "How many orders were placed in Q3 2025?"
    PORT=8081 ./ask.sh "..."      if you moved the server off 8080

## Build and run

    ./build.sh          compile src/main/java -> out/ (also copies resources)
    ./build.sh clean    wipe out/ first
    ./run.sh            build if sources changed, then serve
    ./fetch-libs.sh     re-download lib/*.jar from Maven Central if lib/ is missing

`run.sh` passes any `-Dkey=value` arguments through to the JVM, and every key in
`src/main/resources/application.properties` can be overridden that way:

    ./run.sh -Dserver.port=8081 -Dengine.sla-ms=3000

Runtime jars live in `lib/`: sqlite-jdbc, jackson (core/databind/annotations),
slf4j-api, logback (classic/core). Nothing else is required.

## Layout

    Nl2SqlApplication        entry point; wires the object graph by hand
    config/EngineConfig      application.properties + -D overrides
    web/HttpApi              HTTP transport on the JDK's built-in server
    web/QueryController      request/response shaping
    pipeline/QueryPipeline   the current flow: generate -> execute
    llm/LlmClient            model interface
    llm/StubLlmClient        reproducible stand-in for the hosted model
    exec/SqlExecutor         runs SQL against SQLite
    db/Database              connection settings
    metrics/PipelineMetrics  timings, SLA breaches, error rate
    data/DataSeeder          builds the database on first run

Read `SCENARIO.md` first.

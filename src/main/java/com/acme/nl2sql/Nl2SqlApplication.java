package com.acme.nl2sql;

import com.acme.nl2sql.config.EngineConfig;
import com.acme.nl2sql.data.DataSeeder;
import com.acme.nl2sql.db.Database;
import com.acme.nl2sql.exec.SqlExecutor;
import com.acme.nl2sql.llm.LlmClient;
import com.acme.nl2sql.llm.StubLlmClient;
import com.acme.nl2sql.metrics.PipelineMetrics;
import com.acme.nl2sql.pipeline.QueryPipeline;
import com.acme.nl2sql.web.HttpApi;
import com.acme.nl2sql.web.QueryController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point. Wires the object graph by hand, seeds the database, serves HTTP.
 */
public final class Nl2SqlApplication {

    private static final Logger log = LoggerFactory.getLogger(Nl2SqlApplication.class);

    private Nl2SqlApplication() { }

    /**
     * @param args ignored; settings come from {@code application.properties} and {@code -D} overrides
     */
    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();
        EngineConfig config = EngineConfig.load();

        Database database = new Database(config.get("datasource.url", "jdbc:sqlite:data/analytics.db"));
        new DataSeeder(database).seed();

        LlmClient llm = new StubLlmClient();
        SqlExecutor executor = new SqlExecutor(database);
        PipelineMetrics metrics = new PipelineMetrics();
        QueryPipeline pipeline = new QueryPipeline(llm, executor, metrics, config.getLong("engine.sla-ms", 1500));
        QueryController controller = new QueryController(pipeline, metrics);

        HttpApi api = new HttpApi(controller,
                config.get("server.host", "0.0.0.0"),
                config.getInt("server.port", 8080));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> api.stop(3), "shutdown"));
        api.start();

        log.info("nl2sql-engine started in {}ms", System.currentTimeMillis() - t0);
    }
}

package com.acme.nl2sql.pipeline;

import com.acme.nl2sql.exec.SqlExecutor;
import com.acme.nl2sql.llm.LlmClient;
import com.acme.nl2sql.metrics.PipelineMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Current production flow.
 *
 *      question  ->  LLM  ->  execute  ->  result
 *
 * No validation, no retry, no fallback, no cache.
 */
public class QueryPipeline {

    private static final Logger log = LoggerFactory.getLogger(QueryPipeline.class);

    private final LlmClient llm;
    private final SqlExecutor executor;
    private final PipelineMetrics metrics;
    private final long slaMs;

    public QueryPipeline(LlmClient llm, SqlExecutor executor, PipelineMetrics metrics, long slaMs) {
        this.llm = llm;
        this.executor = executor;
        this.metrics = metrics;
        this.slaMs = slaMs;
    }

    public QueryResult answer(String question) {
        long start = System.currentTimeMillis();

        LlmClient.Completion completion = llm.generate(question);
        long llmMs = System.currentTimeMillis() - start;

        long execStart = System.currentTimeMillis();
        List<Map<String, Object>> rows;
        String error = null;
        boolean ok = true;
        try {
            rows = executor.run(completion.sql());
        } catch (Exception e) {
            rows = List.of();
            ok = false;
            error = rootMessage(e);
        }
        long execMs = System.currentTimeMillis() - execStart;
        long totalMs = System.currentTimeMillis() - start;
        boolean breached = totalMs > slaMs;

        log.info("q=\"{}\" ok={} llm={}ms exec={}ms total={}ms sla={}",
                question, ok, llmMs, execMs, totalMs, breached ? "BREACH" : "ok");
        if (!ok) {
            log.warn("execution failed: {}", error);
        }

        QueryResult result = new QueryResult(question, completion.sql(), rows, ok, error,
                llmMs, execMs, totalMs, breached);
        metrics.record(result);
        return result;
    }

    private String rootMessage(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null) r = r.getCause();
        return r.getMessage();
    }
}

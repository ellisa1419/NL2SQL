package com.acme.nl2sql.web;

import com.acme.nl2sql.metrics.PipelineMetrics;
import com.acme.nl2sql.pipeline.QueryPipeline;
import com.acme.nl2sql.pipeline.QueryResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shapes the HTTP payloads for the query endpoints. Transport lives in {@link HttpApi}.
 */
public class QueryController {

    private final QueryPipeline pipeline;
    private final PipelineMetrics metrics;

    public QueryController(QueryPipeline pipeline, PipelineMetrics metrics) {
        this.pipeline = pipeline;
        this.metrics = metrics;
    }

    /**
     * Answers a natural-language question.
     *
     * @param body decoded request body, expected to carry a {@code question} field
     * @return the response body for {@code POST /v1/query}
     */
    public Map<String, Object> query(Map<String, String> body) {
        QueryResult r = pipeline.answer(body.get("question"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("question", r.question());
        out.put("sql", r.sql());
        out.put("ok", r.ok());
        if (r.error() != null) out.put("error", r.error());
        out.put("rows", r.rows());
        out.put("timing", Map.of("llmMs", r.llmMs(), "execMs", r.execMs(),
                "totalMs", r.totalMs(), "sla", r.slaBreached() ? "BREACH" : "ok"));
        return out;
    }

    /**
     * @return the response body for {@code GET /v1/metrics}
     */
    public Map<String, Object> metrics() {
        return metrics.snapshot();
    }

    /**
     * Clears the collected metrics.
     *
     * @return the response body for {@code POST /v1/metrics/reset}
     */
    public Map<String, Object> reset() {
        metrics.reset();
        return Map.of("reset", true);
    }

    /**
     * @return the response body for {@code GET /v1/schema}
     */
    public String schema() {
        return SchemaDoc.TEXT;
    }
}

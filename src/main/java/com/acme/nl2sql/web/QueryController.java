package com.acme.nl2sql.web;

import com.acme.nl2sql.metrics.PipelineMetrics;
import com.acme.nl2sql.pipeline.QueryPipeline;
import com.acme.nl2sql.pipeline.QueryResult;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class QueryController {

    private final QueryPipeline pipeline;
    private final PipelineMetrics metrics;

    public QueryController(QueryPipeline pipeline, PipelineMetrics metrics) {
        this.pipeline = pipeline;
        this.metrics = metrics;
    }

    @PostMapping("/v1/query")
    public Map<String, Object> query(@RequestBody Map<String, String> body) {
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

    @GetMapping("/v1/metrics")
    public Map<String, Object> metrics() {
        return metrics.snapshot();
    }

    @PostMapping("/v1/metrics/reset")
    public Map<String, Object> reset() {
        metrics.reset();
        return Map.of("reset", true);
    }

    @GetMapping("/v1/schema")
    public String schema() {
        return SchemaDoc.TEXT;
    }
}

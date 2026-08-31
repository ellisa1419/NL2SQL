package com.acme.nl2sql.metrics;

import com.acme.nl2sql.pipeline.QueryResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PipelineMetrics {

    private final List<QueryResult> results = new ArrayList<>();

    public synchronized void record(QueryResult r) {
        results.add(r);
    }

    public synchronized void reset() {
        results.clear();
    }

    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        int n = results.size();
        m.put("queries", n);
        if (n == 0) return m;

        long executed = results.stream().filter(QueryResult::ok).count();
        long breaches = results.stream().filter(QueryResult::slaBreached).count();

        m.put("executedWithoutError", executed);
        m.put("executionErrorRate", pct(n - executed, n));
        m.put("slaBreaches", breaches);
        m.put("slaBreachRate", pct(breaches, n));
        m.put("totalP50Ms", percentile(results.stream().map(QueryResult::totalMs).toList(), 50));
        m.put("totalP95Ms", percentile(results.stream().map(QueryResult::totalMs).toList(), 95));
        m.put("llmP50Ms", percentile(results.stream().map(QueryResult::llmMs).toList(), 50));
        m.put("llmP95Ms", percentile(results.stream().map(QueryResult::llmMs).toList(), 95));
        m.put("execP50Ms", percentile(results.stream().map(QueryResult::execMs).toList(), 50));
        m.put("execP95Ms", percentile(results.stream().map(QueryResult::execMs).toList(), 95));

        List<Map<String, Object>> per = new ArrayList<>();
        for (QueryResult r : results) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("question", r.question());
            row.put("ok", r.ok());
            row.put("llmMs", r.llmMs());
            row.put("execMs", r.execMs());
            row.put("totalMs", r.totalMs());
            row.put("sla", r.slaBreached() ? "BREACH" : "ok");
            row.put("rows", r.rows().size());
            if (r.error() != null) row.put("error", r.error());
            per.add(row);
        }
        m.put("perQuery", per);
        return m;
    }

    private String pct(long a, long b) {
        return b == 0 ? "0%" : String.format("%.1f%%", (100.0 * a) / b);
    }

    private long percentile(List<Long> xs, int p) {
        List<Long> s = new ArrayList<>(xs);
        s.sort(null);
        if (s.isEmpty()) return 0;
        int i = (int) Math.ceil((p / 100.0) * s.size()) - 1;
        return s.get(Math.max(0, Math.min(i, s.size() - 1)));
    }
}

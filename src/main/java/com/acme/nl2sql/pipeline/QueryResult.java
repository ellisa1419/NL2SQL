package com.acme.nl2sql.pipeline;

import java.util.List;
import java.util.Map;

public record QueryResult(
        String question,
        String sql,
        List<Map<String, Object>> rows,
        boolean ok,
        String error,
        long llmMs,
        long execMs,
        long totalMs,
        boolean slaBreached) { }

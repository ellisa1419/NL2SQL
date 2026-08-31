package com.acme.nl2sql.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stand-in for the hosted model.
 *
 * The production engine calls a hosted LLM. For this exercise the call is stubbed so
 * latency and failure modes are reproducible run to run. Treat it as a black box that
 * costs what a real generation costs. Its outputs are what we observed in production
 * for these questions.
 */
public class StubLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(StubLlmClient.class);

    private static final String FIXTURES = "fixtures/llm-responses.json";

    private final Map<String, JsonNode> fixtures = new HashMap<>();

    public StubLlmClient() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(FIXTURES)) {
            if (in == null) {
                throw new IllegalStateException("missing classpath resource: " + FIXTURES);
            }
            JsonNode root = new ObjectMapper().readTree(in);
            root.fields().forEachRemaining(e -> fixtures.put(normalize(e.getKey()), e.getValue()));
        } catch (Exception e) {
            throw new IllegalStateException("could not load llm fixtures", e);
        }
    }

    @Override
    public Completion generate(String question) {
        JsonNode f = fixtures.get(normalize(question));
        if (f == null) {
            sleep(900);
            log.warn("no fixture for question; returning degenerate completion");
            return new Completion("SELECT 1", 900, "sql-gen-large");
        }
        long latency = jitter(f.path("latencyMs").asLong(900));
        sleep(latency);
        return new Completion(f.path("sql").asText(), latency, "sql-gen-large");
    }

    @Override
    public Completion repair(String question, String badSql, String error) {
        JsonNode f = fixtures.get(normalize(question));
        long latency = jitter(f == null ? 800 : f.path("repairLatencyMs").asLong(800));
        sleep(latency);
        String repaired = (f != null && f.hasNonNull("repairedSql"))
                ? f.get("repairedSql").asText()
                : badSql;
        return new Completion(repaired, latency, "sql-gen-large");
    }

    private long jitter(long base) {
        return Math.max(50, base + ThreadLocalRandom.current().nextLong(-80, 121));
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    private String normalize(String q) {
        return q.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
    }
}

package com.acme.nl2sql.llm;

public interface LlmClient {

    /** Generate SQL for a natural-language question. Blocking. */
    Completion generate(String question);

    /**
     * Ask the model to repair SQL that failed. Blocking.
     * Costs roughly the same as generate().
     */
    Completion repair(String question, String badSql, String error);

    record Completion(String sql, long latencyMs, String model) { }
}

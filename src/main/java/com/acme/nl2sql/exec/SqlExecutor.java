package com.acme.nl2sql.exec;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SqlExecutor {

    private final JdbcTemplate jdbc;

    public SqlExecutor(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> run(String sql) {
        return jdbc.queryForList(sql);
    }
}

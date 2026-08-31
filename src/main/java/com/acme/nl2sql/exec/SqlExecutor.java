package com.acme.nl2sql.exec;

import com.acme.nl2sql.db.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs SQL against the analytics database and materialises the whole result set.
 *
 * <p>No timeout, no row cap, no statement inspection.
 */
public class SqlExecutor {

    private final Database database;

    public SqlExecutor(Database database) {
        this.database = database;
    }

    /**
     * Executes a query and returns every row as a column-name keyed map.
     *
     * @param sql the statement to run
     * @return the rows, in result-set order, with column order preserved per row
     * @throws SQLException if the statement is invalid or execution fails
     */
    public List<Map<String, Object>> run(String sql) throws SQLException {
        try (Connection conn = database.open();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return readAll(rs);
        }
    }

    /**
     * Copies a result set into memory.
     *
     * @param rs an open result set positioned before the first row
     * @return the materialised rows
     * @throws SQLException if reading a value fails
     */
    private List<Map<String, Object>> readAll(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columns = meta.getColumnCount();
        String[] keys = new String[columns];
        for (int i = 1; i <= columns; i++) {
            keys[i - 1] = columnKey(meta, i);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>(columns);
            for (int i = 1; i <= columns; i++) {
                row.put(keys[i - 1], rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * Resolves the key to expose for a column, preferring the alias the query asked for.
     *
     * @param meta   result set metadata
     * @param index  one-based column index
     * @return the label, the underlying column name, or a positional fallback
     * @throws SQLException if the metadata cannot be read
     */
    private String columnKey(ResultSetMetaData meta, int index) throws SQLException {
        String label = meta.getColumnLabel(index);
        if (label != null && !label.isBlank()) {
            return label;
        }
        String name = meta.getColumnName(index);
        return (name != null && !name.isBlank()) ? name : "column_" + index;
    }
}

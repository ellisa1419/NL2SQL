package com.acme.nl2sql.data;

import com.acme.nl2sql.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Random;

/** Builds the analytics database on first run. Deterministic. */
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String[] REGIONS = {"Northeast", "Southeast", "Midwest", "West", "Southwest"};
    private static final String[] SEGMENTS = {"enterprise", "mid-market", "smb"};
    private static final String[] CATEGORIES = {"Hardware", "Software", "Services", "Accessories", "Support"};
    private static final String[] STATUSES = {"completed", "completed", "completed", "completed", "cancelled", "pending"};

    private static final int CUSTOMERS = 900;
    private static final int PRODUCTS = 400;
    private static final int ORDERS = 35000;
    private static final int ORDER_BATCH = 10000;
    private static final int ITEM_BATCH = 20000;

    private final Database database;

    public DataSeeder(Database database) {
        this.database = database;
    }

    /**
     * Creates and populates the analytics tables unless they are already present.
     *
     * @throws IllegalStateException if the database cannot be built
     */
    public void seed() {
        try (Connection conn = database.open()) {
            if (alreadySeeded(conn)) {
                log.info("analytics database present, order_items={}", count(conn, "order_items"));
                return;
            }

            log.info("seeding analytics database (one-off, ~20s)");
            long t0 = System.currentTimeMillis();
            Random rnd = new Random(20260830L);

            exec(conn, "PRAGMA journal_mode=MEMORY");
            exec(conn, "PRAGMA synchronous=OFF");

            conn.setAutoCommit(false);
            createSchema(conn);
            seedRegions(conn);
            seedCustomers(conn, rnd);
            seedProducts(conn, rnd);
            seedOrders(conn, rnd);
            seedOrderItems(conn, rnd);
            createIndexes(conn);
            conn.commit();
            conn.setAutoCommit(true);

            log.info("seed complete in {}ms: {} orders, {} order_items",
                    System.currentTimeMillis() - t0, ORDERS, count(conn, "order_items"));
        } catch (SQLException e) {
            throw new IllegalStateException("could not seed analytics database", e);
        }
    }

    /**
     * @param conn open connection
     * @return true when the schema has already been created
     * @throws SQLException if the catalogue cannot be read
     */
    private boolean alreadySeeded(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='order_items'")) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private void createSchema(Connection conn) throws SQLException {
        exec(conn, "CREATE TABLE regions (id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
        exec(conn, """
            CREATE TABLE customers (id INTEGER PRIMARY KEY, name TEXT NOT NULL,
              region_id INTEGER NOT NULL, segment TEXT NOT NULL, created_at TEXT NOT NULL)""");
        exec(conn, """
            CREATE TABLE products (id INTEGER PRIMARY KEY, name TEXT NOT NULL,
              category TEXT NOT NULL, unit_price REAL NOT NULL)""");
        exec(conn, """
            CREATE TABLE orders (id INTEGER PRIMARY KEY, customer_id INTEGER NOT NULL,
              order_date TEXT NOT NULL, status TEXT NOT NULL)""");
        exec(conn, """
            CREATE TABLE order_items (id INTEGER PRIMARY KEY, order_id INTEGER NOT NULL,
              product_id INTEGER NOT NULL, quantity INTEGER NOT NULL,
              unit_price REAL NOT NULL, discount REAL NOT NULL)""");
    }

    private void seedRegions(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO regions (id, name) VALUES (?,?)")) {
            for (int i = 0; i < REGIONS.length; i++) {
                ps.setInt(1, i + 1);
                ps.setString(2, REGIONS[i]);
                ps.executeUpdate();
            }
        }
    }

    private void seedCustomers(Connection conn, Random rnd) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO customers (id,name,region_id,segment,created_at) VALUES (?,?,?,?,?)")) {
            for (int i = 1; i <= CUSTOMERS; i++) {
                ps.setInt(1, i);
                ps.setString(2, "Customer " + i);
                ps.setInt(3, 1 + rnd.nextInt(5));
                ps.setString(4, SEGMENTS[rnd.nextInt(SEGMENTS.length)]);
                ps.setString(5, LocalDate.of(2022, 1, 1).plusDays(rnd.nextInt(1200)).toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void seedProducts(Connection conn, Random rnd) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO products (id,name,category,unit_price) VALUES (?,?,?,?)")) {
            for (int i = 1; i <= PRODUCTS; i++) {
                ps.setInt(1, i);
                ps.setString(2, "Product " + i);
                ps.setString(3, CATEGORIES[rnd.nextInt(CATEGORIES.length)]);
                ps.setDouble(4, Math.round((20 + rnd.nextDouble() * 900) * 100.0) / 100.0);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void seedOrders(Connection conn, Random rnd) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO orders (id,customer_id,order_date,status) VALUES (?,?,?,?)")) {
            int pending = 0;
            for (int i = 1; i <= ORDERS; i++) {
                ps.setInt(1, i);
                ps.setInt(2, 1 + rnd.nextInt(900));
                ps.setString(3, LocalDate.of(2025, 1, 1).plusDays(rnd.nextInt(365)).toString());
                ps.setString(4, STATUSES[rnd.nextInt(STATUSES.length)]);
                ps.addBatch();
                if (++pending == ORDER_BATCH) {
                    ps.executeBatch();
                    pending = 0;
                }
            }
            if (pending > 0) {
                ps.executeBatch();
            }
        }
    }

    private void seedOrderItems(Connection conn, Random rnd) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO order_items (id,order_id,product_id,quantity,unit_price,discount) VALUES (?,?,?,?,?,?)")) {
            int itemId = 1;
            int pending = 0;
            for (int orderId = 1; orderId <= ORDERS; orderId++) {
                int lines = 1 + rnd.nextInt(3);
                for (int l = 0; l < lines; l++) {
                    int productId = 1 + rnd.nextInt(180);
                    ps.setInt(1, itemId++);
                    ps.setInt(2, orderId);
                    ps.setInt(3, productId);
                    ps.setInt(4, 1 + rnd.nextInt(8));
                    ps.setDouble(5, Math.round((20 + rnd.nextDouble() * 900) * 100.0) / 100.0);
                    ps.setDouble(6, rnd.nextInt(100) < 22 ? Math.round(rnd.nextDouble() * 30) / 100.0 : 0.0);
                    ps.addBatch();
                    pending++;
                }
                if (pending >= ITEM_BATCH) {
                    ps.executeBatch();
                    pending = 0;
                }
            }
            if (pending > 0) {
                ps.executeBatch();
            }
        }
    }

    /**
     * Creates the indexes the analytics queries rely on.
     *
     * <p>{@code order_items.product_id} is intentionally left unindexed.
     *
     * @param conn open connection
     * @throws SQLException if index creation fails
     */
    private void createIndexes(Connection conn) throws SQLException {
        exec(conn, "CREATE INDEX idx_orders_customer ON orders(customer_id)");
        exec(conn, "CREATE INDEX idx_orders_date ON orders(order_date)");
        exec(conn, "CREATE INDEX idx_items_order ON order_items(order_id)");
    }

    /**
     * @param conn  open connection
     * @param table table to count
     * @return the row count, or -1 when it cannot be read
     * @throws SQLException if the count query fails
     */
    private long count(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getLong(1) : -1;
        }
    }

    private void exec(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}

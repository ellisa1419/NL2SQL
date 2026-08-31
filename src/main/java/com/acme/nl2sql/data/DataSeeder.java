package com.acme.nl2sql.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Builds the analytics database on first run. Deterministic. */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String[] REGIONS = {"Northeast", "Southeast", "Midwest", "West", "Southwest"};
    private static final String[] SEGMENTS = {"enterprise", "mid-market", "smb"};
    private static final String[] CATEGORIES = {"Hardware", "Software", "Services", "Accessories", "Support"};
    private static final String[] STATUSES = {"completed", "completed", "completed", "completed", "cancelled", "pending"};

    private final JdbcTemplate jdbc;

    public DataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='order_items'", Integer.class);
        if (existing != null && existing > 0) {
            Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM order_items", Integer.class);
            log.info("analytics database present, order_items={}", rows);
            return;
        }

        log.info("seeding analytics database (one-off, ~20s)");
        long t0 = System.currentTimeMillis();
        Random rnd = new Random(20260830L);

        jdbc.execute("PRAGMA journal_mode=MEMORY");
        jdbc.execute("PRAGMA synchronous=OFF");

        jdbc.execute("CREATE TABLE regions (id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
        jdbc.execute("""
            CREATE TABLE customers (id INTEGER PRIMARY KEY, name TEXT NOT NULL,
              region_id INTEGER NOT NULL, segment TEXT NOT NULL, created_at TEXT NOT NULL)""");
        jdbc.execute("""
            CREATE TABLE products (id INTEGER PRIMARY KEY, name TEXT NOT NULL,
              category TEXT NOT NULL, unit_price REAL NOT NULL)""");
        jdbc.execute("""
            CREATE TABLE orders (id INTEGER PRIMARY KEY, customer_id INTEGER NOT NULL,
              order_date TEXT NOT NULL, status TEXT NOT NULL)""");
        jdbc.execute("""
            CREATE TABLE order_items (id INTEGER PRIMARY KEY, order_id INTEGER NOT NULL,
              product_id INTEGER NOT NULL, quantity INTEGER NOT NULL,
              unit_price REAL NOT NULL, discount REAL NOT NULL)""");

        for (int i = 0; i < REGIONS.length; i++) {
            jdbc.update("INSERT INTO regions (id, name) VALUES (?,?)", i + 1, REGIONS[i]);
        }

        List<Object[]> batch = new ArrayList<>();
        for (int i = 1; i <= 900; i++) {
            batch.add(new Object[]{i, "Customer " + i, 1 + rnd.nextInt(5),
                    SEGMENTS[rnd.nextInt(SEGMENTS.length)],
                    LocalDate.of(2022, 1, 1).plusDays(rnd.nextInt(1200)).toString()});
        }
        jdbc.batchUpdate("INSERT INTO customers (id,name,region_id,segment,created_at) VALUES (?,?,?,?,?)", batch);

        batch = new ArrayList<>();
        for (int i = 1; i <= 400; i++) {
            batch.add(new Object[]{i, "Product " + i, CATEGORIES[rnd.nextInt(CATEGORIES.length)],
                    Math.round((20 + rnd.nextDouble() * 900) * 100.0) / 100.0});
        }
        jdbc.batchUpdate("INSERT INTO products (id,name,category,unit_price) VALUES (?,?,?,?)", batch);

        batch = new ArrayList<>();
        for (int i = 1; i <= 35000; i++) {
            batch.add(new Object[]{i, 1 + rnd.nextInt(900),
                    LocalDate.of(2025, 1, 1).plusDays(rnd.nextInt(365)).toString(),
                    STATUSES[rnd.nextInt(STATUSES.length)]});
            if (batch.size() == 10000) {
                jdbc.batchUpdate("INSERT INTO orders (id,customer_id,order_date,status) VALUES (?,?,?,?)", batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO orders (id,customer_id,order_date,status) VALUES (?,?,?,?)", batch);
        }

        batch = new ArrayList<>();
        int itemId = 1;
        for (int orderId = 1; orderId <= 35000; orderId++) {
            int lines = 1 + rnd.nextInt(3);
            for (int l = 0; l < lines; l++) {
                int productId = 1 + rnd.nextInt(180);
                batch.add(new Object[]{itemId++, orderId, productId, 1 + rnd.nextInt(8),
                        Math.round((20 + rnd.nextDouble() * 900) * 100.0) / 100.0,
                        rnd.nextInt(100) < 22 ? Math.round(rnd.nextDouble() * 30) / 100.0 : 0.0});
            }
            if (batch.size() >= 20000) {
                jdbc.batchUpdate("INSERT INTO order_items (id,order_id,product_id,quantity,unit_price,discount) VALUES (?,?,?,?,?,?)", batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO order_items (id,order_id,product_id,quantity,unit_price,discount) VALUES (?,?,?,?,?,?)", batch);
        }

        jdbc.execute("CREATE INDEX idx_orders_customer ON orders(customer_id)");
        jdbc.execute("CREATE INDEX idx_orders_date ON orders(order_date)");
        jdbc.execute("CREATE INDEX idx_items_order ON order_items(order_id)");
        // NOTE: order_items.product_id is intentionally unindexed.

        Integer items = jdbc.queryForObject("SELECT COUNT(*) FROM order_items", Integer.class);
        log.info("seed complete in {}ms: 35000 orders, {} order_items",
                System.currentTimeMillis() - t0, items);
    }
}

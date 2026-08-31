package com.acme.nl2sql.web;

public final class SchemaDoc {
    private SchemaDoc() { }

    public static final String TEXT = """
        regions(id INTEGER PK, name TEXT)

        customers(id INTEGER PK, name TEXT, region_id INTEGER FK->regions.id,
                  segment TEXT, created_at TEXT)

        products(id INTEGER PK, name TEXT, category TEXT, unit_price REAL)

        orders(id INTEGER PK, customer_id INTEGER FK->customers.id,
               order_date TEXT, status TEXT)

        order_items(id INTEGER PK, order_id INTEGER FK->orders.id,
                    product_id INTEGER FK->products.id,
                    quantity INTEGER, unit_price REAL, discount REAL)

        Indexes: orders(customer_id), orders(order_date), order_items(order_id)

        Notes:
          - orders has no amount column. Order value = SUM(quantity * unit_price * (1 - discount))
            over its order_items.
          - discount is 0.0 when the line was not discounted.
          - order_date is ISO 'YYYY-MM-DD'.
          - status is one of 'completed', 'cancelled', 'pending'.
        """;
}

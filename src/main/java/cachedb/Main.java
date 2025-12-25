package cachedb;

import javax.sql.DataSource;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {

        /* -------------------------------------------
         * Application-owned infrastructure
         * -------------------------------------------
         */

        DataSource ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        CacheDB cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(2)
                .build();

        System.out.println("=== CACHE-DB DEMO START ===");

        /* -------------------------------------------
         * Scenario 1 — Basic lifecycle
         * -------------------------------------------
         */

        System.out.println("\n--- Scenario 1: Basic lifecycle ---");

        cache.set(
                "users",
                Map.of("id", 1),
                Map.of("name", "Alice", "email", "alice@test.com")
        );

        System.out.println("GET users:1 → " +
                cache.get("users", Map.of("id", 1)));

        Thread.sleep(1000);

        cache.set(
                "users",
                Map.of("id", 1),
                Map.of("name", "Alice-v2", "email", "alice@new.com")
        );

        System.out.println("GET users:1 → " +
                cache.get("users", Map.of("id", 1)));

        Thread.sleep(3000);

        System.out.println("GET users:1 after TTL → " +
                cache.get("users", Map.of("id", 1)));

        /* -------------------------------------------
         * Scenario 2 — Multiple tables
         * -------------------------------------------
         */

        System.out.println("\n--- Scenario 2: Multiple tables ---");

        cache.set(
                "orders",
                Map.of("order_id", 100),
                Map.of("status", "PAID", "total", 99.50)
        );

        cache.set(
                "users",
                Map.of("id", 2),
                Map.of("name", "Bob")
        );

        System.out.println("GET orders:100 → " +
                cache.get("orders", Map.of("order_id", 100)));

        Thread.sleep(3000);

        /* -------------------------------------------
         * Scenario 3 — Composite primary key
         * -------------------------------------------
         */

        System.out.println("\n--- Scenario 3: Composite PK ---");

        cache.set(
                "order_items",
                Map.of("order_id", 100, "item_id", 3),
                Map.of("qty", 2, "price", 19.99)
        );

        System.out.println("GET order_items → " +
                cache.get(
                        "order_items",
                        Map.of("order_id", 100, "item_id", 3)
                ));

        Thread.sleep(3000);

        /* -------------------------------------------
         * Scenario 4 — Write-behind burst
         * -------------------------------------------
         */

        System.out.println("\n--- Scenario 4: Write-behind burst ---");

        for (int i = 0; i < 20; i++) {
            cache.set(
                    "users",
                    Map.of("id", 1000 + i),
                    Map.of("name", "User-" + i)
            );
        }

        Thread.sleep(5000);

        System.out.println("\n=== CACHE-DB DEMO END ===");
    }
}


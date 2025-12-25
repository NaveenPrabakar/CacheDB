package cachedb;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheDBMultiTableTest extends CacheDBTestBase {

    @Test
    void supportsMultipleTables() throws Exception {

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

        var order = cache.get("orders", Map.of("order_id", 100));
        assertEquals("PAID", order.get("status"));

        var user = cache.get("users", Map.of("id", 2));
        assertEquals("Bob", user.get("name"));

        Thread.sleep(3000);

        assertNull(cache.get("orders", Map.of("order_id", 100)));
        assertNull(cache.get("users", Map.of("id", 2)));
    }
}

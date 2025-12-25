package cachedb;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheDBLifecycleTest extends CacheDBTestBase {

    @Test
    void basicSetUpdateExpire() throws Exception {

        cache.set(
                "users",
                Map.of("id", 1),
                Map.of("name", "Alice", "email", "alice@test.com")
        );

        var row = cache.get("users", Map.of("id", 1));
        assertEquals("Alice", row.get("name"));

        Thread.sleep(1000);

        cache.set(
                "users",
                Map.of("id", 1),
                Map.of("name", "Alice-v2", "email", "alice@new.com")
        );

        row = cache.get("users", Map.of("id", 1));
        assertEquals("Alice-v2", row.get("name"));

        Thread.sleep(3000);

        assertNull(cache.get("users", Map.of("id", 1)));
    }
}

package cachedb;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheDBReinsertTest extends CacheDBTestBase {

    @Test
    void reinsertAfterExpirationWorks() throws Exception {

        cache.set(
                "users",
                Map.of("id", 8),
                Map.of("name", "Henry")
        );

        Thread.sleep(3000);

        cache.set(
                "users",
                Map.of("id", 8),
                Map.of("name", "Henry-v2")
        );

        var row = cache.get("users", Map.of("id", 8));
        assertEquals("Henry-v2", row.get("name"));

        Thread.sleep(3000);

        assertNull(cache.get("users", Map.of("id", 8)));
    }
}

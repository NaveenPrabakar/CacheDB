package cachedb;

import org.junit.jupiter.api.Test;

import java.util.Map;

class CacheDBIdempotencyTest extends CacheDBTestBase {

    @Test
    void expirationDoesNotDoubleFlush() throws Exception {

        cache.set(
                "users",
                Map.of("id", 5),
                Map.of("name", "Eve")
        );

        Thread.sleep(1000);

        cache.set(
                "users",
                Map.of("id", 5),
                Map.of("name", "Eve-v2")
        );

        Thread.sleep(3000);

        // Pass condition:
        // - no SQL exceptions
        // - no duplicate key violations
        // - no crashes
    }
}

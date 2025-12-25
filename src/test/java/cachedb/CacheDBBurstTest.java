package cachedb;

import org.junit.jupiter.api.Test;

import java.util.Map;

class CacheDBBurstTest extends CacheDBTestBase {

    @Test
    void handlesWriteBehindBurst() throws Exception {

        for (int i = 0; i < 20; i++) {
            cache.set(
                    "users",
                    Map.of("id", 1000 + i),
                    Map.of("name", "User-" + i)
            );
        }

        Thread.sleep(5000);
    }
}

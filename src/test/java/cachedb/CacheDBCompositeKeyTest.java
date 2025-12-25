package cachedb;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheDBCompositeKeyTest extends CacheDBTestBase {

    @Test
    void compositePrimaryKeyWorks() throws Exception {

        cache.set(
                "order_items",
                Map.of("order_id", 100, "item_id", 3),
                Map.of("qty", 2, "price", 19.99)
        );

        var row = cache.get(
                "order_items",
                Map.of("order_id", 100, "item_id", 3)
        );

        assertEquals(2, row.get("qty"));
        assertEquals(19.99, row.get("price"));

        Thread.sleep(3000);

        assertNull(cache.get(
                "order_items",
                Map.of("order_id", 100, "item_id", 3)
        ));
    }
}

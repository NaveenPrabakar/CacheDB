package cachedb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CacheDBCheckpointTest {

    private static final Path WAL_PATH =
            Path.of("logs", "wal.log");

    private DataSource ds;

    @BeforeEach
    void setup() throws Exception {
        ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        Files.createDirectories(WAL_PATH.getParent());
        Files.deleteIfExists(WAL_PATH);
    }

    @Test
    void checkpointDoesNotTruncateWhileMutationIsOutstanding() throws Exception {

        CacheDB db = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10) // long TTL: won't have been flushed by the time we checkpoint
                .build();

        db.set(
                "users",
                Map.of("id", 1),
                Map.of("name", "Alice")
        );

        // WAL should exist and be non-empty
        assertTrue(Files.size(WAL_PATH) > 0);

        // The write hasn't been confirmed durable in MySQL yet, so an explicit
        // checkpoint must NOT discard it (this is the core C1 fix).
        db.checkpoint();

        assertTrue(Files.size(WAL_PATH) > 0);
    }

    @Test
    void checkpointTruncatesOnceMutationIsFlushed() throws Exception {

        CacheDB db = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(1)
                .build();

        db.set(
                "users",
                Map.of("id", 2),
                Map.of("name", "Bob")
        );

        // Let the TTL elapse so ExpirationManager + FlushManager confirm the write.
        Thread.sleep(2000);

        db.checkpoint();

        // Now that the mutation is confirmed durable, the WAL should be empty.
        assertEquals(0, Files.size(WAL_PATH));
    }

    @Test
    void checkpointDoesNotDiscardOtherPendingEntriesAfterOneFlushes() throws Exception {
        // Regression test for C1: checkpoint() previously truncated the WHOLE file
        // after ANY single successful flush, wiping out other still-outstanding
        // records. This reproduces that scenario directly.

        CacheDB db = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(1)
                .build();

        // Entry A: short-lived — will expire, flush, and self-checkpoint during the sleep.
        db.set("users", Map.of("id", 10), Map.of("name", "FlushedSoon"));
        Thread.sleep(2000);

        // Entry B: written AFTER A already flushed, with a long TTL so it stays
        // outstanding (never reaches MySQL) for the rest of this test.
        db.set("users", Map.of("id", 11), Map.of("name", "StillPending"));

        assertTrue(Files.size(WAL_PATH) > 0);

        // A checkpoint here must not truncate the WAL out from under entry B just
        // because entry A already confirmed.
        db.checkpoint();

        assertTrue(Files.size(WAL_PATH) > 0);
    }

    @Test
    void recoveryDoesNotReplayAfterCheckpoint() throws Exception {

        CacheDB db1 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(1)
                .build();

        db1.set(
                "users",
                Map.of("id", 2),
                Map.of("name", "Bob")
        );

        // Let the write actually flush to MySQL before checkpointing.
        Thread.sleep(2000);
        db1.checkpoint();

        db1 = null; // crash

        CacheDB db2 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        // If WAL was cleared, recovery should NOT reinsert
        Map<String, Object> result =
                db2.get("users", Map.of("id", 2));

        assertNull(result);
    }

    @Test
    void recoveryStillWorksWithoutCheckpoint() throws Exception {

        CacheDB db1 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        db1.set(
                "users",
                Map.of("id", 3),
                Map.of("name", "Carol")
        );

        db1 = null; // crash before checkpoint

        CacheDB db2 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        Map<String, Object> result =
                db2.get("users", Map.of("id", 3));

        assertNotNull(result);
        assertEquals("Carol", result.get("name"));
    }

    @Test
    void checkpointIsIdempotent() throws Exception {

        CacheDB db = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(1)
                .build();

        db.set(
                "users",
                Map.of("id", 4),
                Map.of("name", "Dave")
        );

        Thread.sleep(2000);

        db.checkpoint();
        db.checkpoint(); // second call should not fail

        assertEquals(0, Files.size(WAL_PATH));
    }
}
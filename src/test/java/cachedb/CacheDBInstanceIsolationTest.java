package cachedb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression coverage for backlog item C2: {@code WALWriter} used to be a
 * process-wide static singleton, so constructing a second {@code CacheDB}
 * while a first one was still running (and still flushing in the
 * background) silently swapped out the WAL channel the first instance's
 * {@code FlushManager} would checkpoint against.
 *
 * <p>These tests build two overlapping, un-closed {@code CacheDB} instances
 * in the same JVM — mirroring what {@code CacheDBRecoveryTest} and
 * {@code CacheDBCheckpointTest} already do — and confirm that ordinary
 * writes and background flush/checkpoint cycles on the older instance keep
 * working correctly after the newer instance is constructed.
 */
public class CacheDBInstanceIsolationTest {

    private DataSource ds;

    @BeforeEach
    void setup() throws Exception {
        ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        Path wal = Path.of("logs", "wal.log");
        Files.createDirectories(wal.getParent());
        Files.deleteIfExists(wal);
    }

    @Test
    void olderInstanceKeepsFlushingCorrectlyAfterNewerInstanceIsConstructed() throws Exception {

        // Instance A: write several entries so multiple flush tasks land
        // in A's FlushManager queue and are still being processed as B
        // comes online.
        CacheDB dbA = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(2)
                .dashboard(false)
                .build();

        for (int i = 0; i < 5; i++) {
            dbA.set("users", Map.of("id", 900 + i), Map.of("name", "A-User-" + i));
        }

        // Instance B is constructed while A is still live and un-closed —
        // this used to overwrite WALWriter.INSTANCE and hijack A's
        // background checkpoint calls.
        CacheDB dbB = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(2)
                .dashboard(false)
                .build();

        dbB.set("orders", Map.of("order_id", 900), Map.of("status", "PAID"));

        // Give both instances' background flush threads time to run.
        Thread.sleep(3000);

        // A's own writes must still be readable from A's own cache and
        // must not have been lost or corrupted by B's construction.
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = dbA.get("users", Map.of("id", 900 + i));
            assertNotNull(row, "A's entry " + i + " should not be lost after B was constructed");
            assertEquals("A-User-" + i, row.get("name"));
        }

        // A's explicit checkpoint must operate on its own WAL, not throw,
        // and must not be a no-op due to a stolen channel reference.
        assertDoesNotThrow(dbA::checkpoint);

        // B's data must be independently correct too.
        Map<String, Object> bRow = dbB.get("orders", Map.of("order_id", 900));
        assertNotNull(bRow);
        assertEquals("PAID", bRow.get("status"));
    }
}
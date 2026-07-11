package cachedb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WalCheckpointCoordinatorTest {

    private Path walPath;
    private WALWriter wal;
    private WalCheckpointCoordinator coordinator;

    @BeforeEach
    void setUp() throws Exception {
        walPath = Files.createTempFile("wal-coordinator-test", ".log");
        Files.deleteIfExists(walPath);
        wal = new WALWriter(walPath);
        coordinator = new WalCheckpointCoordinator(wal);
    }

    @AfterEach
    void tearDown() throws Exception {
        wal.close();
        Files.deleteIfExists(walPath);
    }

    @Test
    void doesNotTruncateWhileAnyKeyIsOutstanding() throws Exception {
        long v1 = coordinator.appendAndTrack(LogRecord.put(bytes("a"), bytes("1")), "a");
        coordinator.appendAndTrack(LogRecord.put(bytes("b"), bytes("2")), "b");

        // Confirm only "a" — "b" is still outstanding. This is the exact C1 bug
        // scenario: one record flushes while another is still pending.
        coordinator.recordFlushed("a", v1);

        assertTrue(Files.size(walPath) > 0, "WAL must be preserved while 'b' is still unflushed");
        assertEquals(1, coordinator.outstandingKeyCount());
    }

    @Test
    void truncatesOnceEveryKeyIsConfirmed() throws Exception {
        long v1 = coordinator.appendAndTrack(LogRecord.put(bytes("a"), bytes("1")), "a");
        long v2 = coordinator.appendAndTrack(LogRecord.put(bytes("b"), bytes("2")), "b");

        coordinator.recordFlushed("a", v1);
        coordinator.recordFlushed("b", v2);

        assertEquals(0, Files.size(walPath));
        assertEquals(0, coordinator.outstandingKeyCount());
    }

    @Test
    void aNewerWriteAfterAFlushIsInitiatedStaysOutstanding() throws Exception {
        long v1 = coordinator.appendAndTrack(LogRecord.put(bytes("a"), bytes("1")), "a");

        // A newer write for the same key arrives before the original flush confirms
        // (write coalescing — common when a key is set multiple times before its TTL).
        long v2 = coordinator.appendAndTrack(LogRecord.put(bytes("a"), bytes("2")), "a");

        // The stale flush (for v1) confirms — must NOT clear the key, since v2 is newer
        // and hasn't been confirmed yet.
        coordinator.recordFlushed("a", v1);
        assertEquals(1, coordinator.outstandingKeyCount());
        assertTrue(Files.size(walPath) > 0);

        // The up-to-date flush confirms — now it's safe to truncate.
        coordinator.recordFlushed("a", v2);
        assertEquals(0, coordinator.outstandingKeyCount());
        assertEquals(0, Files.size(walPath));
    }

    @Test
    void explicitCheckpointIsSafeWhenNothingIsOutstanding() throws IOException {
        assertDoesNotThrow(() -> coordinator.checkpointIfSafe());
        assertEquals(0, Files.size(walPath));
    }

    @Test
    void explicitCheckpointLeavesOutstandingRecordsInPlace() throws Exception {
        coordinator.appendAndTrack(LogRecord.put(bytes("a"), bytes("1")), "a");

        coordinator.checkpointIfSafe();

        assertTrue(Files.size(walPath) > 0);
        assertEquals(1, coordinator.outstandingKeyCount());
    }

    private static byte[] bytes(String s) {
        return s.getBytes();
    }
}
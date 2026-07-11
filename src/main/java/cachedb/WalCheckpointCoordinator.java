package cachedb;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Guards WAL truncation against premature data loss (defect C1).
 *
 * <p>The WAL is a single append-only file shared by every key. Truncating it is only
 * safe once every record currently on disk is known to be redundant — i.e. every key
 * with an unconfirmed write in the WAL has since been confirmed durable in the backing
 * store. This coordinator tracks that per-key durability state and performs the
 * sync/truncate exactly when it becomes safe to do so.
 *
 * <p>All WAL-affecting operations (appending a new record, replaying an existing one
 * during recovery, confirming a flush, or an explicit caller-triggered checkpoint) are
 * synchronized on this instance so a truncate can never race a not-yet-tracked append —
 * see {@link #appendAndTrack} for why that ordering matters.
 */
public class WalCheckpointCoordinator {

    private final WALWriter wal;
    private final AtomicLong sequence = new AtomicLong(0);
    private final Map<String, Long> pendingVersions = new ConcurrentHashMap<>();

    public WalCheckpointCoordinator(WALWriter wal) {
        this.wal = wal;
    }

    /**
     * Writes {@code record} to the WAL and marks {@code keyId} as having an outstanding
     * (unconfirmed) mutation, atomically with respect to any concurrent checkpoint.
     * This ordering is what prevents a checkpoint triggered by an unrelated key's flush
     * from truncating a record that was just written but not yet visible to this
     * coordinator's bookkeeping.
     *
     * @return the sequence number assigned to this mutation, to be echoed back via
     *         {@link #recordFlushed} once it is confirmed durable in the backing store.
     */
    public synchronized long appendAndTrack(LogRecord record, String keyId) throws IOException {
        wal.append(record);
        return track(keyId);
    }

    /**
     * Marks a WAL record that is already durably on disk (e.g. discovered during crash
     * recovery) as outstanding, without writing it again.
     *
     * @return the sequence number assigned to this mutation.
     */
    public synchronized long trackExisting(String keyId) {
        return track(keyId);
    }

    private long track(String keyId) {
        long version = sequence.incrementAndGet();
        pendingVersions.put(keyId, version);
        return version;
    }

    /**
     * Confirms that {@code keyId} has been durably persisted to the backing store as of
     * {@code version}. If a newer mutation for the same key was appended after this
     * flush was initiated, the key remains outstanding — a future flush of that newer
     * version will confirm it instead. Once no key has an outstanding mutation, every
     * record currently in the WAL is redundant, so it is checkpointed.
     */
    public synchronized void recordFlushed(String keyId, long version) {
        pendingVersions.computeIfPresent(keyId,
                (k, pendingVersion) -> pendingVersion <= version ? null : pendingVersion);
        checkpointIfSafe();
    }

    /**
     * Syncs the WAL and truncates it if, and only if, every mutation currently tracked
     * has been confirmed flushed. Always safe to call.
     */
    public synchronized void checkpointIfSafe() {
        try {
            wal.sync();
        } catch (Exception e) {
            // Best-effort: a failed sync just means the next append/checkpoint will retry.
        }
        if (pendingVersions.isEmpty()) {
            try {
                wal.truncate();
            } catch (Exception e) {
                // Truncation failing is non-fatal: the backing store already has the
                // durable copy of every record in the WAL, and replaying them again
                // after a crash is a safe no-op (upserts/deletes are idempotent).
            }
        }
    }

    /** Number of keys with an unconfirmed mutation currently in the WAL. Visible for tests/monitoring. */
    public int outstandingKeyCount() {
        return pendingVersions.size();
    }
}
package cachedb;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class CacheDB {

    private static final Path WAL_PATH =
            Path.of("logs", "wal.log");

    private final CacheStore store;
    private final ExpirationManager expirationManager;
    private final WALWriter wal;
    private final WalCheckpointCoordinator checkpointCoordinator;
    private Dashboard dashboard;

    private CacheDB(CacheStore store,
                    ExpirationManager expirationManager,
                    WALWriter wal,
                    WalCheckpointCoordinator checkpointCoordinator,
                    Dashboard dashboard) throws IOException {

        this.store = store;
        this.expirationManager = expirationManager;
        this.wal = wal;
        this.checkpointCoordinator = checkpointCoordinator;
        this.dashboard = dashboard;

        recover();
    }

    private void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
        if (dashboard != null) {
            try {
                dashboard.start();
            } catch (IOException e) {
                System.err.println("Warning: Failed to start dashboard: " + e.getMessage());
            }
        }
    }

    /**
     * Requests an explicit checkpoint. This is a best-effort operation: the WAL is only
     * truncated if every mutation currently represented in it has already been
     * confirmed durable in the backing store (see {@link WalCheckpointCoordinator}).
     * Always safe to call.
     */
    public void checkpoint() {
        checkpointCoordinator.checkpointIfSafe();
    }

    private void recover() throws IOException {
        if (!Files.exists(WAL_PATH)) return;

        try (WALReader reader = new WALReader(WAL_PATH)) {
            for (LogRecord r : reader) {
                String key = new String(r.key());
                // key format: table|{pk} — identical to CacheKeys.identity(table, pk)
                // for the ORIGINAL (pre-recovery) primary key, since that's exactly how
                // it was serialized when the record was first appended.
                String[] parts = key.split("\\|", 2);
                String table = parts[0];
                Map<String, Object> pk = SimpleCodec.parseMap(parts[1]);

                if (r.type() == LogType.PUT) {
                    String value = new String(r.value());
                    Map<String, Object> cols = SimpleCodec.parseMap(value);
                    long version = checkpointCoordinator.trackExisting(key);
                    store.upsert(table, pk, cols, version);
                } else if (r.type() == LogType.DELETE) {
                    long version = checkpointCoordinator.trackExisting(key);
                    boolean resident = store.delete(table, pk, version);
                    if (!resident) {
                        // No entry was replayed for this key before the delete (e.g. its
                        // PUT record was already checkpointed away pre-crash). Nothing
                        // will ever expire/flush it, so acknowledge it immediately rather
                        // than letting it block the WAL from ever being checkpointed
                        // again — same reasoning as delete() below, tied to defect C4.
                        checkpointCoordinator.recordFlushed(key, version);
                    }
                }
            }
        }
    }

    public void set(String table,
                    Map<String, Object> primaryKey,
                    Map<String, Object> columns) {

        Objects.requireNonNull(table);
        Objects.requireNonNull(primaryKey);
        Objects.requireNonNull(columns);

        String keyId = CacheKeys.identity(table, primaryKey);
        byte[] walKey = keyId.getBytes();
        byte[] walValue = columns.toString().getBytes();

        long version;
        try {
            version = checkpointCoordinator.appendAndTrack(LogRecord.put(walKey, walValue), keyId);
        } catch (IOException e) {
            throw new RuntimeException("WAL write failed", e);
        }

        store.upsert(table, primaryKey, columns, version);

        // Track write operation
        if (dashboard != null) {
            dashboard.recordWrite();
        }
    }

    public Map<String, Object> get(String table,
                                   Map<String, Object> primaryKey) {
        Map<String, Object> result = store.get(table, primaryKey);

        // Track read operation
        if (dashboard != null) {
            if (result != null) {
                dashboard.recordRead();
            } else {
                dashboard.recordMiss();
            }
        }

        return result;
    }

    public void delete(String table,
                      Map<String, Object> primaryKey) {

        Objects.requireNonNull(table);
        Objects.requireNonNull(primaryKey);

        String keyId = CacheKeys.identity(table, primaryKey);
        byte[] walKey = keyId.getBytes();

        long version;
        try {
            version = checkpointCoordinator.appendAndTrack(LogRecord.delete(walKey), keyId);
        } catch (IOException e) {
            throw new RuntimeException("WAL write failed", e);
        }

        boolean resident = store.delete(table, primaryKey, version);
        if (!resident) {
            // Nothing in cache to expire/flush for this key (defect C4: a delete of a
            // non-resident key never reaches the database today). No future flush will
            // ever confirm this WAL record, so acknowledge it immediately rather than
            // letting it block the WAL from being checkpointed again.
            checkpointCoordinator.recordFlushed(keyId, version);
        }

        // Track delete operation
        if (dashboard != null) {
            dashboard.recordDelete();
        }
    }

    /* ------------ BUILDER ------------ */

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private DataSource dataSource;
        private long ttlMillis = 2000;
        private boolean dashboardEnabled = true;
        private int dashboardPort = 8080;

        public Builder dataSource(DataSource ds) {
            this.dataSource = ds;
            return this;
        }

        public Builder ttlSeconds(long seconds) {
            this.ttlMillis = seconds * 1000;
            return this;
        }

        public Builder dashboard(boolean enabled) {
            this.dashboardEnabled = enabled;
            return this;
        }

        public Builder dashboardPort(int port) {
            this.dashboardPort = port;
            return this;
        }

        public CacheDB build() throws IOException {
            Objects.requireNonNull(dataSource);

            SchemaRegistry schemaRegistry =
                    new SchemaRegistry(dataSource);

            CacheStore store = new CacheStore(ttlMillis);

            Files.createDirectories(WAL_PATH.getParent());
            WALWriter wal = new WALWriter(WAL_PATH);
            WalCheckpointCoordinator checkpointCoordinator = new WalCheckpointCoordinator(wal);

            FlushManager flushManager =
                    new FlushManager(dataSource, schemaRegistry, checkpointCoordinator);

            ExpirationManager expirationManager =
                    new ExpirationManager(store, flushManager);

            new Thread(flushManager, "flush-thread").start();
            new Thread(expirationManager, "expiration-thread").start();

            CacheDB cacheDB = new CacheDB(store, expirationManager, wal, checkpointCoordinator, null);

            if (dashboardEnabled) {
                Dashboard dashboard = new Dashboard(cacheDB, store, dashboardPort);
                cacheDB.setDashboard(dashboard);
            }

            return cacheDB;
        }


    }
}
package cachedb;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;

public final class CacheDB {

    private final CacheStore store;
    private final ExpirationManager expirationManager;

    private CacheDB(CacheStore store, ExpirationManager expirationManager) {
        this.store = store;
        this.expirationManager = expirationManager;
    }

    public void set(String table,
                    Map<String, Object> primaryKey,
                    Map<String, Object> columns) {

        Objects.requireNonNull(table);
        Objects.requireNonNull(primaryKey);
        Objects.requireNonNull(columns);

        store.upsert(table, primaryKey, columns);
    }

    public Map<String, Object> get(String table, Map<String, Object> primaryKey) {
        return store.get(table, primaryKey);
    }

    /* ---------------- BUILDER ---------------- */

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private DataSource dataSource;
        private long ttlMillis = 2000;

        public Builder dataSource(DataSource ds) {
            this.dataSource = ds;
            return this;
        }

        public Builder ttlSeconds(long seconds) {
            this.ttlMillis = seconds * 1000;
            return this;
        }

        public CacheDB build() {
            Objects.requireNonNull(dataSource, "DataSource required");

            SchemaRegistry schemaRegistry = new SchemaRegistry(dataSource);
            CacheStore store = new CacheStore(ttlMillis);
            FlushManager flushManager =
                    new FlushManager(dataSource, schemaRegistry);
            ExpirationManager expirationManager =
                    new ExpirationManager(store, flushManager);

            new Thread(flushManager, "flush-thread").start();
            new Thread(expirationManager, "expiration-thread").start();

            return new CacheDB(store, expirationManager);
        }
    }
}

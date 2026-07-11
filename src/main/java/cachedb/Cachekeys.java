package cachedb;

import java.util.Map;

/**
 * Derives a stable string identity for a (table, primary key) pair. Used both as the
 * WAL record's key bytes and as the lookup key for WAL checkpoint tracking.
 *
 * Centralizing this in one place avoids the write path and the durability-tracking
 * path silently drifting out of sync with each other (e.g. because one of them
 * changed how it stringifies a Map).
 */
final class CacheKeys {

    private CacheKeys() {}

    static String identity(String table, Map<String, Object> primaryKey) {
        return table + "|" + primaryKey;
    }
}
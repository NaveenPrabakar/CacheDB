package cachedb;

import java.util.Map;

public class CacheEntry {

    public final Map<String, Object> primaryKey;
    public Map<String, Object> columns;

    public long expiresAt;
    public long version;
    public boolean dirty;

    public CacheEntry(Map<String, Object> pk,
                      Map<String, Object> columns,
                      long expiresAt) {
        this.primaryKey = pk;
        this.columns = columns;
        this.expiresAt = expiresAt;
        this.version = 1;
        this.dirty = true;
    }
}

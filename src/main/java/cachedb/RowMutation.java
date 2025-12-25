package cachedb;

import java.util.Map;

public class RowMutation {

    public final String table;
    public final Map<String, Object> primaryKey;
    public final Map<String, Object> columns;
    public final long version;

    public RowMutation(String table,
                       Map<String, Object> pk,
                       Map<String, Object> columns,
                       long version) {
        this.table = table;
        this.primaryKey = pk;
        this.columns = columns;
        this.version = version;
    }
}

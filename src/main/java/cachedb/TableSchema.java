package cachedb;

import java.util.List;
import java.util.Map;

public class TableSchema {

    public final List<String> primaryKeys;
    public final Map<String, Integer> columns;

    public TableSchema(List<String> pks,
                       Map<String, Integer> columns) {
        this.primaryKeys = pks;
        this.columns = columns;
    }
}

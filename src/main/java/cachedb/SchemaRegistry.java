package cachedb;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SchemaRegistry {

    private final DataSource ds;
    private final Map<String, TableSchema> cache = new ConcurrentHashMap<>();

    public SchemaRegistry(DataSource ds) {
        this.ds = ds;
    }

    public TableSchema get(String table) {
        return cache.computeIfAbsent(table, this::load);
    }

    private TableSchema load(String table) {
        try (Connection c = ds.getConnection()) {
            DatabaseMetaData meta = c.getMetaData();

            List<String> pks = new ArrayList<>();
            ResultSet pkRs = meta.getPrimaryKeys(null, null, table);
            while (pkRs.next()) {
                pks.add(pkRs.getString("COLUMN_NAME"));
            }

            Map<String, Integer> cols = new HashMap<>();
            ResultSet colRs = meta.getColumns(null, null, table, null);
            while (colRs.next()) {
                cols.put(colRs.getString("COLUMN_NAME"),
                        colRs.getInt("DATA_TYPE"));
            }

            return new TableSchema(pks, cols);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

package cachedb;

import java.sql.*;
import java.util.*;

public class TableMeta {

    public final List<String> pkColumns;
    public final List<String> nonPkColumns;

    private TableMeta(List<String> pkColumns, List<String> nonPkColumns) {
        this.pkColumns = pkColumns;
        this.nonPkColumns = nonPkColumns;
    }

    public static TableMeta load(Connection conn, String table) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();

        List<String> pkCols = new ArrayList<>();
        try (ResultSet rs = meta.getPrimaryKeys(null, null, table)) {
            while (rs.next()) {
                pkCols.add(rs.getString("COLUMN_NAME"));
            }
        }

        if (pkCols.isEmpty()) {
            throw new SQLException("Table " + table + " has no primary key");
        }

        Set<String> allCols = new LinkedHashSet<>();
        try (ResultSet rs = meta.getColumns(null, null, table, null)) {
            while (rs.next()) {
                allCols.add(rs.getString("COLUMN_NAME"));
            }
        }

        List<String> nonPk = new ArrayList<>();
        for (String c : allCols) {
            if (!pkCols.contains(c)) {
                nonPk.add(c);
            }
        }

        return new TableMeta(pkCols, nonPk);
    }
}

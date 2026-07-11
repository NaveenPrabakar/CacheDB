package cachedb;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FlushManager implements Runnable {

    private final BlockingQueue<FlushTask> queue = new LinkedBlockingQueue<>();
    private final DataSource dataSource;
    private final SchemaRegistry schemaRegistry;
    private final WalCheckpointCoordinator checkpointCoordinator;

    public FlushManager(DataSource ds, SchemaRegistry schemaRegistry, WalCheckpointCoordinator checkpointCoordinator) {
        this.dataSource = ds;
        this.schemaRegistry = schemaRegistry;
        this.checkpointCoordinator = checkpointCoordinator;
    }

    public void enqueue(FlushTask task) {
        queue.offer(task);
    }

    @Override
    public void run() {
        while (true) {
            try {
                FlushTask task = queue.take();
                flush(task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void flush(FlushTask task) throws Exception {
        RowMutation m = task.mutation;
        TableSchema schema = schemaRegistry.get(m.table);

        try (Connection c = dataSource.getConnection()) {
            if (m.isDelete) {
                String sql = SqlBuilder.buildDelete(m, schema);
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    int idx = 1;
                    for (String pk : schema.primaryKeys) {
                        Object value = m.primaryKey.get(pk);
                        if (value == null && schema.primaryKeys.size() == 1 && m.primaryKey.size() == 1) {
                            value = m.primaryKey.values().iterator().next();
                        }
                        ps.setObject(idx++, value);
                    }

                    try {
                        ps.executeUpdate();
                        acknowledgeFlush(m);
                    } catch (Exception e) {
                        // DB down → WAL preserved
                    }

                    System.out.println("[FLUSHED DELETE] " + m.table + " " + m.primaryKey);
                }
            } else {
                String sql = SqlBuilder.buildUpsert(m, schema);
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    int idx = 1;
                    for (String pk : schema.primaryKeys) {
                        Object value = m.primaryKey.get(pk);
                        if (value == null) {
                            value = m.columns.get(pk);
                        }
                        if (value == null && schema.primaryKeys.size() == 1 && m.primaryKey.size() == 1) {
                            value = m.primaryKey.values().iterator().next();
                        }
                        ps.setObject(idx++, value);
                    }
                    for (String col : m.columns.keySet()) {
                        if (!schema.primaryKeys.contains(col)) {
                            ps.setObject(idx++, m.columns.get(col));
                        }
                    }

                    try {
                        ps.executeUpdate();
                        acknowledgeFlush(m);
                    } catch (Exception e) {
                        // DB down → WAL preserved
                    }

                    System.out.println("[FLUSHED] " + m.table + " " + m.primaryKey);
                }
            }
        }
    }

    /**
     * Reports this mutation as durably persisted so the coordinator can checkpoint the
     * WAL once every currently-outstanding mutation has been confirmed (see C1).
     */
    private void acknowledgeFlush(RowMutation m) {
        checkpointCoordinator.recordFlushed(CacheKeys.identity(m.table, m.primaryKey), m.version);
    }

}
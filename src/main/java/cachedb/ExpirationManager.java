package cachedb;

public class ExpirationManager implements Runnable {

    private final CacheStore store;
    private final FlushManager flushManager;

    public ExpirationManager(CacheStore store,
                             FlushManager flushManager) {
        this.store = store;
        this.flushManager = flushManager;
    }

    @Override
    public void run() {
        while (true) {
            long now = System.currentTimeMillis();

            store.raw().forEach((table, map) -> {
                map.entrySet().removeIf(e -> {
                    CacheEntry entry = e.getValue();
                    if (entry.expiresAt <= now) {
                        if (entry.dirty) {
                            flushManager.enqueue(
                                    new FlushTask(
                                            new RowMutation(
                                                    table,
                                                    entry.primaryKey,
                                                    entry.columns,
                                                    entry.version
                                            )
                                    )
                            );
                        }
                        return true;
                    }
                    return false;
                });
            });

            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }
    }
}

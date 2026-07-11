package cachedb;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static cachedb.LogSerializer.serialize;

/**
 * Instance-scoped writer for a single CacheDB's write-ahead log file.
 *
 * <p>Each {@code CacheDB} owns exactly one {@code WALWriter}, created once
 * when the instance is built and passed explicitly to every collaborator
 * that needs to append to or truncate the log (e.g. {@link FlushManager}).
 * There is intentionally no process-wide singleton here: a JVM may host
 * multiple independent {@code CacheDB} instances (as several tests already
 * do), each pointed at its own WAL file, and a shared singleton would let
 * one instance's background flush thread truncate or sync a completely
 * different instance's WAL — silently corrupting it.
 */
public class WALWriter implements Closeable {

    private final FileChannel channel;

    public WALWriter(Path path) throws IOException {
        channel = FileChannel.open(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
        );
    }

    public synchronized void append(LogRecord record) throws IOException {
        ByteBuffer buffer = serialize(record);
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
        channel.force(true);
    }

    public synchronized void sync() throws IOException {
        channel.force(true);
    }

    public synchronized void truncate() throws IOException {
        channel.truncate(0);
        channel.position(0);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
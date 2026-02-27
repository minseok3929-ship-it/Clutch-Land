package com.clutch.land.infrastructure;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class AsyncDatabaseWriter {
    private static final DbTask POISON = () -> { };

    private final Logger logger;
    private final Database database;
    private final BlockingQueue<DbTask> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread worker;

    public AsyncDatabaseWriter(JavaPlugin plugin, Database database) {
        this.logger = Objects.requireNonNull(plugin, "plugin").getLogger();
        this.database = Objects.requireNonNull(database, "database");
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        worker = new Thread(this::runLoop, "clutchland-db-writer");
        worker.start();
    }

    public void submit(DbTask task) {
        if (!running.get()) {
            throw new IllegalStateException("Async writer is not running");
        }
        queue.offer(task);
    }

    public void shutdownAndFlush() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        queue.offer(POISON);

        if (worker != null) {
            try {
                worker.join(TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("Interrupted while waiting DB writer to stop.");
            }
        }
    }

    private void runLoop() {
        while (true) {
            try {
                DbTask task = queue.take();
                if (task == POISON) {
                    drainPendingTasks();
                    break;
                }
                task.execute();
            } catch (Exception e) {
                logger.severe("Async DB task failed: " + e.getMessage());
            }
        }
    }

    private void drainPendingTasks() {
        DbTask task;
        while ((task = queue.poll()) != null) {
            if (task == POISON) {
                continue;
            }
            try {
                task.execute();
            } catch (Exception e) {
                logger.severe("Async DB drain task failed: " + e.getMessage());
            }
        }
    }

    @FunctionalInterface
    public interface DbTask {
        void execute() throws Exception;
    }
}

package sapphire.policy.scalability;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author terryz
 */
public class AsyncCommitter implements Closeable {
    private final static Logger logger = Logger.getLogger(AsyncCommitter.class.getName());
    private final ScheduledExecutorService commitExecutor;

    public AsyncCommitter(ILogger<LogEntry> entryLogger) {
        this.commitExecutor = Executors.newSingleThreadScheduledExecutor();
        this.commitExecutor.schedule(new ApplyCommit(entryLogger),
                1000, TimeUnit.SECONDS);
    }

    @Override
    public void close() throws IOException {
        if (commitExecutor != null) {
            commitExecutor.shutdown();
        }
    }

    private static class ApplyCommit implements Runnable {
        final ILogger<LogEntry> entryLogger;

        public ApplyCommit(ILogger<LogEntry> entryLogger) {
            this.entryLogger = entryLogger;
        }

        @Override
        public void run() {
            List<LogEntry> uncommittedEntries = entryLogger.getUncomittedEntries();

            for (LogEntry en : uncommittedEntries) {
                // TODO (Terry): implement apply commit
                entryLogger.markCommitted(en);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}

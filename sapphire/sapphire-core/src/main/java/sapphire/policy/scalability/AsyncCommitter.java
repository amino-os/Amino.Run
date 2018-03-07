package sapphire.policy.scalability;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import sapphire.common.AppObject;

/**
 * @author terryz
 */
// TODO (Terry): Delete AsyncCommitter
public class AsyncCommitter implements Closeable {
    private final static Logger logger = Logger.getLogger(AsyncCommitter.class.getName());
    private final ScheduledExecutorService scheduler;
    private final CommitExecutor commitExecutor;

    public AsyncCommitter(CommitExecutor commitExecutor, ILogger<LogEntry> entryLogger, AppObject appObject) {
        this.commitExecutor = commitExecutor;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.schedule(new AsyncCommitter.ApplyCommit(commitExecutor, entryLogger, appObject),
                100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws IOException {
        if (commitExecutor != null) {
            commitExecutor.close();
        }
    }

    private static class ApplyCommit implements Runnable {
        final ILogger<LogEntry> entryLogger;
        final CommitExecutor commitExecutor;
        final AppObject appObject;

        public ApplyCommit(CommitExecutor commitExecutor, ILogger<LogEntry> entryLogger, AppObject appObject) {
            this.commitExecutor = commitExecutor;
            this.entryLogger = entryLogger;
            this.appObject = appObject;
        }

        @Override
        public void run() {
            List<LogEntry> uncommittedEntries = entryLogger.getUncomittedEntries();
            for (LogEntry en : uncommittedEntries) {
                commitExecutor.applyWriteAsync(en.getRequest(), en.getIndex());
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}

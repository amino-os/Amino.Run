package sapphire.policy.scalability;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author terryz
 */
public class FileLogger implements ILogger<LogEntry> {

    private static Logger logger = Logger.getLogger(FileLogger.class.getName());

    /**
     * Index of the largest committed log entry. A log entry is
     * committed iff it has been replicated to slave and its
     * request has been invoked.
     */
    private long indexOfCommittedEntry;

    private ObjectOutputStream oos;

    // TODO (Terry): truncate the list and the log file periodically
    private List<LogEntry> logEntries = new ArrayList<LogEntry>();

    public FileLogger(String logFilePath) throws Exception {
        this.oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(logFilePath))));
    }

    /**
     * @return the index of the largest log entry, or <code>-1</code> if no
     * log entry exists.
     */
    public long getLargestLogEntryIndex() {
        return logEntries.size() - 1;
    }

    @Override
    public void setIndexOfCommittedEntry(long indexOfCommittedEntry) {
        if (indexOfCommittedEntry < 0) {
            throw new IllegalArgumentException(String.format("invalid negative committed entry index(%s)", indexOfCommittedEntry));
        }

        long largestIndex = getLargestLogEntryIndex();
        if (indexOfCommittedEntry > largestIndex) {
            throw new IllegalArgumentException(String.format("committed index(%s) greater than the current largest index(%s)", indexOfCommittedEntry, largestIndex));
        }

        this.indexOfCommittedEntry = Math.max(this.indexOfCommittedEntry, indexOfCommittedEntry);
    }

    @Override
    public long append(LogEntry entry) {
        try {
            oos.writeObject(entry);
            logEntries.add(entry);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "failed to write entry {0} into file: {1}", new Object[]{entry, e});
        } finally {
            if (oos != null) {
                try {
                    oos.flush();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "failed to flush file: {0}", e);
                }
            }
        }

        return logEntries.size()-1;
    }

    public void finalize() {
        if (oos != null) {
            try {
                oos.close();
            } catch (Exception e) {
                logger.log(Level.FINE, "failed to close file: {0}", e);
            }
        }
    }
}

package sapphire.policy.util.consensus.raft;

import java.util.ArrayList;

/**
 * Log entry for RAFT algorithm.
 */
class LogEntry {
    Object operation;
    int term;
    LogEntry(Object operation, int term) {
        this.operation = operation;
        this.term = term;
    }
}

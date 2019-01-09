package amino.run.policy.util.consensus.raft;

import java.io.Serializable;

/** Log entry for RAFT algorithm. */
public class LogEntry implements Serializable {
    Object operation;
    int term;

    LogEntry(Object operation, int term) {
        this.operation = operation;
        this.term = term;
    }
}

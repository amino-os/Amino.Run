package sapphire.policy.util.consensus.raft;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by quinton on 3/30/18.
 **
 * Persistent state on all servers.  //TODO Make persistent, restore on start.
 * All methods are thread-safe, and use optimistic concurrency for updates.
 */

class PersistentState {
    PersistentState() {
        this.currentTerm = 0;
        this.votedFor = NO_LEADER;
        this.log = new ArrayList<LogEntry>();
        this.myServerID = UUID.randomUUID();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constants
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static final int INVALID_INDEX = -1;
    public static final UUID NO_LEADER = new UUID(0L, 0L);
    private volatile Integer currentTerm = -1;
    private volatile UUID votedFor = NO_LEADER;
    private volatile List<LogEntry> log; // TODO: Garbage collection - log growth currently unbounded.
    public final UUID myServerID;

    /**
     * Get the current term. Is thread-safe.
     * @return
     */
    int getCurrentTerm() {
        synchronized(this.currentTerm) {
            return this.currentTerm;
        }
    }

    /**
     * Set the current term if currentTerm == preconditionTerm. i.e. optimistic concurrency.
     * @param term new value
     * @param preconditionTerm
     * @return currentTerm (after possibly setting to new value). Clients should check this return value
     * to determine whether it was set and retry as required.
     */
    int setCurrentTerm(int term, int preconditionTerm) {
        synchronized(this.currentTerm) {
            if (this.currentTerm == preconditionTerm) {
                this.currentTerm = term;
            }
            return this.currentTerm;
        }
    }

    /**
     * Increment current term, iff currentTerm == preconditionTerm, i.e. optimistic concurrency.
     * Clients should check return value to determine whether current term was incremented,
     * and retry as necessary.
     * @param preconditionTerm
     * @return value of current term (after possible increment)
     */
    int incrementCurrentTerm(int preconditionTerm) {
        synchronized(this.currentTerm) {
            if (this.currentTerm == preconditionTerm) {
                this.currentTerm++;
            }
            return this.currentTerm;
        }
    }

    UUID getVotedFor() {
        synchronized (this.votedFor){
            return this.votedFor;
        }
    }

    /**
     * Set votedFor, iff current value of voteFor == preconditionVotedFor, i.e. optimistic concurrency.
     * Clients should check return value to determine whether the value was set,
     * and retry as necessary.
     * @param preconditionVotedFor
     * @return value of votedFor (after possible update)
     */
    UUID setVotedFor(UUID votedFor, UUID preconditionVotedFor) {
        synchronized(votedFor) {
            if (this.votedFor.equals(preconditionVotedFor)) {
                this.votedFor = votedFor;
                return votedFor;
            } else {
                return this.votedFor;
            }
        }
    }

    List<LogEntry> log() {
        synchronized(log) {
            return this.log;
        }
    }

    void setLog(List<LogEntry> newLog) {
        synchronized(log) {
            this.log = newLog;
        }

    }
}


package sapphire.policy.util.consensus.raft;

/**
 * Created by quinton on 3/30/18.
 */

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sapphire.policy.util.consensus.raft.PersistentState;

import static sapphire.policy.util.consensus.raft.PersistentState.INVALID_INDEX;
import static sapphire.policy.util.consensus.raft.PersistentState.NO_LEADER;

/**
 * Volatile state on all servers.  Stored in RAM, and lost on restarts.
 * All methods are thread-safe, and use optimistic concurrency for updates.
 */
class VolatileState {
    /**
     * All the other servers.
     */
    public ConcurrentMap<UUID, Server> otherServers = new ConcurrentHashMap<UUID, Server>();
    int commitIndex = INVALID_INDEX;
    int lastApplied = INVALID_INDEX;
    /**
     * Who is the current leader?
     */
    UUID currentLeader = PersistentState.NO_LEADER;  // Initially no leader.
    private volatile Server.State state = Server.State.NONE;

    private Object syncObj = new Object(); // Private object to synchronize primitives on, so that clients can synchronize on this, and avoid deadlock.
    int getCommitIndex() {
        synchronized(syncObj) {
            return this.commitIndex;
        }
    }

    /**
     * Set commitindex iff commitIndex == preconditionValue, i.e. optimistic concurrency.
     * @param value
     * @param preconditionValue
     * @return value of commitIndex after possible update.  Clients should check return value to
     * determine whether the update was performed, and retry as necessary.
     */
     int setCommitIndex(int value, int preconditionValue) {
        synchronized(syncObj) { // Can't synchronize on primitive ints in Java
            if (this.commitIndex == preconditionValue) {
                this.commitIndex = value;
            }
            return this.commitIndex;
        }
    }

    int getLastApplied() {
        synchronized (syncObj) {
            return this.lastApplied;
        }
    }

    /**
     * Set lastApplied iff lastApplied == preconditionValue, i.e. optimistic concurrency.
     * @param value
     * @param preconditionValue
     * @return value of lastApplied after possible update.  Clients should check return value to
     * determine whether the update was performed, and retry as necessary.
     */
    int setLastApplied(int value, int preconditionValue) {
        synchronized (syncObj) {
            if (this.commitIndex == preconditionValue) {
                this.commitIndex = value;
            }
            return this.commitIndex;
        }
    }

    int incrementLastApplied(int preconditionValue) {
        synchronized(syncObj) {
            if (this.lastApplied == preconditionValue) {
                ++this.lastApplied;
            }
        }
        return this.lastApplied;
    }

    Server.State getState() {
        synchronized(state) {
            return state;
        }
    }

    Server.State setState(Server.State value, Server.State preconditionValue) {
        synchronized (state) {
            if (this.state == preconditionValue) {
                this.state = value;
            }
            return this.state;
        }
    }

    /**
     * Which Server is the current leader?
     * @return null if there is no leader, else the current leader.
     */
    UUID getCurrentLeader() {
        synchronized(currentLeader) {
            return currentLeader;
        }
    }

    void removeCurrentLeader() {
        synchronized(currentLeader) {
            currentLeader = NO_LEADER;
        }
    }

    synchronized void setCurrentLeader(UUID leader) {
        synchronized(currentLeader) {
            if (!this.currentLeader.equals(leader)) {
                this.currentLeader = leader; // We learned about a new leader.
            }
        }
    }


}


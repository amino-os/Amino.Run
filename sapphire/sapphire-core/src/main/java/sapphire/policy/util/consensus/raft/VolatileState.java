package sapphire.policy.util.consensus.raft;

/**
 * Created by quinton on 3/30/18.
 */

import java.io.Serializable;
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
class VolatileState implements Serializable {
    /**
     * All the other servers.  Just make it public to avoid having to re-implement the whole Map interface.
     * ConcurrentMap is already thread-safe, so can be accessed directly.
     */
    public ConcurrentMap<UUID, Server> otherServers = new ConcurrentHashMap<UUID, Server>();
    Integer commitIndex = INVALID_INDEX;
    Integer lastApplied = INVALID_INDEX;
    /**
     * Who is the current leader?
     */
    UUID currentLeader = PersistentState.NO_LEADER;  // Initially no leader.
    private volatile Server.State state = Server.State.NONE;

    int getCommitIndex() {
        synchronized(commitIndex) {
            return commitIndex;
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
        synchronized(commitIndex) { // Can't synchronize on primitive ints in Java
            if (commitIndex == preconditionValue) {
                commitIndex = value;
            }
            return commitIndex;
        }
    }

    int getLastApplied() {
        synchronized (lastApplied) {
            return lastApplied;
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
        synchronized (lastApplied) {
            if (lastApplied == preconditionValue) {
                lastApplied = value;
            }
            return lastApplied;
        }
    }

    int incrementLastApplied(int preconditionValue) {
        synchronized(lastApplied) {
            if (lastApplied == preconditionValue) {
                ++lastApplied;
            }
        }
        return lastApplied;
    }

    Server.State getState() {
        synchronized(state) {
            return state;
        }
    }

    Server.State setState(Server.State value, Server.State preconditionValue) {
        synchronized (state) {
            if (state == preconditionValue) {
                state = value;
            }
            return state;
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

    void setCurrentLeader(UUID leader) {
        synchronized(currentLeader) {
            if (!currentLeader.equals(leader)) {
                currentLeader = leader; // We learned about a new leader.
            }
        }
    }


}


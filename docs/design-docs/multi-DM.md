# Introduction

Multiple DM's may be associated with each MicroService.  This
document describes some combinations of DM's, and 
how these combinations behave and might be useful.

In general, DM's in the same category are mutually exclusive, and it
does not usually make sense to combine them.  For example, KeepInCloud
and KeepOnDevice do not make sense together - choose one or the other.
Similarly, choose between LockingTransactions and
OptimisticTransactions, and not both.  

Conversely, by combining DM's in different categories, new and often
very useful deployment behaviors can be achieved without having to
write any code.  For example, by combining ConsensusRSM, KeepInCloud,
and LockingTransactions, it is possible to get the union of their
behaviors, namely:

1. A RAFT-based replicated state machine (ConsensusRSM). 
2. All replicas remain in a given cloud zone (KeepInCloud).
3. Multi-operation read-write transactions using server-side locking
   (LockingTransactions)
   
## KeepInCloud + LockingTransactions + ConsensusRSM

### Desired behavior

1. Client creates a new instance or obtains a reference to an existing MicroService.
2. Client starts a locking transaction, by calling startTransaction()
3. Client invokes multiple read and write operations against the
   MicroService.
4. Client either commits or rolls back the transaction.
5. Client expects the MicroService to be highly available,
   resilient to server machine failures (provided that concurrent
   failures are limited to a minority quorum).
6. Client expects the MicroService to be high performance (all
   quorum communication is on the local zone network).
7. Client does not expect the MicroService to be resilient to zone failure.   
   
### How it works under the hood

1. Client creates an instance of a MicroService (_new()).
  1. Kernel invokes group.onCreate() on all DM's (some handwaving
     here, but I think we can make it work).
	 1. KeepInCloud.group.onCreate() ensures that all replicas are in
        the required cloud zone.
	 1. LockingTransactions.group.onCreate() does nothing unusual.
     1. ConsensusRSM.group.onCreate() creates 2f+1 replicas (by invoking
     sapphire_replicate, which in turn invokes addServer on all DM's).
2. Client starts a locking transaction, by calling startTransaction()
   on the MicroService
  1. The above is intercepted by KeepInCloud.client.onRPC(), that does nothing
     other than server.onRPC().
  1. The above is intercepted by LockingTransactions.client.onRPC()
     that identifies startTransaction() and acquires a server-side lock by
     invoking LockingTransactions.server.acquireLock().
  1. The above is intercepted by ConsensusRSM.client.onRPC(), that
     invokes the RAFT consensus algorithm across all replicas to
     ensure that the RPC call (acquireLock() in this case)  is
     committed against the quorum.  (note that in the current
     implementation, LockingTransactions.server issues the lease
     identifier (a random UUID).  Given that there will be 2f+1
     servers in this case, 2f+1 different lease id's would be
     issued. So to make this work, there should be an option to have
     the lease ID generated on the client -
     LockingTransactions.client - to ensure that the lock identifier
     is consistent across all replicas.  This change should be
     straightforward.

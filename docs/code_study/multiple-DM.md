# Introduction

Multiple DM's may be associated with each Sapphire Object.  This
document describes some combinations of DM's, and 
how these combinations behave and might be useful.

The basic DM's are described in [DMList.md](DMList.md).

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

1. Client creates a new instance or obtains a reference to an existing Sapphire Object.
2. Client starts a locking transaction, by calling startTransaction()
3. Client invokes multiple read and write operations against the
   Sapphire Object.
4. Client either commits or rolls back the transaction.
5. Client expects the Sapphire Object to be highly available,
   resilient to server machine failures (provided that concurrent
   failures are limited to a minority quorum).
6. Client expects the Sapphire Object to be high performance (all
   quorum communication is on the local zone network).
7. Client does not expect the SO to be resilient to zone failure.   
   
### How it works under the hood

1. Client creates an instance of a Sapphire Object (_new()).
  1. Kernel creates an instance of the SO, 
  1. and a chain of client and server DM's, in "onion order" (i.e.
     client-DM's in the order declared, server-DM's in the reverse order: 
	 (KIK-C -> LT-C -> CRSM-C -> CRSM-S -> LT-S -> KIK-S -> SO).
  1. and a set of independent group-DM's (i.e. group DM's are not 
     chained together TODO IS THIS CORRECT?)
  1. initially none of the DM components are initialized (i.e. none of their onCreate() methods
     have been called yet).  Therefore client-DM's and server-DM's do not yet know their group-DM,
	 and group-DM('s) do not know their (first) server-DM.
  1. Kernel invokes onCreate() on all DM's in the following sequence (NOTE: There's an inherent problem here,
     in that the group-DM and server-DM both get initialized with the other one (i.e. 
	 groupDM.onCreate(serverDM) and serverDM.onCreate(GroupDM) , so inherently one or the other will be 
	 uninitialized when passed to it's counterpart.  Perhaps that is why the original code had addServer() 
	 being called by the kernel, before onCreate(), to break this cyclic dependency?):
	 1. KeepInCloud.group.onCreate() ensures that all replicas are in
        the required cloud zone.
	 1. LockingTransactions.group.onCreate() does nothing unusual.
     1. ConsensusRSM.group.onCreate() creates 2f+1 replicas (by invoking
     sapphire_replicate, which in turn invokes addServer on all DM's).
2. Client starts a locking transaction, by calling startTransaction()
   on the SO
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
	 
   
## KeepInCloud + LockingTransactions + ConsensusRSM

### Desired behavior

1. Client creates a new instance or obtains a reference to an existing Sapphire Object.
2. Client starts a locking transaction, by calling startTransaction()
3. Client invokes multiple read and write operations against the
   Sapphire Object.
4. Client either commits or rolls back the transaction.
5. Client expects the Sapphire Object to be highly available (due to ConsensusRSM),
   resilient to server machine failures (provided that concurrent
   failures are limited to a minority quorum).
6. Client expects the Sapphire Object to be high performance (all
   quorum communication is on the local zone network).
7. Client does not expect the SO to be resilient to zone failure.   
   
### How it works under the hood

1. Client creates an instance of a Sapphire Object (_new()).
  1. Kernel invokes group.onCreate() on all DM's (some handwaving
     here, but I think we can make it work).
	 1. KeepInCloud.group.onCreate() ensures that all replicas are in
        the required cloud zone.
	 1. LockingTransactions.group.onCreate() does nothing unusual.
     1. ConsensusRSM.group.onCreate() creates 2f+1 replicas (by invoking
     sapphire_replicate, which in turn invokes addServer on all DM's).
1. Client starts a locking transaction, by calling startTransaction()
   on the SO
  1. The above is intercepted by KeepInCloud.client.onRPC(), thast does nothing
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
	 
## AtleastOnce + CacheLease + OptimisticTransactions + LoadBalancedFrontend

### Desired behavior

1. Client starts an optimistic transaction, by calling startTranaction() (from OptimisticTransactions)
1. Client invokes multiple read and write operations against the
   Sapphire Object.  Any of the SO's methods may be invoked.
1. Client either commits or rolls back the transaction (by calling 
   commitTransation() or rollbackTransaction())
1. Client expects the Sapphire Object to be highly performant, as all 
   read operations occur locally (CacheLease), and only write operations incur network 
   overhead.
1. Client expects not to see exceptions due to transient network errors, temporary server overload 
   or cache lease contention (because AtLeastOnce automatically retries on transient errors).
1. Client expects all reads and writes within an optimistic transaction to be consistent (i.e.
   isolated from other transactions), but not 
   across transactions (because optimistic transactions are load balanced across potentially 
   inconsistent replicas). 
   
### How it works under the hood

1. Client creates an instance of a Sapphire Object (_new()).
  1. Kernel 
  1. Kernel invokes group.onCreate() on all DM's:
	 1. Only LoadBalancedFrontend.group.onCreate() does anything useful,
	    creating a configurable number of replicas of the object, across 
		which load balancing will be performed.  It calls server.sapphire_replicate() 
		to create each replica, and sapphire_pin(OID, node_selection_criteria) to move 
		each one so that they don't end up on the same node (and perhaps specifies 
		other node selection criteria too,
		like keeping replicas in the same zone, or spreading them across different zones, 
		all according to the configuration of the LBFE DM).
	 1. For each replica created above, sapphire kernel ensures that it is fronted by a 
	    chain of server policies, in reverse order (LBFE -> OT -> CL -> ALO -> SO), on 
		the node where the replica resides.
	 1. Also, the ServerPolicyStub returned for each replica is backed by a chain of client policies
	    in forward order (ALO -> CL -> OT -> LBFE) [Quinton: does this work??!]
1. Client starts a locking transaction, by calling startTransaction()
   on the SO
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
	 
	 
	 
	 
	 
	 

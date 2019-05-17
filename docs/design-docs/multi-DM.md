# Introduction

Multiple DM's may be associated with each MicroService.  This
document describes some combinations of DM's, and 
how these combinations behave and might be useful.

The basic DM's are described in [DM List](DM-list.md).

In general, DM's in the same category are mutually exclusive, and it
does not usually make sense to combine them.  For example, PeriodicCheckpoint
and ExplicitCheckpoint do not make sense together - choose one or the other.
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

# Example combinations

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

1. Client creates an instance of a MicroService (create()).
  1. Kernel invokes group.onCreate() on all DM's (some handwaving
     here, but I think we can make it work).
	 1. KeepInCloud.group.onCreate() ensures that all replicas are in
        the required cloud zone.
	 1. LockingTransactions.group.onCreate() does nothing unusual.
     1. ConsensusRSM.group.onCreate() creates 2f+1 replicas (by invoking
     replicate(), which in turn invokes addServer on all DM's).
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
     
## AtLeastOnceRPC + DHT + ConsensusRSM

### Desired behavior

1. Client creates a new instance or obtains a reference to an existing MicroService.
2. Client makes a call to an application method in Microservice and gets the result back.
3. Client DM retries on failure for certain duration.
4. Microservice replicas are sharded.
5. Client expects the MicroService to be highly available,
   resilient to server machine failures (provided that concurrent
   failures are limited to a minority quorum).
6. Client expects the MicroService to be high performance (all
   quorum communication is on the local zone network).
7. Client does not expect the Microservice to be resilient to zone failure.   
   
### How it works under the hood

1. Client creates an instance of a MicroService (create()).
	1. Kernel invokes group.onCreate() on all DM's in the order of inner most DM to outer most DM.
		1. AtLeastOnceRPCPolicy.GroupPolicy.onCreate() does nothing unusual.	
		1. DHTPolicy.GroupPolicy.onCreate() ensures that each Microservice is distributed
		based on the input parameters of the application method.
		1. ConsensusRSMPolicy.GroupPolicy.onCreate() creates 2f+1 replicas (by invoking 
		replicate(), which in turn invokes addServer on all DM's).
2. Client starts a invocation of the application method with input parameter(s).
  	1. The above is intercepted by AtLeastOnceRPCPolicy.ClientPolicy.onRPC(), that does nothing 
	other than server.onRPC() unless there is a failure.
  	1. The above is intercepted by DHTPolicy.ClientPolicy.onRPC() 
	that finds a responsible node based on the application parameter.
  	1. The above is intercepted by ConsensusRSM.ClientPolicy.onRPC(), that calls the server policy
	of the leader node which invokes the RAFT consensus algorithm across all replicas to ensure that
	the RPC call is committed against the quorum
  	1. The last DM in the DM client chain (ConsensusRMM.ClientPolicy.onRPC()) calls the chosen server.
3. Server starts a invocation of the application method with stacked parameters.
  	1. The server unravels the parameters which resolves ConsensusRSM.ServerPolicy.onRPC()
  	1. ConsensusRSM.ServerPolicy.onRPC() calls the leader (itself) and followers (replcas).
  	1. When above DM invokes for the method in application object, it calls 
	the DHTPolicy.ServerPolicy.onRPC() as the application object references the DHTPolicy.ServerPolicy.
  	1. DHTPolicy.ServerPolicy.onRPC() invokes the application object which calls the 
	AtLeastOnceRPCPolicy.ServerPolicy.onRPC() as the application object references the 
	AtLeastOnceRPCPolicy.ServerPolicy.
  	1. Finally, AtLeastOnceRPCPolicy.ServerPolicy.onRPC() invokes the actual appplication object.
4. Result of the application method call is propagated back via the DM chain it has went through 
(at the leader node and the follower nodes).
  	1. Result from the application object is returned to the AtLeastOnceRPCPolicy.ServerPolicy.onRPC().
  	1. Above result is returned to the DHTPolicy.ServerPolicy.onRPC().
  	1. Above result is returned to the ConsensusRSM.ServerPolicy.onRPC().
5. Result of the server side call is returned to the client.
 	1. Above result is returned to the ConsensusRSM.ClientPolicy.onRPC().
  	1. Above result is returned to the DHTPolicy.ClientPolicy.onRPC().
  	1. Above result is returned to the AtLeastOnceRPCPolicy.ClientPolicy.onRPC().
  	1. Above result is returned to the application.
	

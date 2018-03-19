# Master/Slave with Sync Replication
![MasterSlaveSyncDM](../images/MasterSlaveSynchronousDiagram.png)

# Master/Slave with Async Replication

## Components
Like most DMs, *LoadBalancedMasterSlaveDM* has three components:

* **ClientPolicy**: A Sapphire object with *LoadBalancedMasterSlaveDM* has two replicas: master and slave. Client policy queries *group policy* to figure out master and slave. It sents *muttable* operations to master, and *immutable* operations to one of the replicas in round robin manner.

* **ServerPolicy**: Each replica has its own *server policy*. *Server policies* compete for a lock in *group policy*. The server who owns the lock is the master; the other server is the slave. A server operates either in master mode or in slave mode. In master mode, the server is in charge of appending request to log file, replicating request to slave, and applying request on Sapphire object. In slave mode, the server is in charge of receving replicated requests from master, appending requests to log, and apply requests to Sapphire object. 

* **GroupPolicy**: *Group policy* provides a lock service. It keeps track of the status of master and slave.

## Normal Process
![MasterSlaveDM](../images/MasterSlaveDiagram.png)

Above diagram shows the normal process sequence:

1. Client sends request to server. Client figures out which replica is master by querying group.
2. Server append the request in log file.
3. Server replicate the request to slave asynchorously. Upon receiving the replicated request, slave appends the request to its log, and apply the request to Sapphire object asynchronously.
4. Server applies request to Sapphire object.
5. Server sends the response back to client.

A few things worth mentioning:

* Log file maintains two pointers: *LargestReplicatedIndex* and *LargestCommittedIndex*. Because master applies every request to its Sapphire object, on master, the LargestCommittedIndex is also the LargestReceivedIndex on the log file. Because master replicates requests to slave asnychronously, LargestReplicatedIndex may sometimes fall bahind LargestCommittedIndex on master. Because every request received by slave is also a successfully replicated request, on slave, the LargestReplicatedIndex on slave is also the LargestReceivedIndex. Because slave applies requests on Sapphire asynchronously, LargestCommittedIndex may sometimes fall behind LargestReplicatedIndex on slave. 

* Server periodically snapshot its log file for failure recovery purpose. Snapshots are stored in a snapshot log file.

* According to the definition of LoadBalancedMasterSlave DM, the replication from master to slave is asynchronous. Due to asynchronous replication, users may experience data loss during fail over. We should probably change it to synchronous replication.

## Implementations

Below are some key classes used in implementation:

* **StateManager**: Keeps track of replica state changes, e.g. between master to slave or vice versa.
* **FileLogger**: Keeps track of log entries and takes snapshot periodically.
* **AsyncReplicator**: Replicates requests from master to slave asynchronously. Only runs in master mode.
* **CommitExecutor**: Apply requests on Sapphire object.

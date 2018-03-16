# Components
Like most other DMs, *LoadBalancedMasterSlaveDM* has three components:

* **ClientPolicy**: A Sapphire object with *LoadBalancedMasterSlaveDM* has two replicas, master replica and slave replica. Client policy queries *group policy* to figure out master and slave. It sents *muttable* operations to master, and *immutable* operations to one of the replicas in round robin manner.

* **ServerPolicy**: Each replica has its own *server policy*. *Server policies* compete for a lock in *group policy*. The server who owns the lock is the master; the other server is the slave. A server operates either in master mode or in slave mode. In master mode, the server is in charge of appending request to log file, replicating request to slave, and applying request on Sapphire object. In slave mode, the server is in charge of receving replicated requests from master, appending requests to log, and apply requests to Sapphire object. 

* **GroupPolicy**: *Group policy* provides a lock service. It keeps track of the status of master and slave.

# Overview
![MasterSlaveDM](../images/MasterSlaveDiagram.png)

## Objective
*LockingTransactions* DM uses lock to enforce, on a *single Sapphire* object, the serial execution of transactions each of which consists of one or many RPC calls.

Transactions should satisfy ACID properties. Note that these are not standard database ACID definitions. I modified them to suit our DCAP use cases. 
* Atomicity: Either all RPCs in the entire transaction are executed or nothing is executed
* Consistency: If the Sapphire object has replicas, the transaction should be executed on all replicas
* Isolation: RPCs in a transaction should not be interfered by other RPCs (either in a different transaction or outside any transaction)
* Durability: Once a transaction is commited, the state of the Sapphire object will persist even if the system crashes. When a Sapphire object is resurrected, it should be restored to the last commited state, even if it is resurrected on a different host.

## Failure Cases

#### Incomplete Transaction
User forgets to commit or rollback a transaction. 

#### Client Dies
Client dies after starting a transaction.

#### Network Failure
Network breaks during a transaction.

#### Server Dies
Server dies during a transaction. The current transaction should be rolled back. Sapphire object should be restored to the last committed state after being resurrected.


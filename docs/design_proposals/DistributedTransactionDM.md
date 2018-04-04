# Use Case
## Ideal User Experience
The `transferMoney` method in the following class is annotated with `@Trasaction` which means that operations within the method
should be executed as one transaction, either all succeed or all fail. Operations in `transferMoney` method, e.g. `account1.debit` or 
`account2.credit` or `ledger.log` are remote service calls.

```
public class Finance {   
    Ledger book;
    ...
    @Transaction
    public boolean transferMoney(BankAccountFoo account1, AccountBar account2, unsigned int amount) {
       account1.debit(amount);
       account2.credit(amount);
       this.book.log(String.Format("transferred from %s to %s amount %d", account1.name(), account2.name(), amount)); 
       return true;
    }
}
```

Transactional code is written as below:
```
Finance finance;
BankAccountFoo account1;
BankAccountBar account2;
...
try{
  ...
  boolean success = finance.transferMoney(account1, account2, 100);
  ...
} catch (TransactionException te) {
  ...
}
```
### Options
* We may constraint (transaction-annotated) Transactional method declared as static to express the scope of resources involved in the transaction for the first phase, for the sake of simpilicty.
* We may consider other forms of failure notification than the exception.   

## Phase 1 - Collaborative 2PC Distributed Transaction
### Assumptions of Phase 1
<br/>All participants of the transaction have to have 2PC-compiliant transaction DM.
<br/>Concuurent control policy is lock-based perssimitive. 
### User Experience
Code to define transaction
```
class FinanceTxn implements Sapphire<DCAPTranaction> {
    Ledger book;
    ...
    public boolean transferMoney(BankAccountFoo account1, AccountBar account2, unsigned int amount) {
       account1.debit(amount);
       account2.credit(amount);
       this.book.log(String.Format("transferred from %s to %s amount %d", account1.name(), account2.name(), amount)); 
       return true;
    }
}
```
Code to invoke transaction
```
FinanceTxn finance = (FinanceTxn)Sapphire.new_(FinanceTxn.class);
try {
  finance.transferMoney(account1, account2, 100);
  ...
} catch(TransactionException te) {
  ... // nothing on failure
}
```
![](../images/tx-participants.png)
#### Alternative options
* Explict specification of participants of the tranaction.
<br/> explicitly specifying the scope of Sapphire objects involved in the transaction helps to reduce the overhead of transaction and make transaction less likely fail due to false negative conflicting. The con is the decision task shifted to application developer, which is error prone when referals to other Sapphire objects exist.
### DM Design
#### DCAPTransaction
* client policy: not much
* server policy
<br/>acting as distributed transaction coordinator, enforcing 2PC protocol (1st phase)
<br/> ![](../images/tx-state-chart.png)
* group policy: not much
* TranactionException spec
<br/>name: "DCAP_transaction_failure"
<br/>inner exception: present if available from runtime
#### 2PC-Compliant Participanting DM
* client policy
  <br/> register the target to transaction scope
* server policy
<br/> tx_join
<br/> tx_vote
<br/> tx_commit
<br/> tx_abort
#### Participant state transition in 2PC transaction
<br/>![participant transaction state diagram](../images/tx-participant-svrdm.png)
## External Databases
External databases, if the adequate interface of 2PC participants is present, should be able to take part in the distributed transaction while the ACID property being well maintained. The wrapper of Sapphire object encapsulating the database access should expose the desired callback methods for the transaction coordinator to manage the transaction as a whole.
```
interface I2PCParticipant {
  void onJoin(UUID txnId);
  VoteStatus onVoteRequested(UUID txnId);
  void onCommit(UUID txnId);
  void onAbort(UUID txnId);
}
```

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

## Phase 1 
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
#### Required enhancements from participanting DMs
* tx_join
* tx_vote
* tx_commit
* tx_abort
#### participant state transition in 2PC transaction
<br/>![participant transaction state diagram](../images/tx-participant-svrdm.png)

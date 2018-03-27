# Use Case
The `transferMoney` method in the following class is annotated with `@Trasaction` which means that operations within the method
should be executed as one transaction, either all succeed or all fail. Operations in `transferMoney` method, e.g. `account1.debit` or 
`account2.credit` or `ledger.log` are remote service calls.

```
public class Finance {   
    @Transaction
    public static boolean transferMoney(BankAccount account1, Account account2, unsigned int amount, Ledger book) {
       account1.debit(amount);
       account2.credit(amount);
       book.log(String.Format("transferred from %s to %s amount %d", account1.name(), account2.name(), amount)); 
       return true;
    }
}
```

Transactional code is written as below:
```
BankAccount account1, account2;
Ledger book;
...
try{
  ...
  boolean success = Finance.transferMoney(account1, account2, 100, book);
  ...
} catch (TransactionException te) {
  ...
}
```

# Design Notes
* (transaction-annotated) Transactional method declared as static to express the scope of resources involved in the transaction for the first phase. We may extend to instance methods in the futher.
* The transaction method throws exception on failure in the first phase. We may consider other form of failure notification in the future.   

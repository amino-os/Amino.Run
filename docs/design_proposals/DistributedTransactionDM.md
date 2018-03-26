# Use Case
The `adopt` method in the following class is annotated with `@Trasaction` which means that operations within the method
should be executed as one transaction, either all succeed or all fail. Operations in `adop` method, e.g. `users.add` or 
`pets.add` are remote service calls.

```
public class PetStore {
    private UserService users;
    private PetService pets;
    
    @Transaction
    public boolean adopt(Pet p, User u) {
       users.add(u);
       pets.add(p);
       p.addOwner(u);
    }
}
```

Transactional code is written as below:
```
PetStore petStore;
Pet p;
User u;
...
try{
  ...
  boolean bool = petStore.adopt(p, u);
  ...
} catch (TransactionException te) {
  ...
}
```

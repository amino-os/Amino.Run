# DM Implementation
Sapphire paper mentions that every DM has three components: a *proxy*, a *instance manager*, and a *coordinator*. The terminologies you see in [source code](https://github.com/Huawei-PaaS/DCAP-Sapphire/blob/master/sapphire/sapphire-core/src/main/java/sapphire/policy/SapphirePolicyUpcalls.java), however, are different from the ones used in the paper. In source code, they are  called *ClientSidePolicy*, *ServerSidePolicy*, and *GroupPolicy*. (This is my understanding. Correct me if I am wrong.) To implement a DM means to write a class that implements the aforementioned three interfaces.

The following interface definitions were copied from [sapphire source  code](https://github.com/Huawei-PaaS/DCAP-Sapphire/blob/master/sapphire/sapphire-core/src/main/java/sapphire/policy/SapphirePolicyUpcalls.java). Be aware that this is **not the final version**. They are subject to future changes. For example, sapphire paper also mentions `onDestroy`, `onLowMemory`, and `onHighLatency` in interfaces. We probably need to add these functions into the interface later. 

```java
// proxy interface
public interface  SapphireClientPolicyUpcalls extends Serializable {
	public void onCreate(SapphireGroupPolicy group);
	public void setServer(SapphireServerPolicy server);
	public SapphireServerPolicy getServer();
	public SapphireGroupPolicy getGroup();
	public Object onRPC(String method, ArrayList<Object> params) throws Exception;
}

// instance manager interface
public interface SapphireServerPolicyUpcalls extends Serializable {
	public void onCreate(SapphireGroupPolicy group);
	public SapphireGroupPolicy getGroup();
	public Object onRPC(String method, ArrayList<Object> params) throws Exception;
	public void onMembershipChange();
}
	
// coordinator interface
public interface SapphireGroupPolicyUpcalls extends Serializable {
	public void onCreate(SapphireServerPolicy server);
	public void addServer(SapphireServerPolicy server);
	public ArrayList<SapphireServerPolicy> getServers();
	public void onFailure(SapphireServerPolicy server);
	public SapphireServerPolicy onRefRequest();
}
```
# DM Usage
App developers use a DM by passing the name of DM class as a generic type to `SapphireObject` interface. In the following example, `UserManager` declares to use DM `DHTPolicy` to manage its users.

```java
public class UserManager implements SapphireObject<DHTPolicy>, DHTInterface {
	Map<DHTKey, User> users;
	private TagManager tm;
    ...
}
```

Again, this is subject to change. We may use Java annotation down the road.

```java
@SapphireObject
@Proxy(name="DHTClient", 
@InstanceManager(name="DHTServer")
@Coordinator(name="DHTCoordinator")
public class UserManager {
}
```
# DM Injection

![](./images/SapphireOverview.png)

In the above diagram, the dashed arrow lines are remote method invocations between Sapphire objects, the solid arrow lines are local method invocations within a Sapphire object (i.e. within JVM). DMs sit below Sapphire objects. They are essentially proxies for Sapphire objects. When one object calls a remote method on another Sapphire object, the request will first be processed by the DM on client side, (i.e. `DM.Proxy`), the DM on server side (i.e. `DM.InstanceManager`) , and then finally be sent to the real Java object.

As shwon in the followign diagram, DM consists of many automatically generated components. All these components are wired up by DCAP automatically. Therefore as an App developer, you cannot use normal Java `new` keyword to create a Sapphire object. Sapphire objects have to be created by [`Sapphire._new()`](https://github.com/Huawei-PaaS/DCAP-Sapphire/blob/master/sapphire/sapphire-core/src/main/java/sapphire/runtime/Sapphire.java). Moreover, to invoke a method on an Sapphire object, you must first get the reference to object from OMS - OMS will return a *stub* of the actual Sapphire object.
 
![](./images/DCAP_StubStructure.png)

![](./images/DCAP_RemoteMethodInvocationSequence.png)

# DM List
Here are 26 DMs proposed in Sapphire paper. I will start by writing down my personal thoughts on each DM. The purpose is to trigger disccusions within the team so that we can quickly build up concensus on the purpose, the implementation, and the value of each DM. 

I will assign a rate, LOW/MED/HIGH, to each DM to indicate its value to App developers. Again, it is my perosnal judgement. You are welcome to chime in with your opinions.

![](./images/DMList.png)

### Immutable (N/A)
> Efficient distribution and access for immutable SOs

<span style="color:blue">Should *immutable* be a property declared on Sapphire object, or a DM?</span> 

### AtLeastOnceRPC (LOW)
> Automatically retry RPCs for bounded amount of time

This DM will retry failed operations until timeout is reached.

The value of this DM is rated because many SDKs provide similar retry mechanism. App developers have a lot of choices.

By the way, to make this DM work properly, we have to make one change to the current DM mechanism:

* Provide Operation Level Support in DM: DMs today are proxies of Sapphire objects in which case DM logics are applied to all operations of a Sapphire object. Retry configuration, however, may vary a lot from operation to operation. DM should provide operation level support.


### KeepInPlace / KeepInRegion / KeepOnDevice (N/A)
> Keep SO where it was created (e.g., to access device-specific APIs)

If I understand correctly, by default, SOs cannot move. In order to make a SO mobile, the SO must be managed by some special DM which has the object moving capability. Do we really need a `KeepInPlace` DM? If a SO is not supposed to move, we simply don't associate any DM to this SO. 

Rather than defining *KeepInPlace* as a DM, I feel that it is better to define it as annotation on *Sapphire objects*. If a *Sapphire object* is declared as *KeepInPlace*, then no DM should move it.

<span style="color:blue">Should *KeepInRegion* and *KeepOnDevice* properties declaredon Sapphire objects, or DM implementations?</span>

### ExplicitCaching (LOW)
> Caching w/ explicit push and pull calls from application

<span style="color:blue">Not sure what it is...</span>

### WriteThroughCaching (LOW)
> Caching w/ writes serialized to server and stale, local reads

*WriteThroughCache* directs write operations (i.e. mutable operations) onto cached object and through to remote object before confirming write completion. Read operations (i.e. immutable operations) will be invoked on cached object directly.

State changes on remote object caused by one client will not automatically invalidate to cached objects on other clients. Therefore *WriteThroughCache* may contain staled object.

The value of this DM is rated as LOW because 

* Many matual cache libraries out there. 
* It is not difficult for developers to write their customized client side write through cache. It is not a big deal for them even if we don't provide this DM. 

<span style="color:blue">To make *WriteThroughCache* work, DM needs a mechanism to distinguish *mutable* operations and *immutable* operations.</span>

### ConsistentCaching

### SerializableRPC
* Do we serialize across SO replicas? 

### Locking Transaction

### DurableSerializableRPC

### DurableTransactions
* Difference between ExplicitMigration and ExplicitCodOffloading?
* Difference between Offloading and Migration



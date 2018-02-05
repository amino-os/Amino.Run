### Mobility - Explicit Migration

#### According to the paper

In ExplicitMigration DM, the user is required to mention about the destination Kernel Server address in the application code.

 ##### Few Queries regarding above
 1. Ideally now, the application developer only knows about the other Kernel Servers' IP addresses and not anything more. So, just IP addresses would not suffice in most of the cases for choosing where to migrate the object to. So, we would need to expose few of the metrics to the application developer
 
 2. Even if we expose few of the metrics like for example, availability and capacity of CPU, RAM, memory of all the kernel servers, even then the next concern which would arise is that, ideally we should not put deployment decisions or concerns to the application right? (As one of the adavantage of the DCAP)
 
#### Proposals for new DM and enhancement under/for Mobility
  
  ##### RandomExplicitMigration
  In case of this DM, application will call a specific kind of explicitMigration method, for example, migrateObject(), where the application is just pressed on for some of the resource (CPU, RAM, memory or etc.) which are available in its own machine or server and it just wants to migrate to any other Kernel Server other than the current. In this case the drawback is that, it is not guaranteed that the other new server would have the required resource which was scarce in the current)
  
  ```java
  // Fully random across all Kernel Servers other than the current
  void migrateObject()
  ```
  
  Other option should be ideally, migrateObject(string preferred_resource), in which the preferred_resource can be "CPU" or "Memory" or any other resource. On the basis of resource mentioned by the user, we can shortlist few (we can do this many ways, one simple way would be is "top n" servers where the availability of that resource is maximum) and choose random out of that list. Following are some examples for the methods,
  
  ```java
  // Random on the basis of the resource specified by application as mentioned above
  void migrateObject(string preferred_resource)
  ```
  (These above functionalities can also be provided using Annotations)
  
  (This can be either added as an overloaded method to current migrateObject() of "ExplicitMigration" DM or as a separate DM under mobility named "RandomExplicitMigration" DM. But from the discussion previously it was suggested to have a separate DM, as we need to preserve the DM implementations of the paper to be inline with the explanation in paper)
  
  ##### ResourceBasedExplicitMigration (enhance ExplicitMigration DM)
  
  In case of this DM, application developer will mention the Destination Kernel Server Address. For application developer to be able to choose appropriate destination Kernel Server, we would have to expose resource availablities and resource capacities of all the Kernel Servers. This can be achieved in such a way that we return a sorted list of all Kernel Servers on the basis of resource type as shown below,
  
  ```java
  /* getServersSortByResource in OMS which returns a sorted list on the basis of resource type,
  element used for sorting can be just the resource availablity or
  ratio of availaibility and capacity of the resource or any other way can be finalized */
  ArrayList<InetSocketAddress> getServersSortByResource(string type_of_resource) 
  ```
  
  Or other way would be that, we return a map of all Kernel Servers and their resource related metrics on the basis of what resource type the application developer specifies as shown below
    
  ```java
  /* getServersWithResource in OMS which returns a map with app with all the KernelServers addresses
  along with the corresponding resource or a way to provide both availability and capacity */
  Map<InetSocketAddress, String> oms.getServersWithResource(string type_of_resource)
  ```
  Once we provide this, then the application developer using the getServers() to migrate becomes more useful, as now he would know more about other Kernel Servers, and will be able to choose and mentioned to where to migrate, rather than blindly migrating to a new Kernel Server by using the ExplicitMigration DM.
  
  One more small confusion is that, should the above mentioned change go into a new DM named, ResourceBasedExplicitMigration or to add as new method in OMS.
  
  (On a lighter note, all these resources have to keep getting updated at specific interval at OMS level, by communicating with the Kernel Servers or when this information is required or asked, then we can communicate with OMS and get it consolidated from all Kernel Servers and give back to user or some other way, can be thought over)
  
  So, in this way application will have the option (can be perceived as overhead or previlige) to choose the appropriate destination Kernel Server for explicit Migration.


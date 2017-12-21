
# Onboarding Process
* Send github account and Huawei email address to Sungwook
* Join [slack channel](https://huawei.slack.com/)
* Read [papers](https://sapphire.cs.washington.edu/research/)
* Read [source code](https://sapphire.cs.washington.edu/code.html)
* Read [notes](https://github.com/bitbyteshort/DCAP-Sapphire-Main/blob/master/docs/SapphireInternal.md)

# Objectives

The goal of DCAP (Distributed Cloud Application Platform) project is to build a *distributed programming platform*, based on the ideas in [Sapphire](https://sapphire.cs.washington.edu/papers/sapphire-osdi14.pdf) paper, which aims to simplify the programming of today's highly distributed mobile/cloud applications.

More specifically, DCAP project will deliver the following two pieces:

* *SDK*: a software library with 20+ predefined *DM*s which support a wide range of management tasks commonly occur in a distribued environment, such as consistent client-side caching, durable transactions, Paxos replication, and dynamic code offloading, etc.  
 
* *Kernel*: a light weight distributed runtime which manages and executes DCAP applications, i.e applications written with DCAP SDK. 

# Schedule

* *Charter*: 01/04/2018 
  * *Why* do we invest in DCAP? *What* customer issues do we address?
  * *What* are the competative advantages of DCAP? *What* is the uniqueness of DCAP? *Why* customers choose DCAP, rather than other solutions?
  * *What* are the key technologies in DCAP? 
* *PDCP*: 03/30/2018
  * *POC completes*: All potential risks should have been identified
  * *Design completes*: Clear design at module level. All risk areas should have mutually agreed mitigation plans 
  * A *clear* scope on what we will deliver by 08/30/2018
  
* *TDCP*: 08/30/2018
  * Deliver
  
# Technical Challenges

### NAT Issue
### Replace RMI with gRPC
### DCAP Application Deployment/Upgrade
### DCAP Cross Team Collaboration
### Multi Langurage Support

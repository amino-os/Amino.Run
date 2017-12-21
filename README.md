
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

|Phase | Date | Objective | Tasks|
|------|------|-----------|------|
|*Charter*| 01/04/2018 | * *Why* invest in DCAP? <br/> * *What* are the competative advantages? <br/> * *Why* customers choose DCAP? | Prepare charter PPTs |
| *PDCP*| 03/30/2018 |* POC Complete <br/> * Design Complete<br/> * Project Delivery Defined|  * Set up code repository and CI/CD process <br/> * Compose 5+ sophysticated DMs <br/> * Write/Port an application onto DCAP to demonstrate the power of DCAP <br/> * Finalize the design to solve NAT issue <br/> * Finalize the design to replace RMI with gPRC <br/> * Identify all potential risk areas <br/> * Come up proposals to mitigate each risk |
|*TDCP*| 08/30/2018 | Deliver DCAP | * Compose 20+ DMs <br/> * Develop a complicated application with DCAP <br/> * Deliver a toolset DCAP application compilation and packaging <br/> * Replace OMS with a high availability implementation <br/> * Replace Java Registry with a high availability implementation <br/> * Replace RMI with gRPC <br/> * Support code offloading between any two endpoints, e.g. between devices, between servers, or between devices and servers. <br/> * All DCAP deliveries pass acceptance tests|
|*Future*| TBD (not in the scope of this project) | Enhance DCAP | * Support multiple languages <br/> * Develop collaboration tools to ficilitate cross team development with DCAP <br/> * Explore the integration with related technologies, e.g. Istio, Kubernetes, etc.



  
# Technical Challenges

### NAT Issue
### Replace RMI with gRPC
### DCAP Application Deployment/Upgrade
### DCAP Cross Team Collaboration
### Multi Langurage Support

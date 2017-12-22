
# Onboarding Process
* Send github account and Huawei email address to Sungwook
* Join [slack channel](https://huawei.slack.com/)
* Read [papers](https://sapphire.cs.washington.edu/research/)
* Read [source code](https://sapphire.cs.washington.edu/code.html)
* Read [notes](https://github.com/bitbyteshort/DCAP-Sapphire-Main/blob/master/docs/SapphireInternal.md)

# Objectives

The overall goal of DCAP (Distributed Cloud Application Platform) project is to build a *distributed mobile programming platform*, based on three UW papers [Sapphire](https://sapphire.cs.washington.edu/papers/sapphire-osdi14.pdf), [Diamond](https://sapphire.cs.washington.edu/research/project/diamond.html), and [TAPIR](https://syslab.cs.washington.edu/research/tapir/), which aims to simplify the programming of today's highly distributed mobile/cloud applications.

In PaaS V3R3 technology project (ends in 08/2018), we will primarily focus on providing Sapphire capability in DCAP. More specifically, DCAP will provide two unique features to application programmers:

* DCAP supports code *offloading* - it allows *objects* to move between devices, edges, and servers in central cloud

* DCAP provides a collection of pre-built *DM*s which solve a wide range of distributed system tasks, such as offloading and caching - it frees application programmers from writing complex distributed logics


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

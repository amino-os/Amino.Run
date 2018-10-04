# What is DCAP

DCAP, the Distributed Cloud Application Platform, is an open source,
multilanguage development platform and distributed runtime environment
designed to make distributed applications much easier to design,
develop and operate.  It supports most commonly used programming
languages and is, by default, deployed and managed in container environments using
[Kubernetes](https://www.k8s.io).  Common examples of distributed applications include:

1. [mobile-cloud applications](https://www.techopedia.com/definition/26679/mobile-cloud-computing-mcc), 
2. [mobile backend style applications](https://en.wikipedia.org/wiki/Mobile_backend_as_a_service),
3. [edge computing](https://en.wikipedia.org/wiki/Edge_computing) applications and
4. other [cloud-native](https://github.com/cncf/toc/blob/master/DEFINITION.md) applications

All of these classes of applications share a common set of difficult
design and development challenges including performance, distributed
concurrency, remote invocation, synchronization, fault tolerance,
scalability, sharding, code and data migration, leader election, load
balancing, observability, fault diagnosis and many more.

DCAP is based on, and extends, several years of research work done at
the [University Of Washington Computer Systems
Lab](https://syslab.cs.washington.edu/research/) in
Seattle.<sup>1,2,3,4,5</sup> 

DCAP is alpha software, and not yet
suitable for production use.  We have a well-funded development team
actively working on getting it to production readiness, and actively
support contributions from the open source community.

# Why we created DCAP

In a nutshell, we created DCAP to make design, development and operation of
reliable, fast, distributed applications quicker, easier and more fun.

In summary, our approach is to:

1. provide a wide and expandable range of standard, re-usable,
   pluggable and production-ready Deployment Managers
   (DMs)](https://github.com/Huawei-PaaS/DCAP-Sapphire-Core/blob/master/docs/code_study/DMList.md)
   to solve many common distributed computing problems (including all
   of those mentioned above) so that you can focus on application
   logic, not solving hard distributed systems challenges.
2. make it very easy to plug combinations of these into new or
   existing application code, even if it was not designed to be
   distributed - in many cases a few lines of code can change a simple
   standalone application written to run on a single computer into a
   robust, scalable distributed, cloud-native application.  Create
   sharded, consistent replicas of your objects, or replicated shards.
   Either way it requires only a one-line code or configuration change
   to your application.
3. support a wide variety of programming languages - we recognise the
   need for different languages and embrace that need. Java,
   Javascript, Python, C++, Swift, Ruby, Rust and others<sup>6</sup>-
   we've got you covered - and without the need for clunky and inefficient
   REST or RPC library code to get them to talk to each other.  We
   encourage using multiple different langauages to develop
   different parts of a single application.
4. make it easy to deploy your application anywhere, and move it
   around (piece by piece) as you wish:
   1. On your local machine
   2. On (public or private) cloud servers
   3. On mobile devices (Android, iOS)
   4. On edge devices
   5. Even have DCAP move parts of your application around automatically at runtime
      to optimize performance, reliability or battery power consumption.

Over time DCAP will include a runtime process manager (Sapphire), a
reactive distributed memory manager (Diamond), a consistent
transactional storage system (TAPIR) and a privacy and security framework
(Agate). Initial focus is on making the runtime process manager
production-ready.

## Design Docs
<!--- TODO: Remove Huawei internal documents, edit for external visibility, and convert to MD  --->
* [User Experience Design](https://docs.google.com/document/d/1fJ8C-uQYdzOIDPSFRlD5QQ_QTJ_H3mO-RSl95hDuotQ/edit)
* [Sapphire Core Enhancement](https://docs.google.com/document/d/1aqJxQ9LqnWxo7vWU2cbCiCkrkn55g9I6Piogs6VTo_U/edit#heading=h.x7ziw49lzhk5)
* [Multi-Lang Design](https://docs.google.com/document/d/1WwmX7fuVr4AoRz0lgbAwv4nt4Jwc9I9WHP5dO07jBj8/edit)
* [Multi-DM Design](https://docs.google.com/document/d/1g5SnzsnyGXzdZVDF_uj9MQJomQpHS-PMpfwnYn4RNDU/edit#)
* [Code Offloading Design](https://docs.google.com/document/d/17umH9X61h8A6ckQ0LakGTiMD81P3Z7hZEm_UAAGW4B0/edit#heading=h.ftifncwym4cn)
* [Diamond Tapir Integration](https://docs.google.com/document/d/1JvIofXhEMqulPfb2BxfTtNgmvhmLTna2lJo0FmZIZeM/edit#heading=h.yprn9eci8t8e)

## Other Important Links
* [Getting started](docs/GettingStarted.md)
* [DCAP Planning](https://github.com/Huawei-PaaS/DCAP-Sapphire/wiki/DCAP-Planning)
* [Meetings](https://github.com/Huawei-PaaS/DCAP-Sapphire/wiki/Meetings)
* Huawei Repository: http://rnd-github-usa-g.huawei.com/SeattleCloudBU/DCAP-Sapphire

<!--- TODO: Add links to UW papers --->
<sup>1</sup> Link to paper<br/>
<sup>2</sup> Link to paper<br/>
<sup>3</sup> Link to paper<br/>
<sup>4</sup> Link to paper<br/>
<sup>5</sup> Link to paper<br/>
<sup>6</sup> Not all of these languages are currently officially supported, but they are all on our medium-term roadmap, support based on [GraalVM](http://www.graalvm.org/docs/)<br/>

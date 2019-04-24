# What is Amino.Run?

Amino.Run is an open source, multilanguage development platform and
distributed runtime environment designed to make distributed
applications much easier to design, develop and operate.  It is the
distributed process runtime component of the broader Amino distributed
operating system effort, which also includes subsystems for:

1. distributed, reactive memory (Amino.Sync)
2. distributed, transactional persistent store (Amino.Store)
3. distributed privacy and security (Amino.Safe)

Amin.Run supports most commonly used programming languages
and is, by default, deployed and managed in container environments
using [Kubernetes](https://www.k8s.io).  Common examples of
distributed applications include:

1. [mobile-cloud applications](https://www.techopedia.com/definition/26679/mobile-cloud-computing-mcc), 
2. [mobile backend style applications](https://en.wikipedia.org/wiki/Mobile_backend_as_a_service),
3. [edge computing](https://en.wikipedia.org/wiki/Edge_computing) applications and
4. other [cloud-native](https://github.com/cncf/toc/blob/master/DEFINITION.md) applications

All of these classes of applications share a common set of difficult
design and development challenges including performance, distributed
concurrency, remote invocation, synchronization, fault tolerance,
scalability, sharding, code and data migration, leader election, load
balancing, observability, fault diagnosis and many more.

Amino.Run is based on, and extends, several years of research work done at
the [University Of Washington Computer Systems
Lab](https://syslab.cs.washington.edu/research/) in
Seattle.<sup>1,2,3</sup> 

Amino.Run is alpha software, and not yet
suitable for production use.  We have a well-funded development team
actively working on getting it production ready, and actively
support contributions from the open source community.

# Why we created Amino.Run?

In a nutshell, we created Amino.Run to make design, development and operation of
reliable, fast, distributed applications quicker, easier and more fun.

In summary, our approach is to:

1. provide a wide and expandable range of standard, re-usable,
   pluggable and production-ready Deployment Managers
   (DMs)](DMList.md)
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
   Javascript, Python, C++, Swift, Ruby, Rust and others<sup>4</sup>-
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
   5. Even have Amino.Run move parts of your application around automatically at runtime
      to optimize performance, reliability or battery power consumption.

Over time Amino will include a runtime process manager (Amino.Run), a
reactive distributed memory manager (Amino.Sync), a consistent
transactional storage system (Amino.Sync) and a privacy and security framework
(Amino.Safe). Initial focus is on making Amino.Run production-ready.

# Documentation
* [Getting started](getting-started/getting-started.md)
* Contributing:
  * [Setting up your developer environment](developer.md)
  * [Contributing to the documentation](documentation.md)
* [External documentation web site](http://amino.readthedocs.io/en/latest)
<!--
TODO: Change the above to http://docs.amino-os.io as soon as that site and 
      redirection have been set up correctly.
-->

# References
<sup>1</sup> [Customizable and Extensible Deployment for Mobile/Cloud Applications
](https://syslab.cs.washington.edu/papers/sapphire-osdi14.pdf)<br/>
<sup>2</sup> [Diamond: Automating Data Management and Storage for Wide-area, Reactive Applications](https://syslab.cs.washington.edu/papers/diamond-osdi16.pdf)<br/>
<sup>3</sup> [Building Consistent Transactions with Inconsistent Replication (Extended Version)](https://syslab.cs.washington.edu/papers/tapir-tr-v2.pdf)<br/>
<sup>4</sup> Not all of these languages are currently officially supported, but they are all on our medium-term roadmap, support based on [GraalVM](http://www.graalvm.org/docs/)<br/>

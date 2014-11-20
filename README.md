Sapphire
========

## Organization

This directory contains all of the code for running Sapphire as a Java
library with your Android apps. We also include our example
applications, performance testing applications and scripts for
deploying applications across servers. 

- deployment: scripts for running Sapphire applications on
  servers. Starts the Sapphire servers for the "cloud side" and the
  OMS (called the OTS in the paper).
  
- example_apps: our to do list application and twitter-like
  application.

- generators: scripts for running our compiler for generating Sapphire
  object stubs and DM stubs.

- sapphire: the core sapphire library. It is deployed as an Android app.
  - src/sapphire/app: Application specific classes, like the starting point for bootstrapping a Sapphire app.
  - src/sapphire/common: Basic data structures.
  - src/sapphire/compiler: our compiler for generating Sapphire object and DM component stubs.
  - src/sapphire/dms: some example deployment managers
  - src/sapphire/kernel: the Sapphire kernel server that runs as a library on every node that runs a Sapphire app.
  - src/sapphire/oms: the Object Management/Tracking Service. (called the OTS in the paper).
  - src/sapphire/runtime: library functions for creating a Sapphire object (hack because we don't have sapphire keywords in the JVM).

- tests: performance testing apps. Good example simple example
  application with one Sapphire object.

## Setting up

1. Place your servers in deployment/servers.json.

2. Place the config for the app that you want to run in
deployment/app.py. This file needs a starting point for the
server-side and the client-side of your Sapphire app.

2. Run deploy.py to run the app

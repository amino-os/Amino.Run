Following are steps to deploy a Demo App named HanksTodo,

### Process Based Deployment

Clone DCAP-Sapphire-Core
```sh
git clone https://github.com/Huawei-PaaS/DCAP-Sapphire-Core.git
```
Move to Deployment Directory
```sh
cd DCAP-Sapphire-Core/deployment/linux
```
Execute following script to build and deploy OMS and Kernel Server as processes
```sh
./build-and-start-servers.sh hankstodo "https://kshafiee:a462a6da9e040f8ddabff9cfe44235a8f9df87c0@github.com/Huawei-PaaS/DCAP-Sapphire-Examples.git" DCAP-Sapphire-Examples 127.0.0.1 22346 22345
```
Execute following script to deploy application as a process
```sh
./start-application.sh hankstodo sapphire.appexamples.hankstodo.device.TodoActivity 127.0.0.1 22346 127.0.0.2 22345
```

### Container Based Deployment
Clone DCAP-Sapphire-Core
```sh
git clone https://github.com/Huawei-PaaS/DCAP-Sapphire-Core.git
```
Move to Deployment Directory
```sh
cd DCAP-Sapphire-Core/deployment/linux
```
Execute following script to build and deploy OMS and Kernel Server as containers
```sh
./build-and-start-servers.sh hankstodo "https://kshafiee:a462a6da9e040f8ddabff9cfe44235a8f9df87c0@github.com/Huawei-PaaS/DCAP-Sapphire-Examples.git" DCAP-Sapphire-Examples 172.31.44.202 22346 22345 container yes 172.31.44.202 172.31.44.203
```
Execute following script to deploy application as a container
```sh
./start-application.sh hankstodo sapphire.appexamples.hankstodo.device.TodoActivity container 172.31.44.202 22346 172.31.44.206 127.0.0.2 22345
```
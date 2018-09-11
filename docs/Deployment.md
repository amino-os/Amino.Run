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
Export ANDROID_HOME, path to where Android is installed on your machine
```sh
export ANDROID_HOME="/home/root1/Android/Sdk"
```
(Note: I am looking into ways to avoid this step/command from user. Gradle requires this parameter currently as there is single gradle file for assembing in the DCAP-Sapphire-Examples repo for both example-minnietwitter (android app) and hankstodo app (non-android app currently). And as example minnietwitter app requires this parameter to perform gradle assemble in scripts, once the gradle scripts are splitted, then this above command would not be required for java apps)

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
Export ANDROID_HOME, path to where Android is installed on your machine
```sh
export ANDROID_HOME="/home/root1/Android/Sdk"
```
(Note: Same note holds good here also, as mentioned above)

Execute following script to build and deploy OMS and Kernel Server as containers
```sh
./build-and-start-servers.sh hankstodo "https://kshafiee:a462a6da9e040f8ddabff9cfe44235a8f9df87c0@github.com/Huawei-PaaS/DCAP-Sapphire-Examples.git" DCAP-Sapphire-Examples 172.31.44.202 22346 22345 container yes 172.31.44.202 172.31.44.203
```
Execute following script to deploy application as a container
```sh
./start-application.sh hankstodo sapphire.appexamples.hankstodo.device.TodoActivity container 172.31.44.202 22346 172.31.44.206 127.0.0.2 22345
```

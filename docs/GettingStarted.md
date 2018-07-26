# Onboarding Process

* Send public GitHub account, Slack account and Huawei email address to Sungwook (sungwook.moon@huawei.com)
* Join [slack workspace](https://huawei.slack.com/)
* Get access to GitHub Repo by contacting Sungwook
* Read [papers](https://sapphire.cs.washington.edu/research/)
* Read [Sapphire source code](https://sapphire.cs.washington.edu/code.html)
* Read [code study notes](./docs/code_study/)
* Read [github workflow guide](https://github.com/kubernetes/community/blob/master/contributors/devel/development.md). Please follow this workflow to submit pull requests.
* Follows instructions in [this document](https://github.com/Huawei-PaaS/DCAP-Sapphire/blob/master/docs/Development.md) to set up environment
* Review the Principles of Distributed Systems. If you have not done a university course on distributed systems you will need to read the following to get a basic understanding of the principles and common terminology. Feel free to add more resource links below.
  * [UMass Course 677](http://lass.cs.umass.edu/~shenoy/courses/677/)
  * [Distributed System Principles by Andrew Tanenbaum](https://www.amazon.com/Distributed-Systems-Principles-Andrew-Tanenbaum/dp/153028175X)


# Quick Start

### Checking out Sapphire
```shell
> git clone https://github.com/Huawei-PaaS/DCAP-Sapphire
```
### List Sapphire Projects
```shell
> DCAP_Sapphire/sapphire
> ./gradlew projects
```

### Clean Sapphire
```shell
# clean sapphire-core and dependencies
> cd DCAP_Sapphire/sapphire
> ./gradlew clean

# clean only sapphire-core
> ./gradlew :sapphire-core:clean
```

### Format Source Code
```shell
# format source code
> cd DCAP_Sapphire/sapphire
> ./gradlew goJF

# verify source code style
> ./gradlew verGJF
```

### Build Sapphire
```shell
# build sapphire-core and dependencies
> cd DCAP_Sapphire/sapphire
> ./gradlew build

# build only sapphire-core
> ./gradlew :sapphire-core:build
```

### Publish Sapphire 
```shell
export BINTRAY_USER="<bintray_user>"
export BINTRAY_API_KEY="<bintray_api_key>"

# publish sapphire-core to bintray
> ./gradlew --info :sapphire-core:bintrayUpload

# publish apache harmony to bintray
> ./gradlew --info :apache.harmony:bintrayUpload
```

### Gradle Tips
```shell
> ./gradlew projects
> ./gradlew tasks --all
> ./gradlew properties
> ./gradlew jar
```


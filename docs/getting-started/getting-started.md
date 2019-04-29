# Quick Start
## Download and install GraalVM Community Edition
* You will need to download and install the corrrect version (usually the latest stable version) 
  based on the dependency configured in 
  [core/build.gradle](../../core/build.gradle). 
  As of October 2018, that's
  [GraalVM Community Edition 1.0 RC8](https://github.com/oracle/graal/releases/tag/vm-1.0.0-rc8).
  Note that the open source Community Edition works fine, so don't bother with the Enterprise Edition unless 
  you have a specific need for it.
  Follow instructions at  https://www.graalvm.org/docs/getting-started/ for downloading and installing.
  In particular, set your JAVA_HOME and PATH variables appropriately.  For example, something along the lines of the following at the end of your ~/.bash_profile in your home directory works well on Linux and Mac OS X:
```  
export GRAALVM_HOME=~/Downloads/graalvm-ce-1.0.0-rc8/Contents/Home
export JAVA_HOME=$GRAALVM_HOME
export PATH=$GRAALVM_HOME/bin:$PATH
```
  After then, you need to install ruby support for GraalVM:
```
gu install ruby
```

## Install Android SDK and Android Studio (optional)
* Android SDK and Android Studio are *not* required by Amino. But many Amino demo applications are android applications. We recommend installing Android SDK and Android Studio.
* Follow [instructions](https://developer.android.com/studio/) to install Android SDK and Android Studio. More details can be found at [here](https://wiki.appcelerator.org/display/guides2/Installing+the+Android+SDK#InstallingtheAndroidSDK-InstallingAndroidSDKToolsonmacOS).
```shell
// on Mac
$ brew cask install android-sdk
$ brew cask install android-ndk
```

### Accept Android SDK License
```shell
// on Mac
$ /usr/local/share/android-sdk/tools/bin/sdkmanager --licenses
```

### Add Android Properties
```shell
> cd Amino.Run/
> cat >> local.properties  << EOF
ndk.dir=<your ndk dir>
sdk.dir=<your sdk dir>
EOF
```

## Get and Build the Source Code

### Check out from Github
```shell
# checkout Amino.Run
$ git clone https://github.com/Amino-Distributed-OS/Amino.Run
> cd Amino.run/core
```

### Build and Test the Core
```shell
> ../gradlew build
```

### Build and Run Basic Example Applications
```shell
> ./gradlew :examples:run
```

### Other Gradle Tasks and Tips

#### List Projects
```shell
> ./gradlew projects
```
#### List All Gradle Tasks
```shell
> ./gradlew tasks --all
```

#### Clean All Build Artifacts
```shell
> ./gradlew clean

```
#### Format Source Code
```shell
> ./gradlew goJF

# verify source code style
> ./gradlew verGJF
```

#### Generate Policy Stub
```shell
> cd DCAP_Sapphire/sapphire/sapphire-core
> ../gradlew genStubs
```

### Other Gradle Tips
```shell
> ./gradlew properties
> ./gradlew jar
```

# Communicate

<!--
TODO: Create public slack channels, and allow self-signup.  In the mean time, Sungwook signs people up.
-->
* To join our [Slack](http://slack.com) channels, send your public GitHub account, Slack account name and email address to Sungwook Moon (sungwook.moon@huawei.com)

# Some additional Background Reading for the Curious

* Read [Papers](https://sapphire.cs.washington.edu/research/)
* Read [Sapphire source code](https://sapphire.cs.washington.edu/code.html)
* Read [Code Study Notes](./code_study/)
* Review the Principles of Distributed Systems. If you have not done a university course on distributed systems you will need to read the following to get a basic understanding of the principles and common terminology. Feel free to add more resource links below.
  * [UMass Course 677](http://lass.cs.umass.edu/~shenoy/courses/677/)
  * [Distributed System Principles by Andrew Tanenbaum](https://www.amazon.com/Distributed-Systems-Principles-Andrew-Tanenbaum/dp/153028175X)

## Releasing

### Publish Core to Bintray 
```shell
export BINTRAY_USER="<bintray_user>"
export BINTRAY_API_KEY="<bintray_api_key>"

> ./gradlew --info :core:bintrayUpload

# publish apache harmony to bintray
> ./gradlew --info :dependencies:apache.harmony:bintrayUpload
```



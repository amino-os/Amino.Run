# Onboarding Process

* Send public GitHub account, Slack account and Huawei email address to Sungwook (sungwook.moon@huawei.com)
* Join [slack workspace](https://huawei.slack.com/)
* Get access to GitHub Repo by contacting Sungwook
* Read [papers](https://sapphire.cs.washington.edu/research/)
* Read [Sapphire source code](https://sapphire.cs.washington.edu/code.html)
* Read [code study notes](./docs/code_study/)
* Read [github workflow guide](https://github.com/kubernetes/community/blob/master/contributors/devel/development.md). Please follow this workflow to submit pull requests.
* Follows instructions in [this document](https://github.com/Huawei-PaaS/DCAP-Sapphire/blob/master/docs/Development.md) to set up environment
* Review the Principles of Distributed Systems. Here are a couple of recommendations: 
  * [UMass Course 677](http://lass.cs.umass.edu/~shenoy/courses/677/)
  * [Distributed System Principles by Andrew Tanenbaum](https://www.amazon.com/Distributed-Systems-Principles-Andrew-Tanenbaum/dp/153028175X)


# Fast Start: build & push sapphire-core.jar to remote repo.

```
$ cd sapphire/
$ ./gradlew goJF # format java files
$ ./gradlew assemble
$ ./gradlew copyJar
$ ./gradlew genStubs
$ ./gradlew assemble
$ ./gradlew bintrayUpload
```


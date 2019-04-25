## Developer Environment Setup
### Development Workflow
* Amino follows the Kubernetes development workflow. Please read [Kubernetes Development Workflow Guide](https://github.com/kubernetes/community/blob/master/contributors/guide/github-workflow.md).
  Replace 'kubernetes/kubernetes' with 'amino-distributed-os/Amino.Run' and 'k8s.io' with 'amino-os.io' and you're pretty much good to go.

### Install Android Studio (optional)
* Download Gradle update if necessary (Android Studio will inform you).
* Install JDK (latest version) if you don't have one.
* Install Android Studio. Latest version or 3.0
* Install [Google Java Format plugin](https://plugins.jetbrains.com/plugin/8527-google-java-format) in Android Studio
  * Download [Google Java Format plugin](https://plugins.jetbrains.com/plugin/8527-google-java-format)
  * Follow [instructions](https://stackoverflow.com/questions/30617408/how-to-install-plugin-in-android-studio) to install plugin
  * Change code style to Android Open Source Project style in plugin (File &gt; OtherSettings &gt; DefaultSettings &gt; OtherSettings &gt; google-java-format settings).

### Check Out Code
* Fork your own repository from the [Amino.Run repository](https://github.com/amino-distributed-os/Amino.Run) 
* `git clone` from your own repository
* Make sure to sync with the latest source before creating a pull request to amino-distributed-os/Amino.Run.
* Make sure to rebase your code instead of simple git pull (read carefully about git pull part from above link).

### Open Project in Android Studio (optional).
* Open the Amino.Run project from the local repo you just cloned. (i.e open Amino.Run/)
* Android Studio will ask you about missing files - click OK.
* Sync Gradle. If it fails, just restart and try it again.
* Android Studio may show the bar for Gradle sync. If not: Tools &gt; Android &gt; Sync with Gradle files.
* Build project in Android Studio.

### Build with Android Studio or IntelliJ IDE (optional)
* Go to `File` -> `Settings`
* Inside `Settings` go to `Build, Execution, Deployment` -> `Build Tools` -> `Gradle` -> `Runner` 
* Check the box `Delegate IDE build/run actions to gradle` (if it is unchecked) and select `Platform Test Runner` in drop-down menu of `Run tests using:`
* Run the build gradle task from `Gradle tool Window` at `AminoRun` -> `AminoRun(root)` -> `Tasks` -> `build` -> `build`

### Build with Gradle
* cd Amino.Run/ && ./gradlew build

## Run Example Applications

* ./gradlew examples:run


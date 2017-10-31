mkdir out
javac -d out *.java
cd out
~/sapphire_code/android/out/host/linux-x86/bin/dx --dex --output=../hello_dex.jar example/hello/*.class

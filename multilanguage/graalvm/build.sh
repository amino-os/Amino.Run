
GRAALVM_HOME=/home/ackeri/Downloads/graalvm-ce-1.0.0-rc4/
PATH=$GRAALVM_HOME/bin:$PATH

echo "Building"
#greps pull out GC link order errors 
javac -cp "./*:." Generator.java -d classes | grep -v HotSpotJVMCIRuntime | grep -v createCompiler | grep -v "garbage collector is not" | grep -v "java:205"
echo "Executing"
java -cp "classes/*:classes/" -XX:+UseSerialGC Generator

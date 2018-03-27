    export PATH=$PATH:/usr1/golang/go-1.6.2/go/bin:/usr1/openjdk/jdk1.6.0_23/bin
    export LD_LIBRARY_PATH=/usr1/openjdk/jdk1.6.0_23/jre/lib/amd64/server:/usr1/llvm/lltap/LLTap/build/lib
    javac sapphire.java
    javac sapphireJni.java
    clang++ -S -emit-llvm -std=c++11 demo_app.cpp -o demo_app_cpp.bc
    opt -load /usr1/llvm/llvm-3.8.0/llvm-build/lib/libFuncHookPass.so -LLTapInst -S demo_app_cpp.bc -o demo_app_cpp_inst.bc
    clang++ -I/usr1/llvm/lltap/LLTap/include -I/usr1/openjdk/jdk1.6.0_23/include -I/usr1/openjdk/jdk1.6.0_23/include/linux -std=c++11 -S -emit-llvm sapphire_jni.cpp -o sapphire_jni.bc
    clang++ -L/usr1/openjdk/jdk1.6.0_23/jre/lib/amd64/server -L/usr1/llvm/lltap/LLTap/build/lib -L/usr1/llvm/llvm-3.8.0/llvm-install/lib demo_app_cpp_inst.bc sapphire_jni.bc -o HelloWorld -llltaprt -ljvm -lpthread
    ./HelloWorld

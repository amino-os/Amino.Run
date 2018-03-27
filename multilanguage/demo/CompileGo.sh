    export PATH=$PATH:/usr1/golang/go-1.6.2/go/bin:/usr1/openjdk/jdk1.6.0_23/bin
    export PATH=$PATH:/usr1/llvm/llvm-3.8.0/llvm-install/bin
    export LLVM_CONFIG=/usr1/llvm/llvm-3.8.0/llvm-install/bin/llvm-config
    export LD_LIBRARY_PATH=/usr1/openjdk/jdk1.6.0_23/jre/lib/amd64/server:/usr1/llvm/lltap/LLTap/build/lib
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr1/llvm/llvm-3.8.0/llvm-install/lib
    javac sapphire.java
    javac sapphireJni.java

    llgo -S -emit-llvm demo_app.go -o demo_app_golang.bc
    opt -load /usr1/llvm/llvm-3.8.0/llvm-build/lib/libFuncHookPass.so -LLTapInst -S demo_app_golang.bc -o demo_app_golang_inst.bc
    clang++ -I/usr1/llvm/lltap/LLTap/include -I/usr1/openjdk/jdk1.6.0_23/include -I/usr1/openjdk/jdk1.6.0_23/include/linux -std=c++11 -g3 -S -emit-llvm sapphire_jni.cpp -o sapphire_jni.bc
    clang++ -L/usr1/openjdk/jdk1.6.0_23/jre/lib/amd64/server -L/usr1/llvm/lltap/LLTap/build/lib -L/usr1/llvm/llvm-3.8.0/llvm-install/lib -g3 demo_app_golang_inst.bc sapphire_jni.bc -o HelloWorld -llltaprt -ljvm -lpthread -lgobegin-llgo -lgo-llgo
    ./HelloWorld


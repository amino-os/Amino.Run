The below steps need to be followed for the DCAP Multi Language support demo based on LLVM.

Points to note:-
---------------
1). Two versions of LLVM have been build and installed on this machine for the demo.
    LLVM version - 3.8.0
       Used for C, C++ and Golang demo.

    LLVM version - 3.3
       Used for Python demo as the PythonLLVM framework supports LLVM version 3.3

2). The LLVM version 3.8.0 is available in the below path.
    Source path:        /usr1/llvm/llvm-3.8.0/llvm-3.8.0.src
    Build path:         /usr1/llvm/llvm-3.8.0/llvm-build
    Install path:       /usr1/llvm/llvm-3.8.0/llvm-install

3). The LLVM version 3.3 is available in the below path.
    Source path:        /usr1/llvm/llvm-3.3/llvm-3.3.src
    Build path:         /usr1/llvm/llvm-3.3/llvm-3.3.src/Release+Asserts
    Install path:       /usr1/llvm/llvm-3.3/llvm-install
   
4). OpenJDK 1.6.0_23u installation
    /usr1/openjdk/jdk1.6.0_23

5). Golang version 1.6.2 installation
    /usr1/golang/go-1.6.2/go

5). LLTap framework used for hooking the functions is available in the below path
    /usr1/llvm/lltap/LLTap 


General steps:-
--------------
   The below environment variables need to be setup before building the demo files.

1). Add the Golang and Java binary paths in $PATH
    Already done in ~/.bashrc
    export PATH=$PATH:/usr1/golang/go-1.6.2/go/bin:/usr1/openjdk/jdk1.6.0_23/bin

2). Add the corresponding LLVM installation path in $PATH
    export PATH=$PATH:/usr1/llvm/llvm-3.8.0/llvm-install/bin     -- For Golang
    export PATH=$PATH:/usr1/llvm/llvm-3.3/llvm-install/bin       -- For Python

3). Export corresponding llvm-config binary path in $LLVM_CONFIG
    export LLVM_CONFIG=/usr1/llvm/llvm-3.8.0/llvm-install/bin/llvm-config     -- For Golang
    export LLVM_CONFIG=/usr1/llvm/llvm-3.3/llvm-install/bin/llvm-config       -- For Python

4). Export the library paths of OpenJdk and LLTap in $LD_LIBRARY_PATH
    export LD_LIBRARY_PATH=/usr1/openjdk/jdk1.6.0_23/jre/lib/amd64/server:/usr1/llvm/lltap/LLTap/build/lib

5). Add the corresponding LLVM library path in $LD_LIBRARY_PATH
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr1/llvm/llvm-3.8.0/llvm-install/lib     -- For Golang
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr1/llvm/llvm-3.3/llvm-install/lib       -- For Python

Build and Execution steps:-
--------------------------

  The demo code file are available in the below path.
  /usr1/llvm/dcap_demo/Mar27

1). Compile the demo Sapphire Java files
    javac sapphire.java
    javac sapphireJni.java

2). C++ demo_application and Sapphire Java

    a). clang++ -S -emit-llvm -std=c++11 demo_app.cpp -o demo_app_cpp.bc
    b). opt -load /usr1/llvm/llvm-3.8.0/llvm-build/lib/libFuncHookPass.so -LLTapInst -S demo_app_cpp.bc -o demo_app_cpp_inst.bc
    c). clang++ -I/usr1/llvm/lltap/LLTap/include -I/usr1/openjdk/jdk1.6.0_23/include -I/usr1/openjdk/jdk1.6.0_23/include/linux -std=c++11 -S -emit-llvm sapphire_jni.cpp -o sapphire_jni.bc
    d). clang++ -L/usr1/openjdk/jdk1.6.0_23/jre/lib/amd64/server -L/usr1/llvm/lltap/LLTap/build/lib -L/usr1/llvm/llvm-3.8.0/llvm-install/lib demo_app_cpp_inst.bc sapphire_jni.bc -o HelloWorld -llltaprt -ljvm -lpthread
    e). ./HelloWorld

3). Golang demo_application and Sapphire Java

    a). llgo -S -emit-llvm demo_app.go -o demo_app_golang.bc
    b). opt -load /usr1/llvm/llvm-3.8.0/llvm-build/lib/libFuncHookPass.so -LLTapInst -S demo_app_golang.bc -o demo_app_golang_inst.bc
    c). clang++ -I/usr1/llvm/lltap/LLTap/include -I/usr1/openjdk/jdk1.6.0_23/include -I/usr1/openjdk/jdk1.6.0_23/include/linux -std=c++11 -g3 -S -emit-llvm sapphire_jni.cpp -o sapphire_jni.bc
    d). clang++ -L/usr1/openjdk/jdk1.6.0_23/jre/lib/amd64/server -L/usr1/llvm/lltap/LLTap/build/lib -L/usr1/llvm/llvm-3.8.0/llvm-install/lib -g3 demo_app_golang_inst.bc sapphire_jni.bc -o HelloWorld -llltaprt -ljvm -lpthread -lgobegin-llgo -lgo-llgo
    e). ./HelloWorld

4). Python demo_application and Sapphire Java
    a). python /home/root1/Downloads/PythonLLVM_workingwithintprintStr/pyllvm.py demo_app.py
    b). opt  -S -load /usr1/llvm/llvm-3.3/llvm-install/lib/LLVMMyHook.so -LLTapInst -S demo_app_python.bc -o demo_app_python_inst.bc
    c). clang++ -I/usr1/llvm/lltap/LLTap/include -I/usr1/openjdk/jdk1.6.0_23/include -I/usr1/openjdk/jdk1.6.0_23/include/linux -std=c++11 -S -emit-llvm sapphire_jni.cpp -o sapphire_jni.bc
    d). clang++ -L/usr1/openjdk/jdk1.6.0_23/jre/lib/amd64/server -L/usr1/llvm/lltap/LLTap/build/lib -L/usr1/llvm/llvm-3.8.0/llvm-install/lib  demo_app_python_inst.bc sapphire_jni.bc -o HelloWorld -llltaprt -ljvm -lpthread 
    e). ./HelloWorld



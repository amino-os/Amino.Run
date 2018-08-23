
1) from the app code need to generate the .proto files using some tools(currently its manually written)
from the .proto files generate the grpc stubs (Currentlu Proto file and stub are generated manualy)

 Just clone and place the sapphire_grpc and  hankstodo folders in GOPATH/src folder

2) Generate the hankstodo shared lib as shown below  
  root1@lenovo:~/Work/src/hankstodo/app$ go build -buildmode=plugin -o ../../sapphire_grpc/sapphire_go_runtime/sapphire_objects/hankstodo.so

3) Build  the runtime  process as mentioned below 
  
   root1@lenovo:~/Work/src/sapphire_grpc/sapphire_go_runtime$ go build 


4) Build the hankstodo_create as mentioned below 
    root1@lenovo:~/Work/src/hankstodo/hankstodo_create$ go build 
  
5) Build the hankstodo_attach as mentioned below 
   
    root1@lenovo:~/Work/src/hankstodo/hankstodo_attach$ go build 

6) Run the runtime process with the  below command line arguments

   gRPC-server-ip=  Runtime gRPC listening  ip
   gRPC-server-port= Runtime gRPC listening port 
   kernelserver-ip= Kernal Server gRPC listening ip
   kernelserver-port= Kernal Server gRPC listening port 
   SharedLibsPath= relative or absolute path of shared lib

  root1@lenovo:~/Work/src/sapphire_grpc/sapphire_go_runtime$./sapphire_go_runtime -gRPC-server-ip 127.0.0.1 -gRPC-server-port  20005 -kernelserver-ip    127.0.0.1 -kernelserver-port  20001 -SharedLibsPath sapphire_objects/

7) Run the hankstodo_create process with the  below command line arguments
 
   RMI-ip= Kernel client RMI ip
   RMI-por= Kernel client RMI port 
   kernelclient-ip= Kernel client gRPC listening ip
   kernelclient-port= Kernel client gRPC listening ip
   
   root1@lenovo:~/Work/src/hankstodo/hankstodo_create$./hankstodo_create -RMI-ip 127.0.0.1 -RMI-port 10003 -kernelclient-ip 127.0.0.1 -kernelclient-port 20003 

8) Run the hankstodo_attach  process with the  below command line arguments
   
   RMI-ip=  Kernel client RMI ip
   RMI-port= Kernel client RMI port 
   kernelclient-ip= Kernel client gRPC listening ip
   kernelclient-port= Kernel client gRPC listening ip
   
   root1@lenovo:~/Work/src/hankstodo/hankstodo_attach$./hankstodo_attach -RMI-ip 127.0.0.1 -RMI-port 10003 -kernelclient-ip 127.0.0.1 -kernelclient-port  20003

 Todo : Runtime Sdk needs to seperated from sapphire_go_runtime
      


package main

import (
	"flag"
	"fmt"
	grpcStub "hankstodo/hankstodo_create/grpc_stub"
	"hankstodo/hankstodo_create/util"
	"log"
)

var (
	RMIIP   = flag.String("RMI-ip", "127.0.0.1", "Kernel client RMI ip")
	RMIPORT = flag.String("RMI-port", "10003", "Kernel client RMI port")
	KCIP    = flag.String("kernelclient-ip", "127.0.0.1", "Kernel client gRPC listening ip")
	KCPORT  = flag.String("kernelclient-port", "20003", "Kernal client gRPC listening  port")
)

func main() {
	flag.Parse()
	err := util.Init(*KCIP, *KCPORT, *RMIIP, *RMIPORT)
	if err != nil {
		return
	}
	log.Println("Grpc client implemtion")
	defer util.GrpcConn.Close()
	// Create the Stub Object
	todoManagerStub, err := grpcStub.NewTodoListManager("go")

	// Set url if object  is created by the golang
	if err != nil {
		fmt.Println("Creating Object for runtime type go has been failed")
		return
	}
	status, err := todoManagerStub.SetURL("Hankstodo.GoRuntime")
	if !status || err != nil {
		log.Println("Set url failed")
		return
	}
	//Creating the TodoList object
	todoListStub := todoManagerStub.NewTOdo("GoHanks")
	fmt.Println(todoListStub.AddToDO("first"))

	fmt.Println(todoListStub.AddToDO("second"))

	fmt.Println(todoListStub.AddToDO("Third"))
}

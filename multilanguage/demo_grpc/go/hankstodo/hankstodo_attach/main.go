package main

import (
	"flag"
	"fmt"
	grpcStub "hankstodo/hankstodo_attach/grpc_stub"
	"hankstodo/hankstodo_attach/util"
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
	log.Println("Grpc Attach client implemtion")
	defer util.GrpcConn.Close()
	todoManagerStub, err := grpcStub.NewTodoListManager("Hankstodo.JavaRuntime")
	if err != nil {
		fmt.Println(err)
		return
	}
	todoListStub := todoManagerStub.NewTOdo("GoHanks")
	fmt.Println(todoListStub.AddToDO("first"))

	fmt.Println(todoListStub.AddToDO("second"))

	fmt.Println(todoListStub.AddToDO("Third"))
}

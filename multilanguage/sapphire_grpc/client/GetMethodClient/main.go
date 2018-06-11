package main

import (
	"fmt"
	"sapphire_grpc/api"

	"github.com/golang/protobuf/proto"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
)

func main() {
	//Establishing connection via gRPC
	conn, err := grpc.Dial(":7000", grpc.WithInsecure())
	if err != nil {
		fmt.Println("Unable to establish the connection")
	}
	defer conn.Close()
	//Creating the gRPC client object
	c := api.NewMgmtgrpcServiceClient(conn)
	var request api.GenericMethodRequest
	request.FuncName = "HelloWorld"
	request.ObjId = "de0e5bc5-c92c-4916-b905-dfa9063d8b91"
	request.SapphireObjName = "New"
	
	request.Params, _ = proto.Marshal(&api.HelloWorldRequest{Num: 10, Name: "Test String"})

	response, err := c.GenericMethodInvoke(context.Background(), &request)
	if err != nil {
		fmt.Println("Unable to Recive the mesage")
		return
	}
	var methodResponse api.HelloWorldReply

	proto.Unmarshal(response.Ret, &methodResponse)

	fmt.Println("Result", methodResponse.RetName,methodResponse.Retval)
}

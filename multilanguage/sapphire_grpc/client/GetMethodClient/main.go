package main

import (
	"fmt"
	"sapphire_grpc/api"
	"sapphire_grpc/api/greeting"
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
	request.ObjId = "83a85426-9cef-40f2-a60f-6dd0c0862c07"
	request.SapphireObjName = "SapphireObj_Greeting"
	
	request.Params, _ = proto.Marshal(&greeting.HelloWorldRequest{Num: 10, Name: "Test String"})

	response, err := c.GenericMethodInvoke(context.Background(), &request)
	if err != nil {
		fmt.Println("Unable to Recive the mesage",err)
		return
	}

        fmt.Println("Result",response)
	var methodResponse greeting.HelloWorldReply

	proto.Unmarshal(response.Ret, &methodResponse)

	fmt.Println("Result", methodResponse.RetName,methodResponse.Retval)
}

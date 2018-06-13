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
	request.FuncName = "Fibbonaci"
	paramData, _ := proto.Marshal(&api.FibbonaciRequest{Num: 10})
	request.Params = paramData

	request.ObjId = "5cb98249-5af7-48a2-b72c-178096f8c697"
	request.SapphireObjName = "New"
	response, err := c.GenericMethodInvoke(context.Background(), &request)
	if err != nil {
		fmt.Println("Unable to Recive the mesage")
		return
	}
	var methodResponse api.FibbonaciReply

	proto.Unmarshal(response.Ret, &methodResponse)

	fmt.Println("Result", methodResponse.Ret)
}

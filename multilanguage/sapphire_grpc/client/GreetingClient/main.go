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

	var uuid string

	//Establishing connection via gRPC
	conn, err := grpc.Dial(":7000", grpc.WithInsecure())
	if err != nil {
		fmt.Println("Unable to establish the connection")
	}
	defer conn.Close()
	//Creating the gRPC client object
	c := api.NewMgmtgrpcServiceClient(conn)

	fmt.Println("CreateSapphireObject Request to SapphireObj_Greeting")
	response, err := c.CreateSapphireObject(context.Background(), &api.CreateRequest{Name: "SapphireObj_Greeting"})
	if err != nil {
		fmt.Println(err)
		fmt.Println("CreateSapphireObject Received error:",err)
		return
	}
	fmt.Println("CreateSapphireObject Response Message is ", response.ObjId)
	uuid = response.ObjId

	var reqHelloWorld api.GenericMethodRequest
	reqHelloWorld.FuncName = "HelloWorld"
	reqHelloWorld.ObjId = uuid
	reqHelloWorld.SapphireObjName = "SapphireObj_Greeting"

	reqHelloWorld.Params, _ = proto.Marshal(&greeting.HelloWorldRequest{Num: 10, Name: "Test String"})

	fmt.Println("GenericMethodInvoke for HelloWorld Method to SapphireObj_Greeting with UUID",uuid)

	resHelloWorldReply, err := c.GenericMethodInvoke(context.Background(), &reqHelloWorld)
	if err != nil {
		fmt.Println("GenericMethodInvoke for HelloWorld Recive the error",err)
		return
	}
	var methodResponse greeting.HelloWorldReply

	proto.Unmarshal(resHelloWorldReply.Ret, &methodResponse)

	fmt.Println("GenericMethodInvoke for HelloWorld Method Result", methodResponse.RetName,methodResponse.Retval)


	var reqFibbonaci api.GenericMethodRequest
	reqFibbonaci.FuncName = "Fibbonaci"
	paramData, _ := proto.Marshal(&greeting.FibbonaciRequest{Num: 10})
	reqFibbonaci.Params = paramData

	reqFibbonaci.ObjId = uuid
	reqFibbonaci.SapphireObjName = "SapphireObj_Greeting"
	fmt.Println("GenericMethodInvoke for Fibbonaci Method to SapphireObj_Greeting with UUID",uuid)
	resFibbonaci, err := c.GenericMethodInvoke(context.Background(), &reqFibbonaci)
	if err != nil {
		fmt.Println("GenericMethodInvoke for Fibbonaci Recived error", err)
		return
	}
	var fibResponse greeting.FibbonaciReply

	proto.Unmarshal(resFibbonaci.Ret, &fibResponse)

	fmt.Println("GenericMethodInvoke Fibbonaci for Result", fibResponse.Ret)

	fmt.Println("DeleteSapphireObject  SapphireObj_Greeting with UUID",uuid)
	resDel, err := c.DeleteSapphireObject(context.Background(), &api.DeleteRequest{ObjId: uuid})
	if err != nil {
		fmt.Println(err)
		fmt.Println("DeleteSapphireObject failed",err)
		return
	}
	fmt.Println("Response Message is ", resDel.Flag)
	return

}

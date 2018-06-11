package main

import (
	"fmt"
	"sapphire_grpc/api"

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
	response, err := c.CreateSapphireObject(context.Background(), &api.CreateRequest{Name: "New"})
	if err != nil {
		fmt.Println(err)
		fmt.Println("Unable to Recive the mesage")
		return
	}
	fmt.Println("Response Message is ", response.ObjId)
}

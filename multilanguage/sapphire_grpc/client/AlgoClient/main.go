package main

import (
	"errors"
	"fmt"
	"sapphire_grpc/api"
	"sapphire_grpc/api/algo"

	"github.com/golang/protobuf/proto"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
)

const (
	objName = "SapphireObj_Algo"
)

func main() {

	conn, err := grpc.Dial(":7000", grpc.WithInsecure())
	if err != nil {
		fmt.Println("Unable to establish the connection")
	}
	defer conn.Close()
	//Creating the gRPC client object
	c := api.NewMgmtgrpcServiceClient(conn)
	objId, err := createObject(c)
	if err != nil {
		fmt.Println("Unable to Create the  object return")
		return
	}
	fmt.Println("ObjectId:", objId)
	err = GenricService(c, objId)
	if err != nil {
		return
	}
	err = DeleteObject(c, objId)
	if err != nil {
		return
	}
}
func createObject(c api.MgmtgrpcServiceClient) (string, error) {
	response, err := c.CreateSapphireObject(context.Background(), &api.CreateRequest{Name: objName})
	if err != nil {
		fmt.Println(err)
		fmt.Println("Unable to Recive the mesage")
		return "", err
	}
	return response.ObjId, nil
}
func GenricService(c api.MgmtgrpcServiceClient, objId string) error {
	var request api.GenericMethodRequest
	request.FuncName = "Sort"
	arr := make([]int32, 0)
	arr = append(arr, 10)
	arr = append(arr, 6)
	arr = append(arr, 5)
	arr = append(arr, 20)
	arr = append(arr, 1)
	paramData, _ := proto.Marshal(&algo.SortRequest{Arr: arr})
	request.Params = paramData

	request.ObjId = objId
	request.SapphireObjName = objName
	response, err := c.GenericMethodInvoke(context.Background(), &request)
	if err != nil {
		fmt.Println("Unable to execute the generic Method")
		return err
	}
	var methodResponse algo.SortReplay
	proto.Unmarshal(response.Ret, &methodResponse)
	fmt.Println("Result", methodResponse.Arr)

	return nil
}
func DeleteObject(c api.MgmtgrpcServiceClient, objId string) error {
	response, err := c.DeleteSapphireObject(context.Background(), &api.DeleteRequest{ObjId: objId})
	if err != nil {
		fmt.Println(err)
		fmt.Println("Unable to Recive the mesage")
		return err
	}
	if !response.Flag {
		fmt.Println("error in deletion")
		return errors.New("ErrorInDeletion")
	}
	fmt.Println("Response Message is ", response.Flag)
	return nil
}

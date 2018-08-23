package grpc_stub

import (
	"fmt"
	app "hankstodo/app_interface"
	"hankstodo/hankstodo_attach/util"
	grpcInterface "sapphire_grpc/sapphire_interface/sapphire_kernel_app"

	"log"

	"github.com/golang/protobuf/proto"

	"golang.org/x/net/context"
)

func Attach(url string) (TodoListManagerStub, error) {

	c := grpcInterface.NewAppServiceClient(util.GrpcConn)
	res, err := c.Attach(context.Background(), &grpcInterface.AttachRequest{Url: url, DmClientRmiEndPoint: util.RMICONFIG})
	if err != nil {
		fmt.Println(err)
		return TodoListManagerStub{}, err
	}
	fmt.Println(res.ClientId, res.SId)
	var stub TodoListManagerStub
	stub.ClientId = res.ClientId
	stub.Sid = res.SId
	var todolistManger app.TodoListManager
	err = proto.Unmarshal(res.ObjectStream, &todolistManger)
	if err != nil {
		return stub, err
	}
	stub.Object = todolistManger
	return stub, nil
}
func GenericInvoke(dmClientId string, funcName string, funcParam []byte) ([]byte, error) {
	c := grpcInterface.NewAppServiceClient(util.GrpcConn)
	var genericInvokeRequest grpcInterface.InvokeRequest
	genericInvokeRequest.DMClientId = dmClientId
	genericInvokeRequest.FuncName = funcName
	genericInvokeRequest.FuncParams = funcParam
	res, err := c.GenericInvoke(context.Background(), &genericInvokeRequest)
	if err != nil {
		log.Println("error in genric invoke", err)
		return make([]byte, 0), err
	}
	return res.ObjectStream, nil
}
func Acquire(sid string) (string, []byte, error) {
	c := grpcInterface.NewAppServiceClient(util.GrpcConn)
	var acquireInvokeRequest grpcInterface.AcquireRequest
	acquireInvokeRequest.SId = sid
	acquireInvokeRequest.DmClientRmiEndPoint = util.RMICONFIG
	res, err := c.AcquireAppStub(context.Background(), &acquireInvokeRequest)
	if err != nil {
		log.Println("Acquire of object failed", err)
		return "", make([]byte, 0), err
	}
	return res.ClientId, res.ObjectStream, nil
}

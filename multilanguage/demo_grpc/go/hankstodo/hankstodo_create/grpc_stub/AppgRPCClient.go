package grpc_stub

import (
	"hankstodo/hankstodo_create/util"
	grpcInterface "sapphire_grpc/sapphire_interface/sapphire_kernel_app"

	"log"

	"golang.org/x/net/context"
)

func CreateSapphireObject(sobjName, constructName, langType string, constructParam []byte) (string, error) {
	c := grpcInterface.NewAppServiceClient(util.GrpcConn)
	var createRequest grpcInterface.CreateRequest
	createRequest.LangType = langType
	createRequest.ConstructName = constructName
	createRequest.ConstructParams = constructParam
	createRequest.SoName = sobjName
	createResponse, err := c.CreateSapphireObject(context.Background(), &createRequest)
	if err != nil {
		log.Println("Error During the object creation ", err)
		return "", err
	}
	return createResponse.SId, nil

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

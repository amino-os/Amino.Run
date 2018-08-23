package runtime_sdk

import (
	"errors"
	"fmt"
	api "sapphire_grpc/sapphire_interface/kernalserver_to_apiruntime"

	"sapphire_grpc/sapphire_go_runtime/common"

	"golang.org/x/net/context"
)

func CreateObjectStub(plugName, objectName, parentId string, replicaObject []byte) ([]byte, interface{}, error) {
	dminfo := api.DMInfo{ClientPolicy: "", ServerPolicy: "", GroupPolicy: ""}
	c := api.NewRuntimeServiceClient(common.KSGRPC)
	var createRequest api.CreateChildSObjRequest
	createRequest.SObjDMInfo = &dminfo
	createRequest.SObjParentSObjId = parentId
	createRequest.SObjName = objectName + ":go"
	createRequest.SObjReplicaObject = replicaObject

	createRet, err := c.CreateChildSObj(context.Background(), &createRequest)
	if err != nil {
		fmt.Println("Error during child object creation")
		return nil, nil, err
	}
	objectPlug, _ := common.Plug.GetPlug(plugName)

	symGreeter, err := objectPlug.Lookup("New_" + objectName)
	if err != nil {
		fmt.Println("Error in plug.Lookup for GenericCreate")
		fmt.Println(err)
		return nil, "", err
	}
	method, ok := symGreeter.(func(string, string, string) ([]byte, interface{}))

	if !ok {
		err = errors.New("New_" + objectName + "GenericCreate function is not there in dynamic lib")
		fmt.Println("Error in symGreeter.(func(" +
			"string, string, string" +
			") ([]byte,interface{})")
		return nil, nil, err
	}

	retData, object := method(parentId, createRet.SObjId, createRet.SObjReplicaId)
	common.ObjectInfo.ADDObject(createRet.SObjId, createRet.SObjReplicaId, parentId, object)
	return retData, object, nil
}

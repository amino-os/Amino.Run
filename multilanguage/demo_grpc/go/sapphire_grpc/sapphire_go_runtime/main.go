package main

import (
	"errors"
	"flag"
	"fmt"
	"log"
	"net"
	"os"
	"plugin"
	"reflect"
	"sapphire_grpc/sapphire_go_runtime/common"
	api "sapphire_grpc/sapphire_interface/runtimeapi_to_kernelserver"

	"golang.org/x/net/context"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

var (
	GRPCSERVERIP   = flag.String("gRPC-server-ip", "127.0.0.1", "Runtime gRPC listening  ip")
	GRPCSERVERPORT = flag.String("gRPC-server-port", "20005", "Runtime gRPC listening port")
	KSSERVERIP     = flag.String("kernelserver-ip", "127.0.0.1", "Kernal Server gRPC listening ip")
	KSPORT         = flag.String("kernelserver-port", "20001", "Kernal Server gRPC listening port")
	SHARELIBPATH   = flag.String("SharedLibsPath", "sapphire_objects/", "relative or absolute path of shared lib")
)

//PlugInName & PlugIn Map

type Server struct{}

// Create the Sapphire Object and Load the Dynamic lib First time
func (s *Server) CreateSObjReplica(c context.Context, in *api.CreateSObjReplicaRequest) (*api.CreateSObjReplicaResponse, error) {

	var err error
	sapphireObjName := in.SObjName
	fmt.Println("sapphireObjName:", sapphireObjName)

	objectPlug, ok := common.Plug.GetPlug(sapphireObjName)

	//  If plugin is not found in Map then Dynamic Library will loaded
	if !ok {

		sharedlibPath := fmt.Sprintf("%s%s.so", *SHARELIBPATH, sapphireObjName)

		_, err = os.Stat(sharedlibPath)

		if err != nil {
			fmt.Println("Shared library is not there is the path", sharedlibPath)
			return nil, status.Errorf(codes.InvalidArgument, err.Error())
		}
		objectPlug, err = plugin.Open(sharedlibPath)
		if err != nil {
			fmt.Println("plugin.Open Failed", err)
			return nil, status.Errorf(codes.Internal, err.Error())

		}

		common.Plug.AddPlug(sapphireObjName, objectPlug)
	}
	//  Checking the Construct method for the Struct
	symGreeter, err := objectPlug.Lookup("New_" + in.SObjConstructorName)
	if err != nil {
		fmt.Println("Error in plug.Lookup for GenericCreate")
		fmt.Println(err)
		return nil, status.Errorf(codes.Internal, err.Error())
	}
	//Creating the Object
	method, ok := symGreeter.(func(string, string, string) ([]byte, interface{}))

	if !ok {
		err = errors.New("New_" + in.SObjConstructorName + "function is not there in dynamic lib")
		fmt.Println("Error in symGreeter.(func(" +
			"string, string, string)" +
			" ([]byte,interface{})")
		return nil, status.Errorf(codes.Internal, err.Error())
	}

	retData, object := method(in.SObjParentSObjId, in.SObjId, in.SObjReplicaId)

	//store the Sapphire Object in the map
	common.ObjectInfo.ADDObject(in.SObjId, in.SObjReplicaId, in.SObjParentSObjId, object)
	fmt.Println("Successfully created the Sapphire Object with replica id :", in.SObjReplicaId, " Objectid:", in.SObjId)
	dmInfo := api.DMInfo{ClientPolicy: "", ServerPolicy: "", GroupPolicy: ""}
	var apiResponse api.CreateSObjReplicaResponse
	apiResponse.SObjReplicaObject = retData
	apiResponse.SObjDMInfo = &dmInfo
	return &apiResponse, nil
}

// Delete the Sapphire Object

func (s *Server) DeleteSObjReplica(c context.Context, in *api.DeleteSObjReplicaRequest) (*api.DeleteSObjReplicaResponse, error) {
	fmt.Println("Request Params ", in.SObjId, in.SObjReplicaId)
	flag := common.ObjectInfo.DeleteObject(in.SObjId, in.SObjReplicaId)

	return &api.DeleteSObjReplicaResponse{Status: flag}, nil
}

func (s *Server) SObjMethodInvoke(c context.Context, in *api.SObjMethodInvokeRequest) (*api.SObjMethodInvokeResponse, error) {

	fmt.Println("Request Params ", in.SObjId, in.SObjReplicaId)
	object, err := common.ObjectInfo.GetObject(in.SObjId, in.SObjReplicaId)

	if err != nil {
		err := errors.New("SapphireObject ID or Replica ID is Invalid")
		fmt.Println(err)
		return nil, status.Errorf(codes.InvalidArgument, err.Error())
	}

	actualMethod := reflect.ValueOf(object).MethodByName(in.SObjMethodName)

	if !actualMethod.IsValid() {
		err := errors.New("Method Name is Inavlid")
		fmt.Println("Method Name is Inavlid :", actualMethod)
		return nil, status.Errorf(codes.InvalidArgument, err.Error())
	}

	objects := make(map[reflect.Type]interface{})
	reqin := make([]reflect.Value, actualMethod.Type().NumIn())

	paramtype := actualMethod.Type().In(0).Elem()
	objects[paramtype] = in.SObjMethodParams
	reqin[0] = reflect.ValueOf(objects[paramtype])

	methodReslut := actualMethod.Call(reqin)
	if errValue := methodReslut[1].Interface(); errValue != nil {
		err = errValue.(error)
		log.Println(err)
		return nil, status.Errorf(codes.Internal, err.Error())
	}
	ret := methodReslut[0].Interface().([]byte)
	return &api.SObjMethodInvokeResponse{SObjMethodResponse: ret}, nil
}
func main() {

	//currently its hard coded either we can take from config or input parameters
	flag.Parse()
	common.Init(*KSSERVERIP, *KSPORT)
	defer common.KSGRPC.Close()
	lis, err := net.Listen("tcp", *GRPCSERVERIP+":"+*GRPCSERVERPORT)
	if err != nil {
		fmt.Println(err)
		fmt.Println("Unable to listen to port ", *GRPCSERVERIP+":"+*GRPCSERVERPORT)
		return
	}
	log.Println("Server Started Listening on port :", *GRPCSERVERPORT)
	s := grpc.NewServer()
	api.RegisterKernelServiceServer(s, &Server{})
	s.Serve(lis)
	defer lis.Close()

}

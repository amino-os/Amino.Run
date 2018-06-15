package main

import (
	"crypto/rand"
	"errors"
	"reflect"
	"fmt"
	"io"
	"os"
	"net"
	"plugin"
	api "sapphire_grpc/api"

	"golang.org/x/net/context"

	"google.golang.org/grpc"
)

//==============================Start UUID related Code ====================================================

type UUID []byte

var rander = rand.Reader

func (uuid UUID) String() string {
	if uuid == nil || len(uuid) != 16 {
		return ""
	}
	b := []byte(uuid)

	return fmt.Sprintf("%08x-%04x-%04x-%04x-%012x", b[:4], b[4:6], b[6:8], b[8:10], b[10:])

}

func randomBits(b []byte) {
	if _, err := io.ReadFull(rander, b); err != nil {
		fmt.Println("Error in randomBits func", err)
		panic(err.Error())
	}
}

func NewRandom() UUID {

	uuid := make([]byte, 16)
	randomBits(uuid)

	uuid[6] = (uuid[6] & 0x0f) | 0x40 //version 4
	uuid[8] = (uuid[8] & 0x3f) | 0x80 //version 10
	return uuid
}

func GetRandomString() string {

	return NewRandom().String()

}

//==============================End UUID related Code ====================================================

type PlugInInfo struct {
	ObjectAddress *plugin.Plugin
}

var SharedLibsPath string
//PlugInName & PlugIn Map
var SapphireNameMap map[string]PlugInInfo

//UUID & SapphireObjectMap
var SapphireIDMap map[string]interface{}

type Server struct{}

// Create the Sapphire Object and Load the Dynamic lib First time
func (s *Server) CreateSapphireObject(c context.Context, in *api.CreateRequest) (*api.CreateReply, error) {

	var plug *plugin.Plugin
	var err error
	sapphireObjName := in.GetName()

	fmt.Println("sapphireObjName:", sapphireObjName)

	plugInfo, ok := SapphireNameMap[sapphireObjName]

	//if the plug in is not loaded then load it other wise just create the UUID &Create the SapphireObject
	if !ok {

		sharedlibPath := fmt.Sprintf("%s%s.so",SharedLibsPath,sapphireObjName)

		_,err = os.Stat(sharedlibPath)

		if err != nil {
			fmt.Println("Shared library is not there is the path", sharedlibPath)
			return nil, err
		}
		plug, err = plugin.Open(sharedlibPath)
		if err != nil {
			fmt.Println("plugin.Open Failed",err)
			return nil, err
		}

		SapphireNameMap[sapphireObjName] = PlugInInfo{ObjectAddress: plug}
		fmt.Println("Successfully Loaded  the Dynamic lib", SapphireNameMap)
	} else {
		plug = plugInfo.ObjectAddress
	}

	uuid := GetRandomString()

	symGreeter, err := plug.Lookup("GenericCreate")
	if err != nil {
		fmt.Println("Error in plug.Lookup for GenericCreate")
		fmt.Println(err)
		return nil, err
	}
	method, ok := symGreeter.(func() interface{})

	if !ok {
		err = errors.New("GenericCreate function is not there in dynamic lib")
		fmt.Println("Error in symGreeter.(func(string) interface{} )")
		return nil, err
	}

	output := method()

	//store the Sapphire Object in the map
	SapphireIDMap[uuid] = output
	fmt.Println("Successfully created the Sapphire Object", SapphireIDMap)
	return &api.CreateReply{ObjId: uuid}, nil
}

// Delete the Sapphire Object
func (s *Server) DeleteSapphireObject(c context.Context, in *api.DeleteRequest) (*api.DeleteReply, error) {

	_, flag := SapphireIDMap[in.ObjId]

	if ! flag  {
		err := errors.New("SapphireObject ID is Invalid")
		fmt.Println("SapphireObject ID is Invalid")
		return &api.DeleteReply{Flag: false},err
	}

	delete(SapphireIDMap, in.ObjId)
	fmt.Println("Successfully&api.DeleteReply{Flag: true} DeleteSapphireObject the Sapphire Object", SapphireIDMap)
	return &api.DeleteReply{Flag: true}, nil
}
func (s *Server) GenericMethodInvoke(c context.Context, in *api.GenericMethodRequest) (*api.GenericMethodReply, error) {

	_, ok := SapphireNameMap[in.SapphireObjName]

	if ! ok  {
		err := errors.New("SapphireObject Name is wrong")
		fmt.Println("SapphireObject Name is wrong")
		return nil,err
	}
    /* GenericSOMethodInvoke  is not required as part of go lang Sapphire Object Shared lib
	symGreeter1, err1 := plugInfo.ObjectAddress.Lookup("GenericSOMethodInvoke")
	if err1 != nil {
		fmt.Println("Error in plug.Lookup for GenericSOMethodInvoke")
		fmt.Println(err1)
		return nil, err1
	}
	method, ok := symGreeter1.(func(interface{}, interface{}) []byte)

	if !ok {
		err := errors.New("GenericCreate function is not there in dynamic lib")
		fmt.Println("Error in symGreeter.(func(string) interface{} )")
		return nil, err
	}
	*/

	Obj, flag := SapphireIDMap[in.ObjId]

	if ! flag  {
		err := errors.New("SapphireObject ID is Invalid")
		fmt.Println("SapphireObject ID is Invalid")
		return nil,err
	}

	actualMethod := reflect.ValueOf(Obj).MethodByName(in.FuncName)

	if ! actualMethod.IsValid() {
		err := errors.New("Method Name is Inavlid")
		fmt.Println("Method Name is Inavlid :", actualMethod)
		return nil,err
	}
	wrapperMethod := reflect.ValueOf(Obj).MethodByName(in.FuncName + "_Wrap")
	
	if ! wrapperMethod.IsValid() {
		err := errors.New("wrappermethod is wrong some issue in stub generation")
		fmt.Println("Method Name is Inavlid :", wrapperMethod ,err)
		return nil, err
	}
	
	objects := make(map[reflect.Type]interface{})
	reqin := make([]reflect.Value, wrapperMethod.Type().NumIn())

	paramtype := wrapperMethod.Type().In(0).Elem()
	objects[paramtype] = in.Params
	reqin[0] = reflect.ValueOf(objects[paramtype])
	

	methodReslut := wrapperMethod.Call(reqin)

	ret := methodReslut[0].Interface().([]byte)

	return &api.GenericMethodReply{Ret: ret}, nil
}
func main() {

	SapphireNameMap = make(map[string]PlugInInfo)
	SapphireIDMap = make(map[string]interface{})

	//currently its hard coded either we can take from config or input parameters
	SharedLibsPath = "../sapphire_objects/"

	lis, err := net.Listen("tcp", ":7000")
	if err != nil {
		fmt.Println("Unable to listen to port 7000")
		return
	}

	s := grpc.NewServer()
	api.RegisterMgmtgrpcServiceServer(s, &Server{})
	s.Serve(lis)
	defer lis.Close()

}

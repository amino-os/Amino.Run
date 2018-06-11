package main

import (
	"fmt"
	"github.com/golang/protobuf/proto"
	"reflect"
	"sapphire_grpc/api"
	"strconv"
)

type GenericSo struct{}

func GenericCreate() interface{} {
	var temp GenericSo

	return &temp
}


func GenericSOMethodInvoke(methodInfoInterface interface{}, soObjectInterface interface{}) []byte {

	objects := make(map[reflect.Type]interface{})
	var ret []byte

	so := soObjectInterface.(*GenericSo)
	methodInfo := methodInfoInterface.(*api.GenericMethodRequest)
	wrapperName := fmt.Sprintf("%s_Wrap",methodInfo.FuncName)
	wrappermethod := reflect.ValueOf(so).MethodByName(wrapperName)


	in := make([]reflect.Value, wrappermethod.Type().NumIn())

	paramtype := wrappermethod.Type().In(0).Elem()
	objects[paramtype] = methodInfo.Params
	in[0] = reflect.ValueOf(objects[paramtype])


	fmt.Println("GenericInvoke wrapperName:", wrapperName)
	methodReslut := wrappermethod.Call(in)


	ret = methodReslut[0].Interface().([]byte)

	fmt.Println("GenericSOMethodInvoke return ret:", ret)
	return ret
}


func (SoObj *GenericSo) HelloWorld_Wrap(reqdata [] byte) []byte {

	fmt.Println("HelloWorld_Wrap:", reqdata)
	var in api.HelloWorldRequest

	err := proto.Unmarshal(reqdata,&in)

	if err != nil {
		fmt.Println("Error during Unmarshal",err.Error())
		return nil
	}
	out := SoObj.HelloWorld(&in)

	byteOut, err := proto.Marshal(out)

	if err != nil {
		fmt.Println("Error during Marshal",err.Error())
		return nil

	}

	return byteOut
}

func (SoObj *GenericSo) Fibbonaci_Wrap(reqdata [] byte) []byte {

	fmt.Println("Fibbonaci_Wrap:", reqdata)
	var in api.FibbonaciRequest

	err := proto.Unmarshal(reqdata,&in)

	if err != nil {
		fmt.Println("Error during Unmarshal",err.Error())
		return nil
	}
	out := SoObj.Fibbonaci(&in)
	byteOut, err := proto.Marshal(out)
	if err != nil {
		fmt.Println("Error during Marshal",err.Error())
		return nil
	}
	return byteOut
}


//The below are te Actual App Logic functions we can use normal parameters instead of struct also...

func (SoObj *GenericSo) HelloWorld(request *api.HelloWorldRequest) *api.HelloWorldReply {
	fmt.Println("HelloWorld:", request)
	return &api.HelloWorldReply{RetName: "HelloWorldMethod",Retval:33}
}


func (SoObj GenericSo) Fibbonaci(request *api.FibbonaciRequest) *api.FibbonaciReply {
	fmt.Println("Fibbonaci:", request)
	n := request.GetNum()
	var i int32
	n1 := 0
	n2 := 1
	fmt.Println("Fibbonaci:", n)
	for i = 0; i < n; i++ {
		temp := n1
		n1 = n2
		n2 = n1 + temp
	}
	fmt.Println("Fibbonaci Numebr is ", n2)
	return &api.FibbonaciReply{Ret: strconv.Itoa(n2)}
}

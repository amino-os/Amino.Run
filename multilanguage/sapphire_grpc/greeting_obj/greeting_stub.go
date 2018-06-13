package main

import (
	"fmt"
	"reflect"
	"sapphire_grpc/api"
	"sapphire_grpc/api/greeting"

	"github.com/golang/protobuf/proto"
)

type GenericGreetingSo struct{}

func GenericCreate() interface{} {
	var temp GenericGreetingSo

	return &temp
}

func GenericSOMethodInvoke(methodInfoInterface interface{}, soObjectInterface interface{}) []byte {

	objects := make(map[reflect.Type]interface{})
	var ret []byte

	so := soObjectInterface.(*GenericGreetingSo)
	methodInfo := methodInfoInterface.(*api.GenericMethodRequest)
	wrapperName := fmt.Sprintf("%s_Wrap", methodInfo.FuncName)
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

func (SoObj *GenericGreetingSo) HelloWorld_Wrap(reqdata []byte) []byte {

	fmt.Println("HelloWorld_Wrap:", reqdata)
	var in greeting.HelloWorldRequest

	err := proto.Unmarshal(reqdata, &in)

	if err != nil {
		fmt.Println("Error during Unmarshal", err.Error())
		return nil
	}
	var out greeting.HelloWorldReply
	out.RetName, out.Retval = SoObj.HelloWorld(in.Name, in.Num)

	byteOut, err := proto.Marshal(&out)
	if err != nil {
		fmt.Println("Error during Marshal", err.Error())
		return nil

	}

	return byteOut
}

func (SoObj *GenericGreetingSo) Fibbonaci_Wrap(reqdata []byte) []byte {

	fmt.Println("Fibbonaci_Wrap:", reqdata)
	var in greeting.FibbonaciRequest

	err := proto.Unmarshal(reqdata, &in)

	if err != nil {
		fmt.Println("Error during Unmarshal", err.Error())
		return nil
	}
	var out greeting.FibbonaciReply
	out.Ret = SoObj.Fibbonaci(in.Num)

	byteOut, err := proto.Marshal(&out)
	if err != nil {
		fmt.Println("Error during Marshal", err.Error())
		return nil
	}
	return byteOut
}

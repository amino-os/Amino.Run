package main

import (
	"fmt"
	"reflect"
	"sapphire_grpc/api"
	"sapphire_grpc/api/algo"

	"github.com/golang/protobuf/proto"
)

type GenericAlgoSo struct{}

func GenericCreate() interface{} {
	var temp GenericAlgoSo

	return &temp
}

func GenericSOMethodInvoke(methodInfoInterface interface{}, soObjectInterface interface{}) []byte {

	objects := make(map[reflect.Type]interface{})
	var ret []byte

	so := soObjectInterface.(*GenericAlgoSo)
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

func (soObj *GenericAlgoSo) Search_Wrap(reqData []byte) []byte {
	fmt.Println("Search_Wrap:", reqData)
	var in algo.SearchRequest

	err := proto.Unmarshal(reqData, &in)

	if err != nil {
		fmt.Println("Error during Unmarshal", err.Error())
		return nil
	}
	var out algo.SearchReplay
	out.Ret = soObj.Search(in.Num, in.Key)

	byteOut, err := proto.Marshal(&out)
	if err != nil {
		fmt.Println("Error during Marshal", err.Error())
		return nil
	}
	return byteOut
}
func (soObj *GenericAlgoSo) Sort_Wrap(reqData []byte) []byte {
	fmt.Println("Sort_Wrap:", reqData)
	var in algo.SortRequest

	err := proto.Unmarshal(reqData, &in)

	if err != nil {
		fmt.Println("Error during Unmarshal", err.Error())
		return nil
	}
	var out algo.SortReplay
	out.Arr = soObj.Sort(in.Arr)

	byteOut, err := proto.Marshal(&out)
	if err != nil {
		fmt.Println("Error during Marshal", err.Error())
		return nil
	}
	return byteOut
}

func (SoObj *GenericAlgoSo) Fibbonaci_Wrap(reqdata []byte) []byte {

	fmt.Println("Fibbonaci_Wrap:", reqdata)
	var in algo.FibbonaciRequest

	err := proto.Unmarshal(reqdata, &in)

	if err != nil {
		fmt.Println("Error during Unmarshal", err.Error())
		return nil
	}
	var out algo.FibbonaciReply
	out.Ret = SoObj.Fibbonaci(in.Num)

	byteOut, err := proto.Marshal(&out)
	if err != nil {
		fmt.Println("Error during Marshal", err.Error())
		return nil
	}
	return byteOut
}

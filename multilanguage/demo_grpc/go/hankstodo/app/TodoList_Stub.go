package main

import (
	"fmt"

	APPInterface "hankstodo/app_interface"

	"github.com/golang/protobuf/proto"
)

type TodoListStub struct {
	TodoList      *TodoList
	SapphireObjId string
	ParentSObjId  string
}

//todoListStub  variable is  reference to  TodoListStub (Stub  code)
//todoList variable is reference to toList Struct (User defined code )
//toListgRPC variable is reference to proto struct (Proto Genreated code )

func New_TodoList(parentSObjId string, sObjId string, replicaId string) ([]byte, interface{}) {
	var todoListStub TodoListStub
	fmt.Println("creating object for id", sObjId)
	todoListStub.ParentSObjId = parentSObjId
	todoListStub.SapphireObjId = sObjId
	var todogRPC APPInterface.TodoList
	todogRPC.Sid = sObjId
	retData, _ := proto.Marshal(&todogRPC)
	return retData, &todoListStub
}
func (t *TodoListStub) AddToDo(reqData []byte) ([]byte, error) {
	var addtodoReq APPInterface.InAddToDo

	proto.Unmarshal(reqData, &addtodoReq)
	var outAddTODo APPInterface.OutAddToDo

	outAddTODo.Arg0 = t.TodoList.AddToDo(addtodoReq.Arg0)
	retData, _ := proto.Marshal(&outAddTODo)
	return retData, nil

}

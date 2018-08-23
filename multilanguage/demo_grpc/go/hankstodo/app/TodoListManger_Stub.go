package main

import (
	"fmt"
	APPInterface "hankstodo/app_interface"

	sdk "sapphire_grpc/sapphire_go_runtime/runtime_sdk"

	"github.com/golang/protobuf/proto"
)

type TodoListManagerStub struct {
	TodoListManager *TodoListManager
	SapphireObjId   string
	ParentSObjId    string
}

//todoListMangerStub  variable is  reference to  TodoListManagerStub (Stub  code)
//todoListManager variable is reference to toListManger Struct (User defined code )
//toListMangergRPC variable is reference to proto struct (Proto Genreated code )

func New_TodoListManager(parentSObjId string, sObjId string, replicaId string) ([]byte, interface{}) {
	var tolistManagerStub TodoListManagerStub
	fmt.Println("creating object for id", sObjId)
	tolistManagerStub.ParentSObjId = parentSObjId
	tolistManagerStub.SapphireObjId = sObjId

	var todoListManager TodoListManager
	todoListManager.TodoLists = make(map[string]*TodoList)
	tolistManagerStub.TodoListManager = &todoListManager
	var todoListManagegRPC APPInterface.TodoListManager
	todoListManagegRPC.Sid = sObjId
	retData, _ := proto.Marshal(&todoListManagegRPC)
	return retData, &tolistManagerStub
}

func (t *TodoListManagerStub) NewTodoList(reqData []byte) ([]byte, error) {
	var newTodoListgRPC APPInterface.NewTodo
	proto.Unmarshal(reqData, &newTodoListgRPC)
	// Create the Inner sapphire Object
	// Intialize the stub from that
	retData, object, err := sdk.CreateObjectStub("hankstodo", "TodoList", t.SapphireObjId, make([]byte, 0))
	if err != nil {
		fmt.Println("Error during child object creation ", err)
		return nil, err
	}
	var todolist APPInterface.TodoList
	proto.Unmarshal(retData, &todolist)
	todolist.Name = newTodoListgRPC.Arg0
	todolistStub := object.(*TodoListStub)
	todolistStub.TodoList = t.TodoListManager.NewTodoList(newTodoListgRPC.Arg0)
	retData, _ = proto.Marshal(&todolist)
	fmt.Println("Todolist object created")
	//fmt.Println("Return data ", retData)
	return retData, nil
}

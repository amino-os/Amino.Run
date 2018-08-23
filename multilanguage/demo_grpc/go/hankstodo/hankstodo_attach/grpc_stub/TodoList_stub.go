package grpc_stub

import (
	"fmt"
	app "hankstodo/app_interface"
	"log"

	"github.com/golang/protobuf/proto"
)

type TodoListStub struct {
	Sid      string
	ClientId string
	Object   app.TodoList
}

func (t *TodoListStub) AddToDO(arg1 string) string {
	ret := ""
	fmt.Println("Inside AddtoDo")
	reqData, err := proto.Marshal(&app.InAddToDo{Arg0: arg1})
	if err != nil {
		log.Println(err)
		return ret
	}
	retData, err := GenericInvoke(t.ClientId, "addToDo", reqData)
	if err != nil {
		log.Println(err)
		return ret
	}
	var newTodoresponse app.OutAddToDo
	proto.Unmarshal(retData, &newTodoresponse)
	return newTodoresponse.Arg0
}

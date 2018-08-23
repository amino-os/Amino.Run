package grpc_stub

import (
	"fmt"
	app "hankstodo/app_interface"
	"log"

	"github.com/golang/protobuf/proto"
)

type TodoListManagerStub struct {
	Sid      string
	ClientId string
	Object   app.TodoListManager //Proto struct TodoListManager
}

func NewTodoListManager(url string) (TodoListManagerStub, error) {
	//Create the Object using the create request
	stub, err := Attach(url)
	if err != nil {
		log.Println(err)
		return TodoListManagerStub{}, err
	}

	return stub, nil
	// Send the stub to the user

}
func (t *TodoListManagerStub) NewTOdo(arg1 string) TodoListStub {
	log.Println("Inside NewtoDo")
	reqData, err := proto.Marshal(&app.NewTodo{Arg0: arg1})
	if err != nil {
		log.Println(err)
		return TodoListStub{}
	}
	retData, err := GenericInvoke(t.ClientId, "newTodoList", reqData)
	if err != nil {
		log.Println(err)
		return TodoListStub{}
	}
	//	log.Println(retData)
	var todoListgRPC app.TodoList
	err = proto.Unmarshal(retData, &todoListgRPC)
	if err != nil {
		fmt.Println("error in unmarshall")
	}
	fmt.Println(todoListgRPC.Name, todoListgRPC.Sid)
	//var newTodoresponse app.OutAddToDo
	//proto.Unmarshal(retData, &newTodoresponse)
	var todoListstub TodoListStub
	todoListstub.Sid = todoListgRPC.Sid
	ClientId, _, err := Acquire(todoListgRPC.Sid)
	if err != nil {
		log.Println(err)
		return TodoListStub{}
	}
	log.Println("Client Id ", ClientId)
	todoListstub.ClientId = ClientId
	todoListstub.Object = todoListgRPC
	return todoListstub

}

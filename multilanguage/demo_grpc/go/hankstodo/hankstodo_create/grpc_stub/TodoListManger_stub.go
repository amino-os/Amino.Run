package grpc_stub

import (
	"fmt"
	app "hankstodo/app_interface"
	"hankstodo/hankstodo_create/util"
	"log"
	grpcInterface "sapphire_grpc/sapphire_interface/sapphire_kernel_app"

	"github.com/golang/protobuf/proto"
	"golang.org/x/net/context"
)

type TodoListManagerStub struct {
	Sid      string
	ClientId string
	Object   app.TodoListManager //  proto struct of TodoListManager
}

func NewTodoListManager(langtype string) (TodoListManagerStub, error) {
	//Create the Object using the create request
	sid, err := CreateSapphireObject("hankstodo", "TodoListManager", langtype, make([]byte, 0))
	// Use acquire to  get the object
	if err != nil {
		return TodoListManagerStub{}, err
	}
	ClientId, retData, err := Acquire(sid)
	if err != nil {
		return TodoListManagerStub{}, err
	}
	var todoListMangerStub TodoListManagerStub
	var object app.TodoListManager
	todoListMangerStub.Sid = sid
	todoListMangerStub.ClientId = ClientId
	proto.Unmarshal(retData, &object)
	todoListMangerStub.Object = object

	return todoListMangerStub, nil
	// Send the stub to the user

}
func (t *TodoListManagerStub) SetURL(url string) (bool, error) {
	c := grpcInterface.NewAppServiceClient(util.GrpcConn)
	var setUrlRequest grpcInterface.URLRequest
	setUrlRequest.SId = t.Sid
	setUrlRequest.Url = url
	res, err := c.SetURL(context.Background(), &setUrlRequest)
	if err != nil {
		fmt.Println("Error During seturl")
		return false, err
	}
	return res.Status, err
}

func (t *TodoListManagerStub) NewTOdo(arg1 string) TodoListStub {
	log.Println("Inside NewtoDo")
	reqData, err := proto.Marshal(&app.NewTodo{Arg0: arg1})
	if err != nil {
		log.Println(err)
		return TodoListStub{}
	}
	retData, err := GenericInvoke(t.ClientId, "NewTodoList", reqData)
	if err != nil {
		log.Println(err)
		return TodoListStub{}
	}
	//log.Println(retData)
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

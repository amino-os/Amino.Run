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

type TodoListStub struct {
	Sid      string
	ClientId string
	Object   app.TodoList // proto struct of todolist
}

func (t *TodoListStub) AddToDO(arg1 string) string {
	ret := ""
	fmt.Println("Inside AddtoDo")
	reqData, err := proto.Marshal(&app.InAddToDo{Arg0: arg1})
	if err != nil {
		log.Println(err)
		return ret
	}
	retData, err := GenericInvoke(t.ClientId, "AddToDo", reqData)
	if err != nil {
		log.Println(err)
		return ret
	}
	var newTodoresponse app.OutAddToDo
	proto.Unmarshal(retData, &newTodoresponse)
	return newTodoresponse.Arg0
}
func (t *TodoListStub) SetURL(url string) (bool, error) {
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

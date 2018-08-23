package common

import (
	"errors"
	"fmt"
	"log"
	"plugin"
	"sync"

	"google.golang.org/grpc"
)

type SObjInfo struct {
	SapphireIDMap map[string]ObjectStub
	sync.RWMutex
}
type PlugInfo struct {
	SapphireNameMap map[string]*plugin.Plugin
	sync.RWMutex
}
type ObjectStub struct {
	SapphireObject interface{}
	ParentId       string
}

var KSGRPC *grpc.ClientConn

func (s *SObjInfo) ADDObject(sapphireID, replicaID, parentID string, Object interface{}) {
	var objectInfo ObjectStub
	objectInfo.ParentId = parentID
	objectInfo.SapphireObject = Object
	key := sapphireID + ":" + replicaID
	s.Lock()
	defer s.Unlock()
	s.SapphireIDMap[key] = objectInfo
}
func (s *SObjInfo) GetObject(sapphireID, replicaID string) (interface{}, error) {
	replicaObject := sapphireID + ":" + replicaID
	s.Lock()
	defer s.Unlock()

	stubObj, ok := s.SapphireIDMap[replicaObject]
	//fmt.Println(stubObj, ok)
	if !ok {
		err := errors.New("SapphireObjectNotFound")
		return nil, err
	}
	return stubObj.SapphireObject, nil
}
func (s *SObjInfo) DeleteObject(sapphireID, replicaID string) bool {
	replicaObject := sapphireID + ":" + replicaID
	s.Lock()

	defer s.Unlock()
	_, ok := s.SapphireIDMap[replicaObject]
	if !ok {
		fmt.Println("Object with id ", replicaObject, " Not found")
		return false
	}
	delete(s.SapphireIDMap, replicaObject)
	return true
}

var ObjectInfo *SObjInfo
var Plug *PlugInfo

func Init(ksIP, ksPORT string) error {
	var err error
	//	log.Println("Creating the grpc connection")
	KSGRPC, err = grpc.Dial(ksIP+":"+ksPORT, grpc.WithInsecure())
	if err != nil {
		log.Fatal("Unable to establish the connection")
		return err
	}
	ObjectInfo = &SObjInfo{}
	Plug = &PlugInfo{}

	Plug.SapphireNameMap = make(map[string]*plugin.Plugin)

	ObjectInfo.SapphireIDMap = make(map[string]ObjectStub)
	return nil
}
func (p *PlugInfo) AddPlug(objectName string, plug *plugin.Plugin) {
	p.Lock()
	defer p.Unlock()
	p.SapphireNameMap[objectName] = plug

}
func (p *PlugInfo) GetPlug(objectName string) (*plugin.Plugin, bool) {

	p.Lock()
	defer p.Unlock()
	plug, ok := p.SapphireNameMap[objectName]
	return plug, ok
}

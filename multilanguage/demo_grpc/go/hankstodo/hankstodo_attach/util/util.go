package util

import (
	"log"

	"google.golang.org/grpc"
)

var RMICONFIG string
var GrpcConn *grpc.ClientConn

func Init(KCIP, KCPORT, rmiIP, rmiPORT string) error {
	var err error
	log.Println("Creating the grpc connection")
	GrpcConn, err = grpc.Dial(KCIP+":"+KCPORT, grpc.WithInsecure())
	if err != nil {
		log.Fatal("Unable to establish the connection")
		return err
	}
	RMICONFIG = rmiIP + ":" + rmiPORT
	return nil
}

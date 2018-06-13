package main

import (
	"fmt"
	"strconv"
)

//The below are te Actual App Logic functions we can use normal parameters instead of struct also...

func (SoObj *GenericGreetingSo) HelloWorld(reqName string, reqNum int32) (string, int32) {

	fmt.Println("HelloWorld:", reqName, reqNum)
	fmt.Println("HelloWorld  method invocked on Object",&SoObj)
	return "HelloWorldMethod Invocked", 33
}
func (SoObj *GenericGreetingSo) Fibbonaci(reqNum int32) string {
	fmt.Println("Fibbonaci  method invocked on Object",&SoObj)
	var i int32
	n1 := 0
	n2 := 1
	fmt.Println("Fibbonaci:", reqNum)
	fmt.Println(&SoObj)
	for i = 0; i < reqNum; i++ {
		temp := n1
		n1 = n2
		n2 = n1 + temp
	}
	fmt.Println("Fibbonaci Numebr is ", n2)
	return strconv.Itoa(n2)
}

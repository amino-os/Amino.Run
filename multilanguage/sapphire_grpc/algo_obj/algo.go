package main

import (
	"fmt"
	"strconv"
)

func (SoObj *GenericAlgoSo) Sort(arr []int32) []int32 {
	fmt.Println("Sort  method invocked on Object",&SoObj)
	for i := 0; i < len(arr)-1; i++ {
		for j := 0; j < len(arr)-i-1; j++ {
			if arr[j] >= arr[j+1] {
				temp := arr[j]
				arr[j] = arr[j+1]
				arr[j+1] = temp
			}
		}
	}
	return arr
}
func (SoObj *GenericAlgoSo) Search(arr []int32, key int32) int32 {
	fmt.Println("Search  method invocked on Object",&SoObj)
	for i := 0; i < len(arr); i++ {
		if arr[i] == key {
			fmt.Println("Search Element found @ Position ", i)
			return int32(i + 1)
		}
	}
	fmt.Println("Search element Not found")
	return int32(-1)
}
func (SoObj *GenericAlgoSo) Fibbonaci(reqNum int32) string {
	fmt.Println("Fibbonaci  method invocked on Object",&SoObj)
	var i int32
	n1 := 0
	n2 := 1
	fmt.Println("Fibbonaci:", reqNum)
	for i = 0; i < reqNum; i++ {
		temp := n1
		n1 = n2
		n2 = n1 + temp
	}
	fmt.Println("Fibbonaci Numebr is ", n2)
	return strconv.Itoa(n2)
}

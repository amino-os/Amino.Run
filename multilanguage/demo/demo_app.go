package main

import "fmt"

func come_back() {
  fmt.Println("Back in Golang. !!!")
}

type DCAP_Sapphire interface {
  sapphire_helloWorld(string)
  sapphire_getObjCount()
  sapphire_incObjCount()
}

type Sapphire struct {
}

func (f Sapphire) sapphire_helloWorld(string) {
  fmt.Println("Inside golang helloWorld() !!!")
}

func (f Sapphire) sapphire_getObjCount() {
  fmt.Println("Inside golang getObjCount() !!!")
}

func (f Sapphire) sapphire_incObjCount() {
  fmt.Println("Inside golang incObjCount() !!!")
}

func newSapphire() DCAP_Sapphire {
  return &Sapphire{}
}

func main() {
 fmt.Println("Inside Golang main function. !!!")
 
 // New Inerface object
 obj := newSapphire()

 app_string := "Hello DCAP from Golang. !!!"
 obj.sapphire_helloWorld(app_string)
 
 obj.sapphire_getObjCount()
 obj.sapphire_incObjCount() 
 
 come_back()
}   


package main

import (
	"fmt"
)

type TodoList struct {
	ToDos []string
	Name  string
}

func (t *TodoList) AddToDo(todo string) string {
	fmt.Println("Inside of AddToDo")
	fmt.Println(todo)
	t.ToDos = append(t.ToDos, todo)
	return "Ok!"
}

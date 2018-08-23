package main

import (
	"fmt"
)

type TodoListManager struct {
	TodoLists map[string]*TodoList
}

func (t *TodoListManager) NewTodoList(name string) *TodoList {
	fmt.Println("Inside of  NewTodoList")
	if todoList, ok := t.TodoLists[name]; ok {
		return todoList
	}
	var todoList TodoList
	todoList.Name = name
	todoList.ToDos = make([]string, 0)
	t.TodoLists[name] = &todoList
	return &todoList
}

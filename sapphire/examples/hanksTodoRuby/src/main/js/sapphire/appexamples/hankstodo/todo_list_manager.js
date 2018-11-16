class TodoListManager {
    constructor() {
        console.log("constructor");
        this.map = new Map();
    }

    newTodoList(name) {
        console.log("new_todo_list");
        var b = this.map.has(name);
        if (!b) this.map.set(name, new Set());
        return b;
    }

    deleteTodoList(name) {
        console.log("delete_todo_list");
        var b = this.map.has(name);
        if (b) this.map.delete(name);
        return b;
    }

    addTodo(todoListName, todoTaskName) {
        console.log("add_todo");
        this.map.get(todoListName).add(todoTaskName);
        return "OK_add_todo"
    }

    getTodos(todoListName) {
        console.log("get_todos");
        var v = this.map.get(todoListName);
        var res = [];
        v.forEach(function(value) {
            res.push(value);
        });
        return res;
    }

    completeTodo(todoListName, todoTaskName) {
        console.log("complete_todo");

        if (this.map.has(todoListName) && this.map.get(todoListName).has(todoTaskName)) {
            this.map.get(todoListName).delete(todoTaskName);
            return true;
        }

        return false;
    }
}

/*
function test() {
    var c = new TodoListManager();
    c.newTodoList("todoList1");
    c.addTodo("todoList1", "task1");
    c.addTodo("todoList1", "task2");
    var v = c.getTodos("todoList1");
    v.forEach(function(value) {
      console.log(value);
    });
    c.completeTodo("todoList1", "task1");
    v = c.getTodos("todoList1");
    v.forEach(function(value) {
          console.log(value);
        });
    c.deleteTodoList("todoList1");
}

// For test purpose only
//test();
*/
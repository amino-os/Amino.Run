class TodoListManager {
    constructor() {
        console.log("constructor");
        this.map = new Map();
    }

    new_todo_list(name) {
        console.log("new_todo_list");
        var b = this.map.has(name);
        if (!b) this.map.set(name, new Set());
        return b;
    }

    delete_todo_list(name) {
        console.log("delete_todo_list");
        var b = this.map.has(name);
        if (b) this.map.delete(name);
        return b;
    }

    add_todo(todoListName, todoTaskName) {
        console.log("add_todo");
        this.map.get(todoListName).add(todoTaskName);
        return "OK_add_todo"
    }

    get_todos(todoListName) {
        console.log("get_todos");
        var v = this.map.get(todoListName);
        var res = [];
        v.forEach(function(value) {
            res.push(value);
        });
        return res;
    }

    complete_todo(todoListName, todoTaskName) {
        console.log("complete_todo");

        if (this.map.has(todoListName) && this.map.get(todoListName).has(todoTaskName)) {
            this.map.get(todoListName).delete(todoTaskName);
            return true;
        }

        return false;
    }
}

function test() {
    var c = new TodoListManager();
    c.new_todo_list("todoList1");
    c.add_todo("todoList1", "task1");
    c.add_todo("todoList1", "task2");
    var v = c.get_todos("todoList1");
    v.forEach(function(value) {
      console.log(value);
    });
    c.complete_todo("todoList1", "task1");
    v = c.get_todos("todoList1");
    v.forEach(function(value) {
          console.log(value);
        });
    c.delete_todo_list("todoList1");
}

test();
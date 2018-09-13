function Student(id, name) {
    this.id = id;
    this.name = name;
}

Student.prototype.setId = function(id) {
    this.id = id;
}

Student.prototype.setName = function(name) {
    this.name = name;
}

 Student.prototype.getId = function() {
     return this.id;
 }

 Student.prototype.getName =function() {
     return this.name;
 }

Student.prototype.constructor = function () {
    return new Student();
}

class College {
    constructor(name) {
        this.name = name;
        this.students = [];
        this.students.constructor = function constructor() {
            return [];
        }
    }

    getName() {
        return this.name;
    }
    
    addStudent(student) {
        this.students.push(student);
    }

    getStudents() {
        return this.students;
    }
}

//module.exports = College;

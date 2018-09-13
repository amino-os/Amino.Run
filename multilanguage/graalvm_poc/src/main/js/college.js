const Student = require('./student.js');

class College {
    constructor(name) {
        this.name = name;
        this.students = [];
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

module.exports = College;

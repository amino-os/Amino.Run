class Student {
    /**
     * Every class must have a default constructor.
     * Deserailizer relies on the default constructor
     * to create instance.
     */
    constructor() {
        this.id = 0;
        this.name = "defaultstudent";
    }

    setId(id) {
        this.id = id;
    }

    setName(name) {
        this.name = name;
    }

    getId() {
        return this.id;
    }

    getName() {
        return this.name;
    }

    toString() {
        return this.id + ":" + this.name;
    }
}

class College {
    constructor() {
        this.name = "defaultcollege";
        this.students = [];
    }

    setName(name) {
        this.name = name;
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

    toString() {
        return this.name + this.students;
    }
}

//module.exports = College;

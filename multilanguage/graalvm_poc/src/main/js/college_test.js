var assert = require('assert');
const College = require('./college.js');
const Student = require('./student.js');

describe('College', function() {
    describe('getStudents', function() {
        var collegeName = "bellevue-college";
        var c = new College(collegeName);
        c.addStudent(new Student(1, "Alex"));
        c.addStudent(new Student(2, "Bob"));
        assert.equal("bellevue-college", c.getName());
        assert.equal(2, c.getStudents().length);
        assert.equal(1, c.getStudents()[0].getId());
        assert.equal(2, c.getStudents()[1].getId());
    });
});

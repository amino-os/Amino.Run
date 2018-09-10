
var grandchild = function(x) {
    this.grandchildfield = x;
}

var child = function(x) {
    this.grandchild = x
}

var complex = function() {
    this.stringfield = "hello";
    this.intfield = 3;
    gc = new grandchild(3);
    this.child1 = new child(gc);
    this.child2 = new child(gc);
}

complex.prototype.testfunc = function() {
    return 2;
}

complex.prototype.construct = function() {
    return new complex();
}

child.prototype.construct = function() {
    return new child();
}

grandchild.prototype.construct = function() {
    return new grandchild();
}

new complex();

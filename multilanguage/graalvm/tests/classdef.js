

var testjs = function() { 
  this.stringfield = "hello";
  this.intfield = 3;
  secret = 5;
}

testjs.prototype.construct = function() {
    return new testjs
}

new testjs();

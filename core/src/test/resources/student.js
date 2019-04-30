class Student {
    constructor() {
        this.id = 0;
        this.name = "";
        this.buddies = [];
    }

    setId(id) {
        this.id = id;
    }

    getId() {
        return this.id;
    }

    setName(name) {
        this.name = name;
    }

    getName() {
        return this.name;
    }

    addBuddy(buddy) {
        this.buddies.push(buddy);
    }

    getBuddies() {
        return this.buddies;
    }

    //TODO: This is workaround for the issue: https://github.com/oracle/graal/issues/678.
    //This will be deleted once the above issue is fixed.
    //It will return all functions and instance variables in this class.
    getMemberKeys() {
        let obj = new Student();
        //TODO:<Start> The common code we can put as Util function and call it by passing object.
        //TODO:Currently we are exploring it and we can handle it in future
        let property = [];
        while (obj = Reflect.getPrototypeOf(obj)) {
            //the below condition is used to avoid 'Object' functions like "toString,isPrototypeOf,propertyIsEnumerable,toLocaleString" etc
            //GraalVm Method 'Value.getMemberKeys()' is not giving any 'Object' functions.
            //the function 'getMemberKeys' should return same as 'Value.getMemberKeys()'
            if (obj != Object.prototype) {
                 let keys = Reflect.ownKeys(obj)
                 keys.forEach((k) => {
                         property.push(k)
                 });
            }
        }

        //Fill the instance variables
        Object.getOwnPropertyNames(this).forEach((k) => {
                                            property.push(k)
                                         });
        //TODO:<End> The common code we can put as Util function and call it by passing object.
        return property;
    }
}

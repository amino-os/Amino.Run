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
    //It will return all functions and instance variables in this class except constructor.
    getMemberKeys() {
        let obj = new Student();
        let property = [];
        while (obj = Reflect.getPrototypeOf(obj)) {
            //the below condition is used to avoid 'Object' functions like "toString,isPrototypeOf,propertyIsEnumerable,toLocaleString" etc
            if (obj != Object.prototype) {
                 let keys = Reflect.ownKeys(obj)
                 keys.forEach((k) => {
                     if (k !== "constructor") {
                         property.push(k)
                     }
                 });
            }
        }

        //Fill the instance variables
        Object.getOwnPropertyNames(this).forEach((k) => {
                                            property.push(k)
                                         });
        return property;
    }
}

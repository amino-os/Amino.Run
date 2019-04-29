
 // This is self-defined data type, which will be used by client to save in the key value store.
 class TestData {
     constructor() {
         this.val = "hello";
     }
 }

class KeyValueStore {
    constructor() {
        this.map = new Map();
    }

    printall() {
        console.log("\n\n***** begin dumping all data in kv store ***********")
        for (let [k, v] of this.map) {
            console.log(k, v);
        }

        console.log("***** finish dumping all data in kv store ***********\n\n")
    }

    set(key, value) {
        this.map.set(key, value);
        console.log("set: " + key + " -> " + value);
        return true;
    }

    get(key) {
        return this.map.get(key);
    }

    contains(key) {
        return this.map.has(key);
    }

    //TODO: This is workaround for the issue: https://github.com/oracle/graal/issues/678.
    //This will be deleted once the above issue is fixed.
    //It will return all functions and instance variables in this class except constructor.
    getMemberKeys() {
        let obj = new KeyValueStore();
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

/*
function test() {
    var store = new KeyValueStore();
    store.set("key1", "value1");
    console.log(store.get("key1"));
    store.set("key1", 2);
    console.log(store.get("key1"));
    console.log(store.contains("key1"));
}

// For test purpose only
// test();
*/
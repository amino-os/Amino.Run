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
        this.printall();
        return true;
    }

    get(key) {
        return this.map.get(key);
    }

    contains(key) {
        return this.map.has(key);
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
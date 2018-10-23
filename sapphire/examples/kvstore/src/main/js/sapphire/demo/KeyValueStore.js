/**
 * A Key-Value Store where keys are {@code String}s and
 * values are {@code Serializable}s.
 */
class JSKeyValueStore {
    constructor() {
        this.list = new Map()
    }

    get(key){
        console.log(`server: getting value with key : ${key}`)
        return this.list.get(key)
    }

    set(key, value) {
        console.log(`server: setting ${key} = ${value}`)
        this.list.set(key,value)
    }

    contains(key) {
        console.log(`server: checking existence with key: ${key}`)
        return this.list.has(key)
    }
}

/*
// For test purpose only
function test() {
    kvs = new JSKeyValueStore()

    for (i=0; i<30; ++i) {
        key = "key_" + i;
        val = "val_" + i;

        console.log(`client: setting ${key} = ${val}`)
        kvs.set(key, val)

        var v = kvs.get(key)
        console.log(`client: got value ${v} with key ${key}`)
    }
}

test();
*/
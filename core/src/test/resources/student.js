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
}

package sapphire.policy.dht;

import java.io.Serializable;

public  class DHTNode implements Comparable<DHTNode>, Serializable {
        public DHTKey id;
        public DHTPolicy.DefaultServerPolicy server;

        public DHTNode(DHTKey id, DHTPolicy.DefaultServerPolicy server) {
            this.id = id;
            this.server = server;
        }

        public DHTKey getId() {
            return id;
        }

        @Override
        public String toString() {
            return id.toString();
        }

        @Override
        public boolean equals(Object other) {
            DHTNode o = (DHTNode) other;
            if (o == null) return false;
            if (o.id.compareTo(id) == 0) return true;
            return false;
        }

        @Override
        public int compareTo(DHTNode another) {
            return another.id.compareTo(this.id);
        }
    }

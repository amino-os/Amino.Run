package sapphire.policy.checkpoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import sapphire.policy.DefaultSapphirePolicy;

/**
 * Created by quinton on 1/15/18.
 *
 * <p>Base class for checkpoint policies. Put common stuff in here that all checkpoint policies can
 * inherit/reuse.
 */
public abstract class CheckpointPolicyBase extends DefaultSapphirePolicy {
    public abstract static class ClientPolicy extends DefaultClientPolicy {}

    public abstract static class ServerPolicy extends DefaultServerPolicy {
        // TODO: Generate sensible file name based on AppObject class, instance and
        // date/time/sequence number
        // TODO: Or probably better, allow the client to specify a file to checkpoint to and from,
        // and delete as required.
        private String checkPointFileName = "checkpoint.dat";

        /**
         * Save a checkpoint of the object to disk
         *
         * @throws Exception TODO: Instead of interacting with OS directly in DM, it is better to
         *     delegate the work to Kernel server. Policies should interacts with kernel server and
         *     kernel server should interacts with OS. this decoupling allows us to add more
         *     functionality in kernel server e.g. 1. Garbage Collection: Kernel server may keep all
         *     checkpoint files under one dedicated directory, and kernel server may have background
         *     thread to garbage collect expired checkpoint files. 2. Data Encryption: Kernel server
         *     may choose to encrypt data files.
         */
        public synchronized void saveCheckpoint() throws Exception {
            ObjectOutputStream oos = null;
            try {
                FileOutputStream ofs = new FileOutputStream(this.checkPointFileName);
                oos = new ObjectOutputStream(ofs);
                this.appObject.write(oos);
                oos.flush();
            } finally {
                if (oos != null) {
                    /* TODO: If the object was only partially written, we might leave an unreadable file.
                      Handle partially written files here by deleting them, or rolling back to a previous checkpoint.
                    */
                    oos.close();
                }
            }
        }

        /**
         * Restore a checkpoint of the object from disk
         *
         * @throws Exception TODO: See above.
         */
        public synchronized void restoreCheckpoint() throws Exception {
            ObjectInputStream ois = null;
            try {
                FileInputStream ifs = new FileInputStream(this.checkPointFileName);
                ois = new ObjectInputStream(ifs);
                this.appObject.read(ois);
            } finally {
                if (ois != null) {
                    ois.close();
                }
            }
        }
        /**
         * Delete a checkpoint of the object from disk
         *
         * @return true if and only if the file or directory is successfully deleted; false
         *     otherwise
         */
        public synchronized boolean deleteCheckpoint() {
            return new File(this.checkPointFileName).delete();
        }
    }

    public abstract static class GroupPolicy extends DefaultGroupPolicy {}
}

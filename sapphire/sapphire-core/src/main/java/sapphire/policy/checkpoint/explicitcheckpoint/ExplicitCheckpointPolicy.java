package sapphire.policy.checkpoint.explicitcheckpoint;


import java.io.Serializable;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import sapphire.common.AppObject;
import sapphire.policy.DefaultSapphirePolicy;

/**
 * Created by quinton on 1/15/18.
 *
 * Checkpoint to disk and restore from checkpoint with explicit calls from the application.
 * The SO MUST implement saveCheckpoint() and restoreCheckpoint() methods by implementing the
 * ExplicitCheckpointer interface (or simply extending the ExplicitCheckpointerImpl class).
 * Note that those methods are only called after the DM has completed the actual checkpoint
 * or restore operation and may therefore be generally be left empty, unless the SO needs to
 * perform logging and/or other similar operations.
 * TODO: Perhaps improve this by e.g. using annotations instead, and possibly supporting both pre- and post- operation  hooks.
 **/
public class ExplicitCheckpointPolicy extends DefaultSapphirePolicy{
    public static class ExplicitCheckpointClientPolicy extends DefaultClientPolicy {}

    public static class ExplicitCheckpointServerPolicy extends DefaultServerPolicy {
        // TODO: Generate sensible file name based on AppObject class, instance and date/time/sequence number
        // TODO: Or probably better, allow the client to specify a file to checkpoint to and from, and delete as required.
        private String checkPointFileName = "checkpoint.dat";

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (isSaveCheckpoint(method)) {
                saveCheckpoint();
                return null;
            }
            else if (isRestoreCheckpoint(method)) {
                restoreCheckpoint();
                return null;
            }
            else {
                return super.onRPC(method, params);
            }
        }

        Boolean isSaveCheckpoint(String method) {
            // TODO better check than simple base name
            return method.contains(".saveCheckpoint(");
        }

        Boolean isRestoreCheckpoint(String method) {
            // TODO better check than simple base name
            return method.contains(".restoreCheckpoint(");
        }

        /**
         * Save a checkpoint of the object to disk
         * @throws Exception
         * TODO: Instead of interacting with OS directly in DM, it is better to delegate the work to Kernel server.
         * DMs should interacts with kernel server and kernel server should interacts with OS.
         * this decoupling allows us to add more functionality in kernel server
         * e.g.
         * 1. Garbage Collection: Kernel server may keep all checkpoint files under one dedicated directory, and kernel server may have background thread to garbage collect expired checkpoint files.
         * 2. Data Encryption: Kernel server may choose to encrypt data files.
         */
        synchronized void saveCheckpoint() throws Exception {
            ObjectOutputStream oos = null;
            try {
                FileOutputStream ofs = new FileOutputStream(this.checkPointFileName);
                oos = new ObjectOutputStream(ofs);
                this.appObject.writeObject(oos);
                oos.flush();
            }
            finally {
                if (oos!=null) {
                    /* TODO: If the object was only partially written, we might leave an unreadable file.
                       Handle partially written files here by deleting them, or rolling back to a previous checkpoint.
                     */
                    oos.close();
                }
            }
        }

        /**
         * Restore a checkpoint of the object from disk
         * @throws Exception
         * TODO: See above.
         */
        synchronized void restoreCheckpoint() throws Exception {
            ObjectInputStream ois = null;
            try {
                FileInputStream ifs = new FileInputStream(this.checkPointFileName);
                ois = new ObjectInputStream(ifs);
                this.appObject.readObject(ois);
            }
            finally {
                if (ois != null) {
                    ois.close();
                }
            }
        }
    }

    public static class ExplicitCheckpointGroupPolicy extends DefaultGroupPolicy {}
}

package sapphire.oms;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.common.SapphireSoStub;
import sapphire.kernel.common.KernelOID;
import sapphire.runtime.EventHandler;

public class SapphireInstanceManager {

    private SapphireObjectID oid;
    private SapphireObjectID parentSapphireObjId;
    private String name;
    private SapphireSoStub objectStub;
    private int referenceCount;
    private ArrayList<KernelOID> clientIdList;
    private ArrayList<SapphireObjectID> childSapphireObjIds;
    private EventHandler instanceDispatcher;
    private HashMap<SapphireReplicaID, EventHandler> replicaDispatchers;
    private Random oidGenerator;

    /**
     * Randomly generate a new replica id
     *
     * @return
     */
    private SapphireReplicaID generateSapphireReplicaID() {
        return new SapphireReplicaID(oid, oidGenerator.nextInt());
    }

    public SapphireInstanceManager(SapphireObjectID oid, EventHandler dispatcher) {
        this.oid = oid;
        instanceDispatcher = dispatcher;
        replicaDispatchers = new HashMap<SapphireReplicaID, EventHandler>();
        oidGenerator = new Random(new Date().getTime());
        clientIdList = new ArrayList<KernelOID>();
        childSapphireObjIds = new ArrayList<SapphireObjectID>();
    }

    public EventHandler getInstanceDispatcher() {
        return instanceDispatcher;
    }

    public void setInstanceDispatcher(EventHandler dispatcher) {
        instanceDispatcher = dispatcher;
    }

    public EventHandler getReplicaDispatcher(SapphireReplicaID rid)
            throws SapphireObjectNotFoundException {
        EventHandler dispatcher = replicaDispatchers.get(rid);
        if (dispatcher == null) {
            throw new SapphireObjectNotFoundException("Not a valid kernel object id.");
        }
        return dispatcher;
    }

    public void setReplicaDispatcher(SapphireReplicaID rid, EventHandler dispatcher) {
        replicaDispatchers.put(rid, dispatcher);
    }

    public SapphireReplicaID addReplica(EventHandler dispatcher) {
        SapphireReplicaID rid = generateSapphireReplicaID();
        replicaDispatchers.put(rid, dispatcher);
        return rid;
    }

    public void removeReplica(SapphireReplicaID rid) {
        replicaDispatchers.remove(rid);
    }

    public Map<SapphireReplicaID, EventHandler> getReplicaMap() {
        return replicaDispatchers;
    }

    public void clear() {
        replicaDispatchers.clear();
    }

    public void addClientId(KernelOID clientId) {
        clientIdList.add(clientId);
    }

    public void removeClientId(KernelOID clientId) {
        clientIdList.remove(clientId);
    }

    public void removeAllClientId() {
        clientIdList.clear();
    }

    public ArrayList<KernelOID> getClientList() {
        return clientIdList;
    }

    public void addChildSapphireObjId(SapphireObjectID sapphireObjId) {
        childSapphireObjIds.add(sapphireObjId);
    }

    public void removeChildSapphireObjId(SapphireObjectID sapphireObjId) {
        childSapphireObjIds.remove(sapphireObjId);
    }

    public ArrayList<SapphireObjectID> getChildSapphireObjIds() {
        return childSapphireObjIds;
    }

    public void setName(String sapphireObjName) {
        name = sapphireObjName;
    }

    public void setObjectStub(SapphireSoStub objStub) {
        objectStub = objStub;
    }

    public SapphireSoStub getObjectStub() {
        return objectStub;
    }

    public int incrRefCountAndGet() {
        return ++referenceCount;
    }

    public int decrRefCountAndGet() {
        return --referenceCount;
    }

    public SapphireObjectID getParentSapphireObjId() {
        return parentSapphireObjId;
    }

    public void setParentSapphireObjId(SapphireObjectID parentOid) {
        this.parentSapphireObjId = parentOid;
    }
}

package sapphire.common;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import sapphire.policy.SapphirePolicy;

/** Created by root1 on 18/7/18. */
public class SapphireSoStub implements Serializable {
    private final SapphireObjectID sapphireObjId;
    private final SapphireObjectID parentSapphireObjId;
    private final byte[] opaqueObject;
    private final AppObjectStub appObjectStub;
    private final Annotation[] dmAnnotations;
    private final String clientPolicyName;
    private final SapphirePolicy.SapphireServerPolicy serverPolicy;
    private final SapphirePolicy.SapphireGroupPolicy groupPolicy;

    public SapphireSoStub(SapphireSoStubBuilder builder) {
        sapphireObjId = builder.sapphireObjId;
        parentSapphireObjId = builder.parentSapphireObjId;
        opaqueObject = builder.opaqueObject;
        dmAnnotations = builder.dmAnnotations;
        clientPolicyName = builder.clientPolicyName;
        serverPolicy = builder.serverPolicy;
        groupPolicy = builder.groupPolicy;
        appObjectStub = builder.appObjectStub;
    }

    public SapphireObjectID getSapphireObjId() {
        return sapphireObjId;
    }

    public byte[] getOpaqueObject() {
        return opaqueObject;
    }

    public Annotation[] getDmAnnotations() {
        return dmAnnotations;
    }

    public String getClientPolicyName() {
        return clientPolicyName;
    }

    public SapphirePolicy.SapphireServerPolicy getServerPolicy() {
        return serverPolicy;
    }

    public SapphirePolicy.SapphireGroupPolicy getGroupPolicy() {
        return groupPolicy;
    }

    public SapphireObjectID getParentSapphireObjId() {
        return parentSapphireObjId;
    }

    public AppObjectStub getAppObjectStub() {
        return appObjectStub;
    }

    public static class SapphireSoStubBuilder implements Serializable {
        private SapphireObjectID sapphireObjId;
        private SapphireObjectID parentSapphireObjId;
        private byte[] opaqueObject;
        private Annotation[] dmAnnotations;
        private String clientPolicyName;
        private SapphirePolicy.SapphireServerPolicy serverPolicy;
        private SapphirePolicy.SapphireGroupPolicy groupPolicy;
        private AppObjectStub appObjectStub;

        public SapphireSoStubBuilder setSapphireObjId(SapphireObjectID sid) {
            sapphireObjId = sid;
            return this;
        }

        public SapphireSoStubBuilder setParentSapphireObjId(SapphireObjectID parentSid) {
            parentSapphireObjId = parentSid;
            return this;
        }

        public SapphireSoStubBuilder setOpaqueObject(byte[] opaqueObj) {
            opaqueObject = opaqueObj;
            return this;
        }

        public SapphireSoStubBuilder setdmAnnotations(Annotation[] annotations) {
            dmAnnotations = annotations;
            return this;
        }

        public SapphireSoStubBuilder setClientPolicyName(String clientName) {
            clientPolicyName = clientName;
            return this;
        }

        public SapphireSoStubBuilder setServerPolicy(SapphirePolicy.SapphireServerPolicy server) {
            serverPolicy = server;
            return this;
        }

        public SapphireSoStubBuilder setGroupPolicy(SapphirePolicy.SapphireGroupPolicy group) {
            groupPolicy = group;
            return this;
        }

        public SapphireSoStubBuilder setAppObjectStub(AppObjectStub appObjStub) {
            this.appObjectStub = appObjStub;
            return this;
        }

        public SapphireSoStub create() {
            return new SapphireSoStub(this);
        }
    }
}

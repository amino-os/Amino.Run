package javaRuntime;

import com.google.protobuf.ByteString;

/**
 * Created with IntelliJ IDEA.
 * User: root1
 * Date: 5/8/18
 * Time: 11:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class SapphireObject {
    private ByteString objectStream;
    private Object objectStub;
    private String sid;
    private String replicaId;

    public SapphireObject(Object objStub, ByteString stream, String sapphireId, String repId) {
        objectStub = objStub;
        objectStream = stream;
        sid = sapphireId;
        replicaId = repId;
    }

    public Object getObjectStub() {
        return objectStub;
    }

    public ByteString getObjectStream() {
        return objectStream;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getReplicaId() {
        return replicaId;
    }

    public void setReplicaId(String replicaId) {
        this.replicaId = replicaId;
    }
}

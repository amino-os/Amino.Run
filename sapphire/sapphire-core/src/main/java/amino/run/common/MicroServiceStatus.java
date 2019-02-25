package amino.run.common;

import amino.run.kernel.common.KernelOID;

/** Created by Venugopal Reddy K on 26/8/18. */

/**
 * MicroServiceStatus is a DTO to notify the health status from server policy(on kernel server) to
 * group policy(on OMS)
 */
public class MicroServiceStatus implements NotificationObject {
    public final MicroServiceID sapphireObjId;
    public final KernelOID groupId;
    public final KernelOID serverId;
    public final boolean status;

    public MicroServiceStatus(
            MicroServiceID sapphireObjId, KernelOID groupId, KernelOID serverId, boolean status) {
        this.sapphireObjId = sapphireObjId;
        this.groupId = groupId;
        this.serverId = serverId;
        this.status = status;
    }

    public MicroServiceID getSapphireObjId() {
        return sapphireObjId;
    }

    public KernelOID getGroupId() {
        return groupId;
    }

    public KernelOID getServerId() {
        return serverId;
    }

    public boolean isStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MicroServiceStatus that = (MicroServiceStatus) o;

        if (!groupId.equals(that.groupId)) return false;
        return serverId.equals(that.serverId);
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + serverId.hashCode();
        return result;
    }
}

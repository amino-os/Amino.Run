package amino.run.common;

import java.io.Serializable;

/**
 * Notification interface. It is a marker interface. An implementation of this interface is passed
 * as parameter to {@link amino.run.policy.Upcalls.GroupUpcalls#onNotification(Notification)} and
 * {@link amino.run.policy.Upcalls.ServerUpcalls#onNotification(Notification)}
 */
public interface Notification extends Serializable {}

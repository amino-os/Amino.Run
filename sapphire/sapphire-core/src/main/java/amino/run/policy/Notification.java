package amino.run.policy;

import java.io.Serializable;

/**
 * Notification interface. It is a marker interface. An implementation of this interface is passed
 * as parameter to {@link Upcalls.GroupUpcalls#onNotification(Notification)} and {@link
 * Upcalls.ServerUpcalls#onNotification(Notification)}
 */
public interface Notification extends Serializable {}

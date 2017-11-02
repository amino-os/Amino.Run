package sapphire.common;

import java.io.Serializable;

import sapphire.policy.SapphirePolicy.SapphireClientPolicy;

public interface AppObjectStub extends Serializable, Cloneable {
	public void $__initialize(SapphireClientPolicy client);
	public void $__initialize(boolean directInvocation);
	public Object $__clone() throws CloneNotSupportedException;
}
package sapphire.policy.scalability;

import sapphire.runtime.MethodInvocationRequest;

/**
 * @author terryz
 */
public interface ILogger {
    void log(MethodInvocationRequest request) throws Exception;
}

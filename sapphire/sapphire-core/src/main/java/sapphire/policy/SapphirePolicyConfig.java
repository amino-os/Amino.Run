package sapphire.policy;

import java.io.Serializable;

/**
 * Interface for sapphire policy configuration.
 *
 * <p>Each sapphire policy can optionally define a Config class to allow programmers to pass
 * configurations to the sapphire policy. All Config classes should implement this interface.
 */
public interface SapphirePolicyConfig extends Serializable {}

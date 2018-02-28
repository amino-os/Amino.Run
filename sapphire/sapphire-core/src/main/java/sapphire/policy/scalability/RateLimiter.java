package sapphire.policy.scalability;

/**
 * Created by SrinivasChilveri on 27/2/18.
 * Generic interface for rate limiter
 */

public interface RateLimiter {
	void start(); // start the rate limiter with the pool of resources
	boolean tryAcquire(); // Used to acquire a resource from the pool
	boolean tryAcquire(int permits); // Used to acquire multiple resource from the pool
	int getMaxAllowedLoad(); // Maximum resource count in the pool
	int getCurrentLoad(); // Get the current used resource count from the pool
	void stop(); // Stop the rate limiter
}

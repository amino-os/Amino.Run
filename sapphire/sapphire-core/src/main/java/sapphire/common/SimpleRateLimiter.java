package sapphire.common;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by SrinivasChilveri on 2/21/18.
 * Simple rate limiter implementation. It uses semaphores with configurable permits, time unit,
 * time period. Mechanism used to rate limit is as follows:
 * User need to tryAcquire a permit in semaphore each time resource is accessed. At the end of time
 * period, all the acquired permits are released back to semaphore.
 * If the number of times resource is accessed exceeds the maximum permits within the time slot,
 * tryAcquire fails and thus controls the resource access.
 */

public class SimpleRateLimiter implements RateLimiter {
	private Semaphore semaphore; 					// Semaphore used to rate limit
	private int maxPermits; 						// Maximum permits in the semaphore
	private TimeUnit timeUnit = TimeUnit.SECONDS; 	// Time unit
	private int period = 1; 						// Time period
	private Runnable customProc; 					// Custom runnable from the user of this rate limiter class
	private ScheduledExecutorService scheduler; 	// Scheduler used to give up the acquired semaphore permits within the time period

	/**
	 * Start a rate limiter with resource access limit(permits), period, time unit and
	 * customized runnable instance(which is called by scheduler periodically after each time period
	 * expiry)
	 */
	public void start() {
		if (null == customProc) {
			schedulePermitReplenishment();
		}
		else {
			schedulePermitReplenishment(customProc);
		}
	}

	/**
	 * Constructor with permits, period and timeunit
	 * @param permits
	 * @param period
	 * @param timeUnit
	 */
	public SimpleRateLimiter(int permits, int period, TimeUnit timeUnit) {
		this.semaphore = new Semaphore(permits,true);
		this.maxPermits = permits;
		this.period = period;
		this.timeUnit = timeUnit;
	}

	/**
	 * Constructor with permits, period, timeunit and custom runnable
	 * @param permits
	 * @param period
	 * @param timeUnit
	 * @param customProc
	 */
	public SimpleRateLimiter(int permits, int period, TimeUnit timeUnit, Runnable customProc) {
		this.semaphore = new Semaphore(permits,true);
		this.maxPermits = permits;
		this.period = period;
		this.timeUnit = timeUnit;
		this.customProc = customProc;
	}
	/**
	 * Constructor with permits
	 * @param permits
	 *
	 */
	public SimpleRateLimiter(int permits) {
		this.semaphore = new Semaphore(permits,true);
		this.maxPermits = permits;
	}

	/**
	 * Acquires a permit from the semaphore
	 * @return true or false based on the permit availability
	 */
	public boolean tryAcquire() {
		return semaphore.tryAcquire();
	}

	/**
	 * Acquires specified permits from the semaphore
	 * @param permits
	 * @return true or false based on the permit availability
	 */
	public boolean tryAcquire(int permits) {
		return semaphore.tryAcquire(permits);
	}

	/**
	 * Gets the maximum allowed load for this rate limiter instance
	 * @return Maximum capacity of this limiter instance
	 */
	public int getMaxAllowedLoad() { return maxPermits; }

	/**
	 * Gets the current load for this rate limiter instance
	 * @return current load on this limiter instance
	 */
	public int getCurrentLoad() { return maxPermits - semaphore.availablePermits(); }

	/**
	 * Stops the scheduler of this rate limiter
	 */
	public void stop() {
		scheduler.shutdownNow();
	}

	/**
	 * Schedule without customized runnable
	 */
	private void schedulePermitReplenishment() {
		scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				semaphore.release(maxPermits - semaphore.availablePermits());
			}
		}, 1, period, timeUnit);
	}

	/**
	 * Schedule with customized runnable
	 * @param customRun
	 */
	private void schedulePermitReplenishment(final Runnable customRun) {
		scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				customRun.run();
				semaphore.release(maxPermits - semaphore.availablePermits());
			}
		}, 1, period, timeUnit);
	}
}

package com.example.movielist.util;

import com.example.movielist.exception.ExternalApiException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classic Gang-of-Four Singleton, hand-implemented as a Java enum — the
 * language-guaranteed way to get exactly one instance, thread-safe, with no
 * container involved. Deliberately built this way instead of as a Spring @Bean
 * (which would also be a singleton, just container-managed — see
 * config.PasswordEncoderConfig for that contrast) specifically so both flavors
 * of "singleton" exist side by side in this codebase and can be explained by
 * hand: this one has no Spring dependency at all, constructed once by the JVM
 * classloader the first time OmdbRateLimiterSingleton.INSTANCE is referenced.
 *
 * In a real production codebase this would almost certainly just be a Spring
 * bean instead — DI makes it mockable in tests and visible in the application
 * context. It's implemented the GoF way here purely as the deliberate teaching
 * contrast the plan for this project calls for.
 *
 * Throttles calls to OMDb's free-tier rate limit with a simple fixed-window
 * counter (not sliding, not distributed — a real production limiter would use
 * something like Bucket4j/Redis for a multi-instance deployment; this is
 * intentionally the simplest version that demonstrates the idea).
 */
public enum OmdbRateLimiterSingleton {
	INSTANCE;

	private static final int MAX_CALLS_PER_WINDOW = 40;
	private static final long WINDOW_MILLIS = 60_000;

	private final AtomicInteger callsInWindow = new AtomicInteger(0);
	private volatile long windowStart = System.currentTimeMillis();

	/** Call before every outbound OMDb request. Throws once the window's budget is exhausted. */
	public synchronized void acquire() {
		long now = System.currentTimeMillis();
		if (now - windowStart > WINDOW_MILLIS) {
			windowStart = now;
			callsInWindow.set(0);
		}
		if (callsInWindow.incrementAndGet() > MAX_CALLS_PER_WINDOW) {
			throw new ExternalApiException("Movie API rate limit exceeded — try again shortly");
		}
	}
}

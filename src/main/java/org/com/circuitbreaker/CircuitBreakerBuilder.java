package org.com.circuitbreaker;

public class CircuitBreakerBuilder {
	private int failureThreshold = 5;
	private long retryTimeoutMillis = 1000;
	private int maxRetryFactor;

}

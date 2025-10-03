package org.com.circuitbreaker;

import org.com.circuitbreaker.exception.InvalidCircuitBreakerValueException;

public class CircuitBreakerBuilder {
	private int failureThreshold = 5;
	private long retryTimeoutMillis = 1000;
	private int maxRetryFactor = 8;


	public CircuitBreakerBuilder withFailureThreshold(int threshold) {
		if(threshold <= 0) {
			throw new InvalidCircuitBreakerValueException("Threshold should always be either positive or zero");
		}
		this.failureThreshold = threshold;
		return this;
	}

	public CircuitBreakerBuilder withRetryTimeoutMillis(long retryTimeoutMillis) {
		if(retryTimeoutMillis <= 0) {
			throw new InvalidCircuitBreakerValueException("RetryTimeoutMillis should always be either positive or zero");
		}
		this.retryTimeoutMillis = retryTimeoutMillis;
		return this;
	}

	public CircuitBreakerBuilder withMaxRetryFactor(int factor) {
		if(factor < 0) {
			throw new InvalidCircuitBreakerValueException("MaxRetryFactor should always either positive or zero");
		}
		this.maxRetryFactor = factor;
		return this;
	}

	public CircuitBreaker build() {
		return new CircuitBreaker(failureThreshold, retryTimeoutMillis, maxRetryFactor);
	}

}

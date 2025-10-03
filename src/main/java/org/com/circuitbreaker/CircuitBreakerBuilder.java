package org.com.circuitbreaker;

import org.com.circuitbreaker.exception.InvalidCircuitBreakerValueException;

/**
 * Builder class for creating CircuitBreaker instances with fluent configuration.
 * <p>
 * Provides a convenient way to configure circuit breaker parameters with validation
 * and default values. All builder methods validate inputs and throw
 * {@link InvalidCircuitBreakerValueException} for invalid values.
 * <p>
 * Default values:
 * <ul>
 *   <li>Failure threshold: 5</li>
 *   <li>Retry timeout: 1000ms</li>
 *   <li>Max retry factor: 8</li>
 * </ul>
 */
public class CircuitBreakerBuilder {
	private int failureThreshold = 5;
	private long retryTimeoutMillis = 1000;
	private int maxRetryFactor = 8;

	/**
	 * Sets the failure threshold for opening the circuit.
	 * <p>
	 * When the number of consecutive failures reaches this threshold,
	 * the circuit will transition to OPEN state.
	 *
	 * @param threshold the failure threshold, must be greater than 0
	 * @return this builder instance for method chaining
	 * @throws InvalidCircuitBreakerValueException if threshold is less than or equal to 0
	 */
	public CircuitBreakerBuilder withFailureThreshold(int threshold) {
		if(threshold <= 0) {
			throw new InvalidCircuitBreakerValueException("Threshold should always be either positive or zero");
		}
		this.failureThreshold = threshold;
		return this;
	}

	/**
	 * Sets the initial retry timeout in milliseconds.
	 * <p>
	 * This is the time the circuit waits in OPEN state before transitioning
	 * to HALF_OPEN to test if the service has recovered.
	 *
	 * @param retryTimeoutMillis the retry timeout in milliseconds, must be greater than 0
	 * @return this builder instance for method chaining
	 * @throws InvalidCircuitBreakerValueException if retryTimeoutMillis is less than or equal to 0
	 */
	public CircuitBreakerBuilder withRetryTimeoutMillis(long retryTimeoutMillis) {
		if(retryTimeoutMillis <= 0) {
			throw new InvalidCircuitBreakerValueException("RetryTimeoutMillis should always be either positive or zero");
		}
		this.retryTimeoutMillis = retryTimeoutMillis;
		return this;
	}

	/**
	 * Sets the maximum retry factor for exponential backoff.
	 * <p>
	 * When a trial request fails in HALF_OPEN state, the retry timeout is doubled
	 * up to a maximum of (initial timeout * max retry factor).
	 *
	 * @param factor the maximum retry factor, must be greater than or equal to 0
	 * @return this builder instance for method chaining
	 * @throws InvalidCircuitBreakerValueException if factor is less than 0
	 */
	public CircuitBreakerBuilder withMaxRetryFactor(int factor) {
		if(factor < 0) {
			throw new InvalidCircuitBreakerValueException("MaxRetryFactor should always either positive or zero");
		}
		this.maxRetryFactor = factor;
		return this;
	}

	/**
	 * Builds and returns a new CircuitBreaker instance with the configured parameters.
	 *
	 * @return a new CircuitBreaker instance
	 */
	public CircuitBreaker build() {
		return new CircuitBreaker(failureThreshold, retryTimeoutMillis, maxRetryFactor);
	}

}

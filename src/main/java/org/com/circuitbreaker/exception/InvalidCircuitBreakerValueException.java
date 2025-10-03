package org.com.circuitbreaker.exception;

/**
 * Exception thrown when invalid configuration values are provided to circuit breaker components.
 * <p>
 * This runtime exception is thrown by the {@link org.com.circuitbreaker.CircuitBreakerBuilder}
 * when validation fails for configuration parameters such as negative thresholds or timeouts.
 */
public class InvalidCircuitBreakerValueException extends RuntimeException {

	/**
	 * Constructs a new InvalidCircuitBreakerValueException with the specified detail message.
	 *
	 * @param message the detail message explaining the validation failure
	 */
	public InvalidCircuitBreakerValueException(String message) {
		super(message);
	}
}

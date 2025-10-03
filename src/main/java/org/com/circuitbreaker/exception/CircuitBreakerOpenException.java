package org.com.circuitbreaker.exception;

/**
 * Exception thrown when a circuit breaker rejects a request because the circuit is in OPEN state.
 * <p>
 * This exception is thrown by the {@link org.com.circuitbreaker.CircuitBreaker#execute(java.util.function.Supplier)}
 * method when the circuit breaker is OPEN and blocking requests. The exception message includes
 * details about the failure threshold, retry timeout, and current state to help with debugging.
 * <p>
 * When this exception is thrown, callers should either implement fallback logic or wait for
 * the circuit to transition to HALF_OPEN state after the retry timeout expires.
 */
public class CircuitBreakerOpenException extends RuntimeException {

	/**
	 * Constructs a new CircuitBreakerOpenException with the specified detail message.
	 *
	 * @param message the detail message explaining why the request was blocked and when to retry
	 */
	public CircuitBreakerOpenException(String message) {
		super(message);
	}
}

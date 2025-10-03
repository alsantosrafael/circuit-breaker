package org.com.circuitbreaker.exception;

/**
 * Exception thrown when a circuit breaker execution fails due to an underlying exception.
 * <p>
 * This exception wraps the original exception that occurred during execution while providing
 * context about the circuit breaker state when the failure happened. It is thrown by the
 * {@link org.com.circuitbreaker.CircuitBreaker#execute(java.util.function.Supplier)} method
 * when the supplied operation fails with an exception.
 */
public class CircuitBreakerExecutionException extends RuntimeException {

	/**
	 * Constructs a new CircuitBreakerExecutionException with the specified detail message and cause.
	 *
	 * @param message the detail message explaining the execution failure context
	 * @param cause the underlying exception that caused the execution failure
	 */
	public CircuitBreakerExecutionException(String message, Throwable cause) {
		super(message, cause);
	}
}

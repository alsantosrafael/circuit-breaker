package org.com.circuitbreaker;

/**
 * Interface for receiving notifications about circuit breaker state changes.
 * <p>
 * Implement this interface to be notified when the circuit breaker transitions
 * between OPEN, HALF_OPEN, and CLOSED states. Listeners are particularly useful
 * for monitoring, logging, or triggering fallback behaviors.
 * <p>
 * Thread Safety: Listener methods may be called concurrently and should be
 * implemented to be thread-safe if they access shared state.
 */
public interface CircuitBreakerListener {

	/**
	 * Called when the circuit breaker transitions to OPEN state.
	 * <p>
	 * This occurs when the failure threshold is reached, indicating that
	 * the external service is likely unavailable and requests will be blocked.
	 */
	void onOpen();

	/**
	 * Called when the circuit breaker transitions to HALF_OPEN state.
	 * <p>
	 * This occurs after the retry timeout has elapsed while in OPEN state,
	 * allowing a single trial request to test if the service has recovered.
	 */
	void onHalfOpen();

	/**
	 * Called when the circuit breaker transitions to CLOSED state.
	 * <p>
	 * This occurs when a request succeeds, indicating that the external
	 * service is available and normal operation can resume.
	 */
	void onClose();
}

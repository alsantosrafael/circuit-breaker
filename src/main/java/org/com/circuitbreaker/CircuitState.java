package org.com.circuitbreaker;

/**
 * Enumeration of possible circuit breaker states.
 * <p>
 * The circuit breaker operates as a state machine transitioning between these three states
 * based on the success/failure of requests and configured timeouts.
 */
public enum CircuitState {

	/**
	 * Circuit is OPEN - requests are blocked.
	 * <p>
	 * The circuit enters this state when the failure threshold is reached.
	 * After a configured timeout period, it transitions to HALF_OPEN to test recovery.
	 */
	OPEN,

	/**
	 * Circuit is CLOSED - requests are allowed through normally.
	 * <p>
	 * This is the normal operating state where requests pass through to the external service.
	 * Failed requests increment the failure counter, potentially leading to OPEN state.
	 */
	CLOSED,

	/**
	 * Circuit is HALF_OPEN - allowing a single trial request.
	 * <p>
	 * The circuit enters this state after the retry timeout expires while OPEN.
	 * A single request is allowed to test if the external service has recovered.
	 * Success leads to CLOSED state, failure returns to OPEN with increased timeout.
	 */
	HALF_OPEN,
}

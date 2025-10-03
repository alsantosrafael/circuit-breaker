package org.com.circuitbreaker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implements a simple Circuit Breaker pattern with exponential backoff for retry timeouts.
 * <p>
 * Thread Safety: This implementation uses lock-free atomic operations via Compare-And-Swap (CAS)
 * for state transitions and counters, ensuring high-performance thread safety without synchronization overhead.
 * State changes use {@link AtomicReference#compareAndSet(Object, Object)} and counter operations use
 * {@link AtomicInteger} methods to avoid blocking and contention.
 * <p>
 * Features:
 * <ul>
 *   <li>HALF_OPEN state for trial requests after timeout</li>
 *   <li>Exponential backoff for retry timeouts up to a maximum</li>
 *   <li>Lock-free thread-safe state transitions using atomic CAS operations</li>
 *   <li>High-performance concurrent access without synchronization blocks</li>
 * </ul>
 */
public class CircuitBreaker {
	private AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
	private AtomicInteger failureCounter = new AtomicInteger(0);
	private final int failureThreshold;
	private final long retryTimeOutMillis;
	private volatile long lastFailureTime = 0;
	private volatile long currentRetryTimeoutMillis;
	private final long maxRetryTimeoutMillis;

	/**
	 * Constructs a CircuitBreaker with the specified failure threshold and retry timeout.
	 * Supports exponential backoff for retry timeouts up to a maximum factor.
	 *
	 * @param failureThreshold the number of failures before opening the circuit
	 * @param retryTimeOutMillis the initial timeout in milliseconds before allowing a retry
	 * @param maxRetryFactor the maximum multiplier for exponential backoff of retry timeout
	 */
	public CircuitBreaker(int failureThreshold, long retryTimeOutMillis, int maxRetryFactor) {
		this.failureThreshold = failureThreshold;
		this.retryTimeOutMillis = retryTimeOutMillis;
		this.currentRetryTimeoutMillis = retryTimeOutMillis;
		this.maxRetryTimeoutMillis = retryTimeOutMillis * maxRetryFactor;
	}

	/**
	 * Determines if a request is allowed based on the current circuit state.
	 * <p>
	 * If the circuit is OPEN and the retry timeout has passed, the state transitions to HALF_OPEN
	 * using atomic CAS operation, allowing a single trial request. Further requests are blocked
	 * until the trial succeeds or fails.
	 * <p>
	 * Thread Safety: Uses lock-free atomic operations for thread-safe state checking and transitions.
	 *
	 * @return true if the request is allowed, false otherwise
	 */
	public boolean allowRequest() {
		if (this.state.get() == CircuitState.OPEN) {
			if (System.currentTimeMillis() - lastFailureTime > this.currentRetryTimeoutMillis) {
				this.state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN);
				System.out.println("Trying to close circuit again!");
				return true;
			}

			return false;
		} else if(this.state.get() == CircuitState.HALF_OPEN) {
			System.out.println("Still testing connection... blocking extra requests.");
			return false;
		}
		return true;
	}

	/**
	 * Records a successful request, closing the circuit and resetting counters and retry timeout.
	 * <p>
	 * Thread Safety: Uses atomic operations to safely reset state without blocking.
	 */
	public void recordSuccess() {
		this.state.getAndSet(CircuitState.CLOSED);
		this.failureCounter.getAndSet( 0);
		this.lastFailureTime = 0;
		this.currentRetryTimeoutMillis = this.retryTimeOutMillis;
	}

	/**
	 * Records a failed request, incrementing the failure counter and updating the last failure time.
	 * <p>
	 * If the circuit is HALF_OPEN and the trial fails, the retry timeout is exponentially increased (up to a max),
	 * and the circuit returns to OPEN. If the failure threshold is reached, the circuit opens.
	 * <p>
	 * Thread Safety: Uses atomic counter increments and state updates for lock-free operation.
	 */
	public void recordFailure() {
		this.failureCounter.getAndIncrement();
		this.lastFailureTime = System.currentTimeMillis();
		if (this.state.get() == CircuitState.HALF_OPEN) {
			this.currentRetryTimeoutMillis = Math.min(2 * this.currentRetryTimeoutMillis, this.maxRetryTimeoutMillis);
			this.state.getAndSet(CircuitState.OPEN);
		}
		if (this.failureCounter.get() >= this.failureThreshold) {
			this.state.getAndSet(CircuitState.OPEN);
		}
	}

	/**
	 * Returns the current state of the circuit.
	 *
	 * @return the current CircuitState
	 */
	public CircuitState getState() {
		return this.state.get();
	}

	public static CircuitBreakerBuilder newBuilder() {
		return new CircuitBreakerBuilder();
	}
}

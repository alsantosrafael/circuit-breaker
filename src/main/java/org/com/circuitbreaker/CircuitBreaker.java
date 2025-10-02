package org.com.circuitbreaker;

/**
 * Implements a simple Circuit Breaker pattern with exponential backoff for retry timeouts.
 * <p>
 * Thread Safety: All state-changing and query methods are synchronized to ensure safe usage in multi-threaded environments.
 * <p>
 * Features:
 * <ul>
 *   <li>HALF_OPEN state for trial requests after timeout</li>
 *   <li>Exponential backoff for retry timeouts up to a maximum</li>
 *   <li>Thread-safe state transitions and counters</li>
 * </ul>
 */
public class CircuitBreaker {
	private CircuitStatus status = CircuitStatus.CLOSED;
	private int failureCounter = 0;
	private final int failureThreshold;
	private final long retryTimeOutMillis;
	private long lastFailureTime = 0;
	private long currentRetryTimeoutMillis;
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
	 * Determines if a request is allowed based on the current circuit status.
	 * <p>
	 * If the circuit is OPEN and the retry timeout has passed, the status transitions to HALF_OPEN,
	 * allowing a single trial request. Further requests are blocked until the trial succeeds or fails.
	 * <p>
	 * Thread Safety: This method is synchronized.
	 *
	 * @return true if the request is allowed, false otherwise
	 */
	public synchronized boolean allowRequest() {
		if (this.status == CircuitStatus.OPEN) {
			if (System.currentTimeMillis() - lastFailureTime > this.currentRetryTimeoutMillis) {
				this.status = CircuitStatus.HALF_OPEN;
				System.out.println("Trying to close circuit again!");
				return true;
			}

			return false;
		} else if(this.status == CircuitStatus.HALF_OPEN) {
			System.out.println("Still testing connection... blocking extra requests.");
			return false;
		}
		return true;
	}

	/**
	 * Records a successful request, closing the circuit and resetting counters and retry timeout.
	 * <p>
	 * Thread Safety: This method is synchronized.
	 */
	public synchronized void recordSuccess() {
		this.status = CircuitStatus.CLOSED;
		this.failureCounter = 0;
		this.lastFailureTime = 0;
		this.currentRetryTimeoutMillis = this.retryTimeOutMillis;
	}

	/**
	 * Records a failed request, incrementing the failure counter and updating the last failure time.
	 * <p>
	 * If the circuit is HALF_OPEN and the trial fails, the retry timeout is exponentially increased (up to a max),
	 * and the circuit returns to OPEN. If the failure threshold is reached, the circuit opens.
	 * <p>
	 * Thread Safety: This method is synchronized.
	 */
	public synchronized void recordFailure() {
		this.failureCounter++;
		this.lastFailureTime = System.currentTimeMillis();
		if (this.status == CircuitStatus.HALF_OPEN) {
			this.currentRetryTimeoutMillis = Math.min(2 * this.currentRetryTimeoutMillis, this.maxRetryTimeoutMillis);
			this.status = CircuitStatus.OPEN;
		}
		if (this.failureCounter >= this.failureThreshold) {
			this.status = CircuitStatus.OPEN;
		}
	}

	/**
	 * Returns the current status of the circuit.
	 *
	 * @return the current CircuitStatus
	 */
	public CircuitStatus getStatus() {
		return this.status;
	}
}

package org.com.circuitbreaker;

/**
 * Implements a simple Circuit Breaker pattern.
 */
public class CircuitBreaker {
	private CircuitStatus status = CircuitStatus.CLOSED;
	private int failureCounter = 0;
	private final int failureThreshold;
	private final long retryTimeOutMillis;
	private long lastFailureTime = 0;

	/**
	 * Constructs a CircuitBreaker with the specified failure threshold and retry timeout.
	 *
	 * @param failureThreshold the number of failures before opening the circuit
	 * @param retryTimeOutMillis the timeout in milliseconds before allowing a retry
	 */
	public CircuitBreaker(int failureThreshold, long retryTimeOutMillis) {
		this.failureThreshold = failureThreshold;
		this.retryTimeOutMillis = retryTimeOutMillis;
	}

	/**
	 * Determines if a request is allowed based on the current circuit status.
	 * If the circuit is open and the retry timeout has passed, the status is set to HALF_OPEN.
	 *
	 * @return true if the request is allowed, false otherwise
	 */
	public synchronized boolean allowRequest() {
		if (this.status == CircuitStatus.OPEN) {
			if (System.currentTimeMillis() - lastFailureTime > retryTimeOutMillis) {
				this.status = CircuitStatus.HALF_OPEN;
				System.out.println("Trying to close circuit again!");
				return true;
			}
			else if(this.status == CircuitStatus.HALF_OPEN) {
				System.out.println("Trial to close circuit again failed!");
				this.status = CircuitStatus.OPEN;
				return false;
			}
			return false;
		}
		return true;
	}

	/**
	 * Records a successful request, closing the circuit and resetting counters.
	 */
	public synchronized void recordSuccess() {
		this.status = CircuitStatus.CLOSED;
		this.failureCounter = 0;
		this.lastFailureTime = 0;
	}

	/**
	 * Records a failed request, incrementing the failure counter and updating the last failure time.
	 * Opens the circuit if the failure threshold is reached.
	 * If the circuit had a previous state of HALF_OPEN, and it fails, it means
	 * the last trial of reestablishing contact failed, the circuit will open again
	 */
	public synchronized void recordFailure() {
		this.failureCounter++;
		this.lastFailureTime = System.currentTimeMillis();
		if (this.status == CircuitStatus.HALF_OPEN) {
			this.status = CircuitStatus.OPEN;
		}
		if (this.failureCounter == this.failureThreshold) {
			this.status = CircuitStatus.OPEN;
		}
	}

	public CircuitStatus getStatus() {
		return this.status;
	}
}
